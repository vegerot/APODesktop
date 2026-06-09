#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
set -x

# Resolve script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# 1. Resolve Android SDK path
SDK_PATH="${ANDROID_HOME:-/home/max/Android/Sdk}"
if [[ ! -d "$SDK_PATH" ]]; then
    echo "Error: Android SDK not found at $SDK_PATH. Please set ANDROID_HOME." >&2
    exit 1
fi

# Locate build tools (find the latest version under build-tools)
BUILD_TOOLS_DIR=$(find "${SDK_PATH}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)
if [[ -z "$BUILD_TOOLS_DIR" || ! -d "$BUILD_TOOLS_DIR" ]]; then
    echo "Error: Could not locate build-tools under $SDK_PATH." >&2
    exit 1
fi

ZIPALIGN="${BUILD_TOOLS_DIR}/zipalign"
APKSIGNER="${BUILD_TOOLS_DIR}/apksigner"

if [[ ! -f "$ZIPALIGN" || ! -f "$APKSIGNER" ]]; then
    echo "Error: zipalign or apksigner not found in $BUILD_TOOLS_DIR." >&2
    exit 1
fi

echo "Using build tools from: $BUILD_TOOLS_DIR"

# 2. Build Release APK using Gradle
echo "Building release APK..."
./gradlew assembleRelease

# 3. Locate the unsigned release APK
UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
if [[ ! -f "$UNSIGNED_APK" ]]; then
    # In some Gradle/AGP configurations it might be named app-release.apk but unsigned if signingConfig is absent
    UNSIGNED_APK=$(find app/build/outputs/apk/release/ -name "*.apk" | head -n 1)
fi

if [[ -z "$UNSIGNED_APK" || ! -f "$UNSIGNED_APK" ]]; then
    echo "Error: Could not locate built release APK under app/build/outputs/apk/release/." >&2
    exit 1
fi

echo "Found built APK: $UNSIGNED_APK"

# 4. Generate local keystore if it doesn't exist
KEYSTORE="local-release.keystore"
KEYSTORE_PASS="android"
ALIAS="local-alias"

if [[ ! -f "$KEYSTORE" ]]; then
    echo "Generating local release keystore..."
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias "$ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=Vegerot, O=Vegerot, C=US"
fi

# 5. Align and Sign the APK
echo "Aligning APK..."
TEMP_ALIGNED="app-release-aligned.apk"
rm -f "$TEMP_ALIGNED"
"$ZIPALIGN" -v -p 4 "$UNSIGNED_APK" "$TEMP_ALIGNED"

echo "Signing APK..."
FINAL_APK="com.vegerot.apodesktop.apk"
rm -f "$FINAL_APK"
"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:"$KEYSTORE_PASS" \
    --ks-key-alias "$ALIAS" \
    --key-pass pass:"$KEYSTORE_PASS" \
    --out "$FINAL_APK" \
    "$TEMP_ALIGNED"

# 6. Clean up temporary files
rm -f "$TEMP_ALIGNED"

echo "--------------------------------------------------------"
echo "Success! Signed optimized release APK created at:"
echo "  $SCRIPT_DIR/$FINAL_APK"
echo "--------------------------------------------------------"
