import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "indi.yume.tools.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "indi.yume.tools.sample"
        minSdk = 21
        targetSdk = 36
        versionCode = (project.property("VERSION_CODE") as String).toInt()
        versionName = project.property("VERSION_NAME") as String

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
    implementation(libs.coroutines.rx2)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":dsladapter"))
    implementation(project(":dsladapter-livedata"))
    implementation(project(":dsladapter-rx2"))
    implementation(project(":dsladapter-rx3"))
    implementation(project(":dsladapter-position"))
    implementation(project(":dsladapter-paging"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.arrow.core.data)
    implementation(libs.rxandroid2)
    implementation(libs.rxkotlin2)
    
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
