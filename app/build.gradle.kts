plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.16"
}

android {
    namespace = "com.api.debug.helper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.api.debug.helper"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // CI 环境：从环境变量读取签名信息
            // 本地环境：从 local.properties 或跳过签名
            val storeFilePath = System.getenv("SIGNING_STORE_FILE") 
                ?: rootProject.file("local.properties").let { 
                    if (it.exists()) {
                        val props = java.util.Properties()
                        props.load(it.inputStream())
                        props.getProperty("SIGNING_STORE_FILE", "")
                    } else ""
                }
            
            if (storeFilePath.isNotEmpty() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") 
                    ?: (rootProject.file("local.properties").let {
                        val props = java.util.Properties()
                        if (it.exists()) props.load(it.inputStream())
                        props.getProperty("SIGNING_STORE_PASSWORD", "")
                    })
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") 
                    ?: (rootProject.file("local.properties").let {
                        val props = java.util.Properties()
                        if (it.exists()) props.load(it.inputStream())
                        props.getProperty("SIGNING_KEY_ALIAS", "")
                    })
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") 
                    ?: (rootProject.file("local.properties").let {
                        val props = java.util.Properties()
                        if (it.exists()) props.load(it.inputStream())
                        props.getProperty("SIGNING_KEY_PASSWORD", "")
                    })
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 只有签名文件存在时才使用签名配置，否则生成 unsigned APK
            signingConfig = if (signingConfigs.getByName("release").storeFile?.exists() == true) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}