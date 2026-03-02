# issue-bridge.ps1
# 이슈 내용과 관련 파일을 ZIP으로 묶어 프로젝트 루트/issues 폴더에 저장
# 사용법: .\issue-bridge.ps1 -IssueContent "..." -RelatedFiles @("파일1", "파일2")
#       .\issue-bridge.ps1 -IssueContentPath ".\issue.md" -RelatedFiles @("파일1", "파일2")

param(
    [string]$IssueContent = "",
    [string]$IssueContentPath = "",
    [string[]]$RelatedFiles = @(),
    [string]$ScreenshotsFolder = "$PSScriptRoot\screenshots"
)

# ── 설정 ──────────────────────────────────────────────
$timestamp  = Get-Date -Format "yyyy-MM-dd HH-mm"
$zipName    = "$timestamp.zip"
$issuesDir  = Join-Path $PSScriptRoot "issues"
$zipPath    = Join-Path $issuesDir $zipName
$tempDir    = Join-Path $env:TEMP "issue-bridge-$([System.Guid]::NewGuid().ToString('N').Substring(0,8))"

# issues 폴더 없으면 자동 생성
if (-not (Test-Path $issuesDir)) {
    New-Item -ItemType Directory -Path $issuesDir | Out-Null
    Write-Host "📁 issues 폴더 생성됨" -ForegroundColor Cyan
}

# ── 임시 폴더 생성 ────────────────────────────────────
New-Item -ItemType Directory -Path $tempDir | Out-Null
New-Item -ItemType Directory -Path "$tempDir\files" | Out-Null
New-Item -ItemType Directory -Path "$tempDir\screenshots" | Out-Null

Write-Host "📁 임시 폴더 생성: $tempDir" -ForegroundColor Cyan

# ── issue.md 저장 ─────────────────────────────────────
if ($IssueContentPath -ne "") {
    $resolvedPath = if ([System.IO.Path]::IsPathRooted($IssueContentPath)) { $IssueContentPath } else { Join-Path $PSScriptRoot $IssueContentPath }
    if (Test-Path $resolvedPath) {
        $IssueContent = [System.IO.File]::ReadAllText($resolvedPath, [System.Text.Encoding]::UTF8)
    }
}
if ($IssueContent -ne "") {
    # 타임스탬프 삽입
    $IssueContent = $IssueContent -replace "\[타임스탬프\]", (Get-Date -Format "yyyy년 MM월 dd일 HH:mm")
    $IssueContent | Out-File -FilePath "$tempDir\issue.md" -Encoding UTF8
    Write-Host "✅ issue.md 생성 완료" -ForegroundColor Green
} else {
    # 기본 템플릿으로 생성
    @"
# 🐛 이슈 요약

## 문제 설명
(내용을 입력해주세요)

## 발생 상황
- 언제:
- 어디서:
- 증상:

## 에러 메시지
```
(에러 메시지)
```

## 시도한 방법
-

## 관련 파일
-

## 예상 원인
-

---
*생성 시각: $(Get-Date -Format "yyyy년 MM월 dd일 HH:mm")*
*프로젝트: cole (디지털 디톡스 앱)*
*환경: Android / Kotlin + Jetpack Compose*
"@ | Out-File -FilePath "$tempDir\issue.md" -Encoding UTF8
    Write-Host "⚠️  이슈 내용 없음 → 기본 템플릿으로 생성" -ForegroundColor Yellow
}

# ── 관련 파일 복사 ────────────────────────────────────
$copiedCount = 0
foreach ($file in $RelatedFiles) {
    if (Test-Path $file) {
        # 상대 경로 구조 유지하며 복사
        $fileName = Split-Path $file -Leaf
        $destPath = "$tempDir\files\$fileName"
        
        # 같은 이름 파일 있으면 상위 폴더명 붙이기
        if (Test-Path $destPath) {
            $parentFolder = Split-Path (Split-Path $file -Parent) -Leaf
            $destPath = "$tempDir\files\${parentFolder}_${fileName}"
        }
        
        Copy-Item $file $destPath
        $copiedCount++
        Write-Host "  📄 복사: $file" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠️  파일 없음 (건너뜀): $file" -ForegroundColor Yellow
    }
}
Write-Host "✅ 관련 파일 $copiedCount개 복사 완료" -ForegroundColor Green

# ── 스크린샷 복사 ─────────────────────────────────────
$screenshotCount = 0
if (Test-Path $ScreenshotsFolder) {
    $screenshots = Get-ChildItem $ScreenshotsFolder -Include "*.png","*.jpg","*.jpeg","*.webp" -Recurse
    foreach ($ss in $screenshots) {
        Copy-Item $ss.FullName "$tempDir\screenshots\$($ss.Name)"
        $screenshotCount++
        Write-Host "  🖼️  스크린샷: $($ss.Name)" -ForegroundColor Gray
    }
    if ($screenshotCount -gt 0) {
        Write-Host "✅ 스크린샷 $screenshotCount개 복사 완료" -ForegroundColor Green
        # 복사 후 원본 삭제 (선택)
        # Remove-Item $ScreenshotsFolder\* -Recurse -Force
    }
} else {
    Write-Host "ℹ️  스크린샷 폴더 없음 (건너뜀): $ScreenshotsFolder" -ForegroundColor Gray
}

# ── ZIP 생성 ──────────────────────────────────────────
try {
    Compress-Archive -Path "$tempDir\*" -DestinationPath $zipPath -Force
    Write-Host ""
    Write-Host "🎉 ZIP 생성 완료!" -ForegroundColor Green
    Write-Host "   📦 파일명: $zipName" -ForegroundColor White
    Write-Host "   📍 위치: issues\$zipName" -ForegroundColor White
    Write-Host "   📄 issue.md" -ForegroundColor Gray
    Write-Host "   📁 files\ ($copiedCount개)" -ForegroundColor Gray
    Write-Host "   🖼️  screenshots\ ($screenshotCount개)" -ForegroundColor Gray
} catch {
    Write-Host "❌ ZIP 생성 실패: $_" -ForegroundColor Red
} finally {
    # 임시 폴더 정리
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}

# ── 바탕화면 열기 (선택) ──────────────────────────────
# explorer.exe $desktop
