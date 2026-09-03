#!/bin/bash
# Bash Script: Sync frontend files to Spring Boot static resources
# Usage: ./sync-static-files.sh
# This copies all frontend files from project root to src/main/resources/static/

echo "Syncing frontend files to Spring Boot static resources..."

# Ensure static directory exists
STATIC_DIR="src/main/resources/static"
mkdir -p "$STATIC_DIR"

# Copy HTML files
for file in index.html checkout.html chemistry.html collections.html findFragrance.html about.html cancellation-refund.html privacy-policy.html shipping-delivery.html terms-and-conditions.html account.html admin.html; do
    if [ -f "$file" ]; then
        cp "$file" "$STATIC_DIR/"
        echo "Copied: $file"
    else
        echo "Not found: $file (skipping)"
    fi
done

# Copy CSS and JS files
for file in javas.js style.css account.js admin.js; do
    if [ -f "$file" ]; then
        cp "$file" "$STATIC_DIR/"
        echo "Copied: $file"
    else
        echo "Not found: $file (skipping)"
    fi
done

# Copy root favicon (browser tab icon)
if [ -f "favicon.ico" ]; then
    cp "favicon.ico" "$STATIC_DIR/"
    echo "Copied: favicon.ico"
fi

# Copy assets folder
if [ -d "assets" ]; then
    cp -r assets "$STATIC_DIR/"
    echo "Copied: assets/ (recursive)"
else
    echo "Not found: assets/ (skipping)"
fi

echo ""
echo "Sync complete! Run 'git add src/main/resources/static/' and commit to deploy."
