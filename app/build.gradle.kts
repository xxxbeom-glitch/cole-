plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.aptox.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aptox.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 36
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 전면 광고 — 출시 전 테스트 단위 ID (Google 공식 테스트)
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_AD_UNIT_ID",
            "\"ca-app-pub-3940256099942544/1033173712\"",
        )
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("dev") {
            dimension = "distribution"
            buildConfigField("boolean", "SHOW_DEBUG_MENU", "true")
            buildConfigField("boolean", "EXCLUDE_3MIN_OPTION", "false")
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/6300978111\"",
            )
            resValue("string", "app_name", "Aptox Dev")
        }
        create("externalTest") {
            dimension = "distribution"
            applicationId = "com.aptox.app"
            buildConfigField("boolean", "SHOW_DEBUG_MENU", "false")
            buildConfigField("boolean", "EXCLUDE_3MIN_OPTION", "true")
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-2691016483768478/4868696837\"",
            )
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("APTOX_KEYSTORE_PATH") as String)
            storePassword = project.findProperty("APTOX_KEYSTORE_PASSWORD") as String
            keyAlias = project.findProperty("APTOX_KEY_ALIAS") as String
            keyPassword = project.findProperty("APTOX_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isCrunchPngs = false  // PNG 리소스 컴파일 오류 회피 (ob_02~04_image 등)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }
}

dependencies {
    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation(libs.androidx.core.ktx)
    // 알림 접힘 상태에서 addAction 노출(MediaStyle.setShowActionsInCompactView)
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Play Billing (구독)
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // WorkManager + Room/Startup (명시 버전으로 전이 의존성 정렬 — WorkDatabase 초기화 안정화)
    // work-runtime-ktx 2.10.2는 Room 2.6.x와 함께 빌드됨. Room 2.6.1과 동일 계열로 유지할 것.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.startup.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/** 기기에 release/externalTest 서명 APK가 있을 때 devDebug 설치가 INSTALL_FAILED_UPDATE_INCOMPATIBLE로 실패하는 경우 대비 */
tasks.register<Exec>("uninstallAptoxForDevInstall") {
    group = "install"
    description = "연결 기기에서 com.aptox.app 제거 후 installDevDebug 등을 다시 시도하세요."
    commandLine("adb", "uninstall", "com.aptox.app")
    isIgnoreExitValue = true
}