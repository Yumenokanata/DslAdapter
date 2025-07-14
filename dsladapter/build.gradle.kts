import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "indi.yume.tools.dsladapter"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        dataBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.recyclerview)

    api(libs.arrow.core.data)

    testImplementation("junit:junit:4.12")
    testImplementation(libs.arrow.test)
    androidTestImplementation("androidx.test:runner:1.1.1")
    androidTestImplementation("androidx.test:rules:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1")
}

apply(from = rootProject.file("gradle/gradle-mvn-push.gradle"))