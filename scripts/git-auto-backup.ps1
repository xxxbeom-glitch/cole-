# cole 프로젝트 30분마다 Git 자동 백업 스크립트
$ErrorActionPreference = "SilentlyContinue"
$projectPath = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $projectPath

# JAVA_HOME 미설정 시 Android Studio JBR 사용
if (-not $env:JAVA_HOME -and (Test-Path "C:\Program Files\Android\Android Studio\jbr\bin\java.exe")) {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
git add -A 2>$null
$status = git status --porcelain 2>$null

if ($status) {
    git commit -m "Auto backup: $timestamp" 2>$null
    git push origin main 2>$null
}
