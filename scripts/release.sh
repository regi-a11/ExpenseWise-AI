#!/bin/bash

# Local release script that replicates .github/workflows/release.yml
# Usage: ./scripts/release.sh [patch|minor|major] [--dry-run]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Global variables for cleanup
CHANGELOG_FILE=""
CHANGELOG_DIR="fastlane/metadata/android/en-US/changelogs"
CHANGES_MADE=false

# Cleanup function for interruptions
cleanup_on_exit() {
    if [ "$CHANGES_MADE" = true ]; then
        echo ""
        echo -e "${YELLOW}⚠️  Script interrupted. Reverting changes...${NC}"
        git checkout -- app/build.gradle.kts 2>/dev/null || true
        if [ -n "$CHANGELOG_FILE" ] && [ -f "$CHANGELOG_FILE" ]; then
            rm -f "$CHANGELOG_FILE"
        fi
        if [ -f "$CHANGELOG_DIR/default.txt" ]; then
            git checkout -- "$CHANGELOG_DIR/default.txt" 2>/dev/null || rm -f "$CHANGELOG_DIR/default.txt"
        fi
        echo -e "${YELLOW}Changes reverted.${NC}"
    fi
}

# Set trap for cleanup on exit
trap cleanup_on_exit EXIT INT TERM

# Parse arguments
VERSION_BUMP=${1:-patch}
DRY_RUN=""
if [ "$2" = "--dry-run" ]; then
    DRY_RUN="true"
    echo -e "${YELLOW}🔍 DRY RUN MODE${NC}"
fi

echo -e "${GREEN}🚀 Starting release (${VERSION_BUMP} bump)${NC}"

# 1. Get current version
CURRENT_VERSION=$(grep "versionName = " app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
CURRENT_CODE=$(grep "versionCode = " app/build.gradle.kts | sed 's/[^0-9]*//g')
echo "Current version: $CURRENT_VERSION (code: $CURRENT_CODE)"

# 2. Calculate next version
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

if [ "$VERSION_BUMP" = "major" ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
elif [ "$VERSION_BUMP" = "minor" ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
elif [ "$VERSION_BUMP" = "patch" ]; then
    PATCH=$((PATCH + 1))
fi

NEXT_VERSION="$MAJOR.$MINOR.$PATCH"
NEXT_CODE=$((CURRENT_CODE + 1))
echo "Next version: $NEXT_VERSION (code: $NEXT_CODE)"

# 3. Check if tag exists
TAG_NAME="v$NEXT_VERSION"
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo -e "${RED}❌ Tag $TAG_NAME already exists locally${NC}"
    exit 1
fi

# 4. Generate changelog
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
echo "# Release v$NEXT_VERSION" > RELEASE_NOTES.md
echo "" >> RELEASE_NOTES.md

if [ -n "$LAST_TAG" ]; then
    echo "## Changes since $LAST_TAG" >> RELEASE_NOTES.md
    echo "" >> RELEASE_NOTES.md
    git log $LAST_TAG..HEAD --pretty=format:"- %s (%h)" >> RELEASE_NOTES.md
else
    echo "## Initial Release" >> RELEASE_NOTES.md
    echo "" >> RELEASE_NOTES.md
    echo "First release of PennyWise" >> RELEASE_NOTES.md
fi

echo "" >> RELEASE_NOTES.md
echo "---" >> RELEASE_NOTES.md
echo "### Installation" >> RELEASE_NOTES.md
echo "Download the APK below and install it on your Android device." >> RELEASE_NOTES.md

echo -e "${GREEN}✅ Changelog generated${NC}"

if [ "$DRY_RUN" = "true" ]; then
    echo -e "${YELLOW}🔍 DRY RUN SUMMARY${NC}"
    echo "=================="
    echo "Current version: $CURRENT_VERSION"
    echo "Next version: $NEXT_VERSION"
    echo "Version code: $NEXT_CODE"
    echo ""
    echo "📝 Release Notes:"
    cat RELEASE_NOTES.md
    exit 0
fi

# 5. Update version (versionName and versionCode)
# Use sed -i '' for macOS, sed -i for Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/versionName = \".*\"/versionName = \"$NEXT_VERSION\"/" app/build.gradle.kts
    sed -i '' "s/versionCode = .*/versionCode = $NEXT_CODE/" app/build.gradle.kts
else
    sed -i "s/versionName = \".*\"/versionName = \"$NEXT_VERSION\"/" app/build.gradle.kts
    sed -i "s/versionCode = .*/versionCode = $NEXT_CODE/" app/build.gradle.kts
fi
CHANGES_MADE=true  # Mark that we've made changes
echo -e "${GREEN}✅ Version updated: $NEXT_VERSION (code: $NEXT_CODE)${NC}"

# 5a. Update fastlane changelog
CHANGELOG_FILE="$CHANGELOG_DIR/${NEXT_CODE}.txt"

# Generate simple changelog for fastlane
if [ -n "$LAST_TAG" ]; then
    echo "Version $NEXT_VERSION" > "$CHANGELOG_FILE"
    echo "" >> "$CHANGELOG_FILE"
    # Get features
    FEATURES=$(git log $LAST_TAG..HEAD --pretty=format:"%s" | grep "^feat" 2>/dev/null | sed 's/^feat[:(].*[):] */• /' | head -5)
    FIXES=$(git log $LAST_TAG..HEAD --pretty=format:"%s" | grep "^fix" 2>/dev/null | sed 's/^fix[:(].*[):] */• /' | head -5)
    
    if [ -n "$FEATURES" ]; then
        echo "New Features:" >> "$CHANGELOG_FILE"
        echo "$FEATURES" >> "$CHANGELOG_FILE"
        echo "" >> "$CHANGELOG_FILE"
    fi
    
    if [ -n "$FIXES" ]; then
        echo "Bug Fixes:" >> "$CHANGELOG_FILE"
        echo "$FIXES" >> "$CHANGELOG_FILE"
    fi
    
    # If no conventional commits, just use recent commits
    if [ -z "$FEATURES" ] && [ -z "$FIXES" ]; then
        git log $LAST_TAG..HEAD --pretty=format:"• %s" | head -5 >> "$CHANGELOG_FILE"
    fi
else
    echo "Initial release" > "$CHANGELOG_FILE"
fi

# Also update default.txt
cp "$CHANGELOG_FILE" "$CHANGELOG_DIR/default.txt"
echo -e "${GREEN}✅ Fastlane changelog created: $CHANGELOG_FILE${NC}"

# 6. Build APKs
echo -e "${YELLOW}🔨 Building APKs...${NC}"

# Function to revert changes on failure
revert_changes() {
    echo -e "${RED}❌ Build failed! Reverting changes...${NC}"
    
    CHANGES_MADE=false  # Reset flag so cleanup_on_exit doesn't run again
    
    # Revert build.gradle.kts
    git checkout -- app/build.gradle.kts
    
    # Remove fastlane changelog files
    if [ -f "$CHANGELOG_FILE" ]; then
        rm -f "$CHANGELOG_FILE"
    fi
    if [ -f "$CHANGELOG_DIR/default.txt" ]; then
        git checkout -- "$CHANGELOG_DIR/default.txt" 2>/dev/null || rm -f "$CHANGELOG_DIR/default.txt"
    fi
    
    echo -e "${YELLOW}⚠️  Changes reverted. Please fix the build errors and try again.${NC}"
    exit 1
}

# Try to build with error handling
if ! ./gradlew clean; then
    revert_changes
fi

if ! ./gradlew assembleStandardRelease; then
    revert_changes
fi

if ! ./gradlew assembleFdroidRelease; then
    revert_changes
fi

echo -e "${GREEN}✅ APKs built${NC}"

# 7. Rename APKs (matching GitHub Actions)
STANDARD_PATH="app/build/outputs/apk/standard/release"
FDROID_PATH="app/build/outputs/apk/fdroid/release"

# Rename universal APK
if [ -f "$STANDARD_PATH/app-standard-universal-release.apk" ]; then
    mv "$STANDARD_PATH/app-standard-universal-release.apk" \
       "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-universal.apk"
fi

# Rename architecture-specific APKs
for arch in armeabi-v7a arm64-v8a x86 x86_64; do
    if [ -f "$STANDARD_PATH/app-standard-${arch}-release.apk" ]; then
        mv "$STANDARD_PATH/app-standard-${arch}-release.apk" \
           "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-${arch}.apk"
    fi
done

# Rename F-Droid APK
if [ -f "$FDROID_PATH/app-fdroid-release-unsigned.apk" ]; then
    mv "$FDROID_PATH/app-fdroid-release-unsigned.apk" \
       "$FDROID_PATH/PennyWise-fdroid-v${NEXT_VERSION}.apk"
fi

echo -e "${GREEN}✅ APKs renamed${NC}"

# 8. Calculate SHA256
ORIGINAL_DIR="$PWD"
cd "$STANDARD_PATH"
for apk in PennyWise-v${NEXT_VERSION}*.apk; do
    if [ -f "$apk" ]; then
        sha256sum "$apk" > "${apk}.sha256"
    fi
done

cd "$ORIGINAL_DIR/$FDROID_PATH"
if [ -f "PennyWise-fdroid-v${NEXT_VERSION}.apk" ]; then
    sha256sum "PennyWise-fdroid-v${NEXT_VERSION}.apk" > "PennyWise-fdroid-v${NEXT_VERSION}.apk.sha256"
fi
cd "$ORIGINAL_DIR"

echo -e "${GREEN}✅ SHA256 calculated${NC}"

# 9. Commit and tag
if [ -f "app/build.gradle.kts" ]; then
    git add app/build.gradle.kts
else
    echo -e "${RED}Error: app/build.gradle.kts not found${NC}"
    exit 1
fi

if [ -f "$CHANGELOG_FILE" ]; then
    git add "$CHANGELOG_FILE"
fi

if [ -f "$CHANGELOG_DIR/default.txt" ]; then
    git add "$CHANGELOG_DIR/default.txt"
fi

git commit -m "chore(release): bump version to $NEXT_VERSION [skip ci]"
git tag -a "v$NEXT_VERSION" -m "Release v$NEXT_VERSION"
CHANGES_MADE=false  # Changes are now committed, no need to revert
echo -e "${GREEN}✅ Commit and tag created${NC}"

# 10. Push to GitHub
echo ""
read -p "Push to GitHub? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push origin main
    git push origin "v$NEXT_VERSION"
    echo -e "${GREEN}✅ Pushed to GitHub${NC}"
    
    # Create GitHub release with gh CLI
    if command -v gh &> /dev/null; then
        echo "Creating GitHub release..."
        gh release create "v$NEXT_VERSION" \
            --title "Release v$NEXT_VERSION" \
            --notes-file RELEASE_NOTES.md \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-universal.apk" \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-universal.apk.sha256" \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-arm64-v8a.apk" \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-arm64-v8a.apk.sha256" \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-armeabi-v7a.apk" \
            "$STANDARD_PATH/PennyWise-v${NEXT_VERSION}-armeabi-v7a.apk.sha256" \
            "$FDROID_PATH/PennyWise-fdroid-v${NEXT_VERSION}.apk" \
            "$FDROID_PATH/PennyWise-fdroid-v${NEXT_VERSION}.apk.sha256"
        echo -e "${GREEN}✅ GitHub release created${NC}"
    else
        echo -e "${YELLOW}gh CLI not found. Create release manually at:${NC}"
        echo "https://github.com/sarim2000/pennywiseai-tracker/releases/new?tag=v$NEXT_VERSION"
    fi
fi

# 11. Build Play Store Bundle (optional)
echo ""
read -p "Build Play Store Bundle (.aab)? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}🔨 Building App Bundle for Play Store...${NC}"
    ./gradlew bundleStandardRelease
    
    # Rename AAB file
    AAB_PATH="app/build/outputs/bundle/standardRelease"
    if [ -f "$AAB_PATH/app-standard-release.aab" ]; then
        mv "$AAB_PATH/app-standard-release.aab" \
           "$AAB_PATH/PennyWise-v${NEXT_VERSION}.aab"
        echo -e "${GREEN}✅ App Bundle created: $AAB_PATH/PennyWise-v${NEXT_VERSION}.aab${NC}"
        
        # Show file size
        SIZE=$(du -h "$AAB_PATH/PennyWise-v${NEXT_VERSION}.aab" | cut -f1)
        echo "Size: $SIZE"
        
        echo ""
        echo -e "${YELLOW}📱 Play Store Upload Instructions:${NC}"
        echo "1. Go to https://play.google.com/console"
        echo "2. Select PennyWise app"
        echo "3. Go to Release > Production (or Testing)"
        echo "4. Create new release"
        echo "5. Upload: $AAB_PATH/PennyWise-v${NEXT_VERSION}.aab"
        echo "6. Add release notes from RELEASE_NOTES.md"
    fi
fi

echo ""
echo -e "${GREEN}✨ Release $NEXT_VERSION complete!${NC}"