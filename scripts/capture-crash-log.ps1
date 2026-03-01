# 앱 크래시 로그 캡처
# 사용법: powershell -File capture-crash-log.ps1
# 1. 앱 재설치 후 이 스크립트 실행
# 2. 앱 실행 → 크래시 발생 시 로그가 자동 저장됨
# 3. Ctrl+C 로 중지 후 cole-crash-*.log 파일 확인

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logDir = Join-Path $PSScriptRoot ".."
$logFile = Join-Path $logDir "cole-crash-$timestamp.log"

adb logcat -c
Write-Host "로그 캡처 중... (Ctrl+C 로 중지)" -ForegroundColor Cyan
Write-Host "저장 위치: $logFile" -ForegroundColor Gray
adb logcat -v threadtime > $logFile 2>&1
