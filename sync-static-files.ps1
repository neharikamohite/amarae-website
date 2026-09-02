# PowerShell Script: Sync frontend files to Spring Boot static resources
# Usage: .\sync-static-files.ps1
# This copies all frontend files from project root to src/main/resources/static/

Write-Host "Syncing frontend files to Spring Boot static resources..." -ForegroundColor Cyan

# Ensure static directory exists
$staticDir = "src\main\resources\static"
if (-not (Test-Path $staticDir)) {
    New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
    Write-Host "Created directory: $staticDir" -ForegroundColor Green
}

# Copy HTML files
$htmlFiles = @("index.html", "checkout.html", "chemistry.html", "collections.html", "findFragrance.html", "about.html", "cancellation-refund.html", "privacy-policy.html", "shipping-delivery.html", "terms-and-conditions.html")
foreach ($file in $htmlFiles) {
    if (Test-Path $file) {
        Copy-Item $file "$staticDir\" -Force
        Write-Host "Copied: $file" -ForegroundColor Green
    } else {
        Write-Host "Not found: $file (skipping)" -ForegroundColor Yellow
    }
}

# Copy CSS and JS files
$staticFiles = @("javas.js", "style.css")
foreach ($file in $staticFiles) {
    if (Test-Path $file) {
        Copy-Item $file "$staticDir\" -Force
        Write-Host "Copied: $file" -ForegroundColor Green
    } else {
        Write-Host "Not found: $file (skipping)" -ForegroundColor Yellow
    }
}

# Copy root favicon (browser tab icon)
if (Test-Path "favicon.ico") {
    Copy-Item "favicon.ico" "$staticDir\" -Force
    Write-Host "Copied: favicon.ico" -ForegroundColor Green
}

# Copy assets folder
if (Test-Path "assets") {
    Copy-Item -Path "assets" -Destination "$staticDir\" -Recurse -Force
    Write-Host "Copied: assets/ (recursive)" -ForegroundColor Green
} else {
    Write-Host "Not found: assets/ (skipping)" -ForegroundColor Yellow
}

Write-Host "`nSync complete! Run 'git add src/main/resources/static/' and commit to deploy." -ForegroundColor Cyan
