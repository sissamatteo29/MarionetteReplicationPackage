#!/bin/bash
# Simple cross-platform browser opener
# Usage: ./open-browser.sh <url>

set -e

# Check if URL parameter is provided
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 <url>"
    echo "Example: $0 http://localhost:8080"
    exit 1
fi

URL="$1"

# Validate URL format (basic check)
if [[ ! "$URL" =~ ^https?:// ]]; then
    echo "Warning: URL should start with http:// or https://"
    echo "Proceeding anyway with: $URL"
fi

echo "Opening: $URL"

# Detect operating system and open browser
case "$(uname -s)" in
    Darwin)
        # macOS
        open "$URL"
        ;;
    Linux)
        # Linux
        xdg-open "$URL"
        ;;
    CYGWIN*|MINGW*|MSYS*)
        # Windows (Git Bash, Cygwin, etc.)
        start "$URL"
        ;;
    *)
        echo "Error: Unsupported operating system: $(uname -s)"
        echo "Please manually open: $URL"
        exit 1
        ;;
esac

echo "✅ Browser opened successfully"