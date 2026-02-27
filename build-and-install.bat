@echo off
cd /d "%~dp0"
REM JAVA_HOME이 없을 경우 Android Studio JBR 사용
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

if "%~1"=="" (
    call gradlew.bat clean installDebug
) else (
    call gradlew.bat %*
)
