# Converge Android - Development Commands
# Install just: brew install just (macOS) or cargo install just

# Default: show available commands
default:
    @just --list

# ============================================
# Build & Run
# ============================================

# Build debug APK
build:
    ./gradlew assembleDebug

# Build release APK
build-release:
    ./gradlew assembleRelease

# Install debug APK on connected device/emulator
install:
    ./gradlew installDebug

# Run app on emulator (build + install + launch)
run: install
    adb shell am start -n zone.converge.android/.MainActivity

# Clean build artifacts
clean:
    ./gradlew clean

# ============================================
# Quality Checks
# ============================================

# Run all quality checks (format, lint, test)
check: format-check lint test
    @echo "✓ All checks passed!"

# Run Detekt static analysis
lint:
    ./gradlew detekt

# Check formatting with Spotless
format-check:
    ./gradlew spotlessCheck

# Apply formatting with Spotless
format:
    ./gradlew spotlessApply

# Run unit tests
test:
    ./gradlew test

# Run unit tests with coverage
test-coverage:
    ./gradlew testDebugUnitTestCoverage

# Run connected/instrumentation tests (requires device/emulator)
test-connected:
    ./gradlew connectedAndroidTest

# Run all tests
test-all: test test-connected

# ============================================
# Proto Generation
# ============================================

# Generate Kotlin code from proto files
proto:
    ./gradlew generateProto

# Clean generated proto code
proto-clean:
    rm -rf app/build/generated/source/proto

# ============================================
# Emulator Management
# ============================================

# List available AVDs
avd-list:
    emulator -list-avds

# Start emulator (uses Converge_Pixel_7 by default)
emulator avd="Converge_Pixel_7":
    emulator -avd {{avd}} &

# ============================================
# Eval & Screenshots
# ============================================

# Run eval harness tests
eval:
    ./gradlew testDebugUnitTest --tests "*EvalTest*"

# Record Paparazzi screenshots
screenshots-record:
    ./gradlew recordPaparazziDebug

# Verify Paparazzi screenshots
screenshots-verify:
    ./gradlew verifyPaparazziDebug

# ============================================
# Dependencies
# ============================================

# Show dependency tree
deps:
    ./gradlew dependencies

# Check for dependency updates
deps-updates:
    ./gradlew dependencyUpdates

# ============================================
# CI / Pre-push
# ============================================

# Quick check before push
pre-push: format-check lint test
    @echo "✓ Ready to push!"

# Full CI check
ci: format-check lint test screenshots-verify
    @echo "✓ CI checks passed!"

# ============================================
# ADB Utilities
# ============================================

# Show connected devices
devices:
    adb devices

# View app logs
logs:
    adb logcat -s ConvergeClient SmartActionViewModel

# Clear app data
clear-data:
    adb shell pm clear zone.converge.android

# Uninstall app
uninstall:
    adb uninstall zone.converge.android

# ============================================
# Release
# ============================================

# Build signed release bundle (requires keystore)
bundle:
    ./gradlew bundleRelease

# Generate release notes from recent commits
release-notes:
    @echo "## Changes"
    @git log --oneline -10 | sed 's/^/- /'
