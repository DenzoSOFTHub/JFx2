#!/bin/bash

# GitHub Sync Script for JFx2
# Checks if repo exists on GitHub, creates if not, then pushes current version

set -e

# Configuration
GITHUB_USER="DenzoSOFTHub"
REPO_NAME="JFx2"
REPO_DESCRIPTION="Guitar Multi-Effects Processor with node-based signal routing"
REPO_VISIBILITY="public"  # or "private"

# Token from environment variable or .github_token file
if [ -z "$GITHUB_TOKEN" ]; then
    TOKEN_FILE="$(dirname "$0")/../.github_token"
    if [ -f "$TOKEN_FILE" ]; then
        GITHUB_TOKEN=$(cat "$TOKEN_FILE" | tr -d '\n')
    else
        echo "Error: GITHUB_TOKEN environment variable not set and .github_token file not found."
        echo ""
        echo "Usage: Set token before running:"
        echo "  export GITHUB_TOKEN=ghp_your_token_here"
        echo "  ./scripts/github-sync.sh"
        echo ""
        echo "Or create .github_token file with your token (will be gitignored)"
        exit 1
    fi
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  JFx2 GitHub Sync Script${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is not installed.${NC}"
    exit 1
fi

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: git is not installed.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites OK${NC}"

# Get current directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo -e "${BLUE}Project directory: ${PROJECT_DIR}${NC}"

# Check if this is a git repository
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}Initializing git repository...${NC}"
    git init
    echo -e "${GREEN}✓ Git repository initialized${NC}"
fi

# Configure git user if not set
if [ -z "$(git config user.email)" ]; then
    git config user.email "denzo@denzosoft.it"
    git config user.name "DenzoSOFTHub"
    echo -e "${GREEN}✓ Git user configured${NC}"
fi

# Get version from pom.xml
VERSION=$(grep -m1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')
echo -e "${BLUE}Current version: ${VERSION}${NC}"

# Check if remote repository exists on GitHub
echo -e "${YELLOW}Checking if repository exists on GitHub...${NC}"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/repos/${GITHUB_USER}/${REPO_NAME}")

if [ "$HTTP_CODE" = "404" ]; then
    echo -e "${YELLOW}Repository does not exist. Creating...${NC}"

    # Create the repository
    RESPONSE=$(curl -s -X POST \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Accept: application/vnd.github.v3+json" \
        "https://api.github.com/user/repos" \
        -d "{
            \"name\": \"${REPO_NAME}\",
            \"description\": \"${REPO_DESCRIPTION}\",
            \"private\": $([ "$REPO_VISIBILITY" = "private" ] && echo "true" || echo "false"),
            \"auto_init\": false
        }")

    # Check if creation was successful
    if echo "$RESPONSE" | grep -q '"id"'; then
        echo -e "${GREEN}✓ Repository created: https://github.com/${GITHUB_USER}/${REPO_NAME}${NC}"
    else
        echo -e "${RED}Error creating repository:${NC}"
        echo "$RESPONSE"
        exit 1
    fi
elif [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Repository exists: https://github.com/${GITHUB_USER}/${REPO_NAME}${NC}"
else
    echo -e "${RED}Error checking repository (HTTP ${HTTP_CODE})${NC}"
    exit 1
fi

# Set up remote with token authentication
REMOTE_URL="https://${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"

# Check if origin remote exists
if git remote get-url origin &>/dev/null; then
    git remote set-url origin "$REMOTE_URL"
    echo -e "${GREEN}✓ Remote origin updated${NC}"
else
    git remote add origin "$REMOTE_URL"
    echo -e "${GREEN}✓ Remote origin added${NC}"
fi

# Check for changes to commit
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${YELLOW}Uncommitted changes found. Staging all files...${NC}"

    # Add all files
    git add -A

    # Create commit message with version
    COMMIT_MSG="Release v${VERSION}

JFx2 Guitar Multi-Effects Processor
Build: $(date +%Y%m%d_%H%M%S)

Generated with Claude Code (https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"

    git commit -m "$COMMIT_MSG"
    echo -e "${GREEN}✓ Changes committed${NC}"
else
    echo -e "${GREEN}✓ No uncommitted changes${NC}"
fi

# Get current branch (after first commit, branch exists)
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "master")

# Rename master to main if needed
if [ "$CURRENT_BRANCH" = "master" ]; then
    echo -e "${YELLOW}Renaming branch master to main...${NC}"
    git branch -m master main
    CURRENT_BRANCH="main"
    echo -e "${GREEN}✓ Branch renamed to main${NC}"
fi

echo -e "${BLUE}Current branch: ${CURRENT_BRANCH}${NC}"

# Push to GitHub
echo -e "${YELLOW}Pushing to GitHub...${NC}"

# First try normal push
if git push -u origin "$CURRENT_BRANCH" 2>&1; then
    echo -e "${GREEN}✓ Pushed to GitHub successfully${NC}"
else
    # If failed, might need to pull first or force push
    echo -e "${YELLOW}Trying force push...${NC}"
    git push -u origin "$CURRENT_BRANCH" --force
    echo -e "${GREEN}✓ Pushed to GitHub successfully${NC}"
fi

# Create/update release tag
TAG_NAME="v${VERSION}"
echo -e "${YELLOW}Creating release tag: ${TAG_NAME}...${NC}"

# Delete existing tag if present (local and remote)
git tag -d "$TAG_NAME" 2>/dev/null || true
git push origin --delete "$TAG_NAME" 2>/dev/null || true

# Create new tag
git tag -a "$TAG_NAME" -m "Release ${TAG_NAME}"
git push origin "$TAG_NAME"

echo -e "${GREEN}✓ Release tag created: ${TAG_NAME}${NC}"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Sync completed successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "Repository: ${BLUE}https://github.com/${GITHUB_USER}/${REPO_NAME}${NC}"
echo -e "Version:    ${BLUE}${VERSION}${NC}"
echo -e "Tag:        ${BLUE}${TAG_NAME}${NC}"
echo ""
