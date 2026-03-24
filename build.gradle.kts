// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

// aptox: AAB 빌드 (Play Store 업로드용)
tasks.register("aptox") {
    dependsOn(":app:bundleDevRelease")
    group = "aptox"
    description = "AAB 빌드 (dev release). Play Store 업로드용"
}
// aptoxDebug: 디버그 APK (release 리소스 오류 시 사용)
tasks.register("aptoxDebug") {
    dependsOn(":app:assembleDevDebug")
    group = "aptox"
    description = "디버그 APK 빌드"
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