// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// aptox: APK 빌드만 수행 (기기 미연결 시 installDebug 실패 방지)
tasks.register("aptox") {
    dependsOn(":app:assembleDevDebug")
    group = "aptox"
    description = "APK 빌드 (dev). 기기 연결 후 설치하려면 installDevDebug 실행"
}
tasks.register("aptoxTest") {
    dependsOn(":app:assembleExternalTestDebug")
    group = "aptox"
    description = "외부 테스트용 APK 빌드 (externalTest, 디버그메뉴 숨김, 3분 제외)"
    doLast {
        val apkDir = file("app/build/outputs/apk/externalTest/debug")
        val apk = apkDir.listFiles()?.find { it.name.endsWith(".apk") }
        if (apk != null) {
            val versionName = "1.0"  // app/build.gradle.kts defaultConfig.versionName과 동기화
            val dest = file("app/build/outputs/apk/aptox-test-$versionName.apk")
            apk.copyTo(dest, overwrite = true)
            println("APK 복사됨: ${dest.absolutePath}")
        }
    }
}