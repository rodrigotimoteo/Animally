#!/bin/sh
#
# Pre-commit hook — runs Detekt + ktlintCheck on staged Kotlin files.
# Installed via: ./gradlew installGitHooks
#

echo "🔍 Running static analysis (detekt + ktlintCheck)..."

# Run detekt and ktlintCheck
./gradlew detekt ktlintCheck --daemon --quiet
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ Lint/format violations found. Commit rejected."
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
