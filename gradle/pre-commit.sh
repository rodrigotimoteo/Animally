#!/bin/sh
#
# Pre-commit hook — runs static analysis and the primary iOS Kotlin compilation.
# Installed via: ./gradlew installGitHooks
#

echo "🔍 Running static analysis and iOS compilation..."

./scripts/check-queries-module.sh
if [ $? -ne 0 ]; then
    echo "❌ SQLDelight query ownership check failed. Commit rejected."
    exit 1
fi

# Keep the iOS-first target compiling as well as formatted. This task is
# incremental, so unchanged platform code remains cheap on repeat commits.
./gradlew detekt ktlintCheck :shared:compileKotlinIosSimulatorArm64 --daemon --quiet
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ Static analysis or iOS compilation failed. Commit rejected."
    echo "   Fix reported issues and stage the changes, or use --no-verify to bypass."
    echo ""
    echo "   Quick fix (auto-format):"
    echo "     ./gradlew ktlintFormat detekt"
    echo ""
    echo "   Generate/update baseline (acknowledge current violations):"
    echo "     ./gradlew detektBaselineAll"
    echo ""
    exit 1
fi

echo "✅ Static analysis passed."
exit 0
