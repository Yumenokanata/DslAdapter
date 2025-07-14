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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.10.2")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.recyclerview)

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation(project(":dsladapter"))
    implementation(project(":dsladapter-livedata"))
    implementation(project(":dsladapter-rx2"))
    implementation(project(":dsladapter-rx3"))
    implementation(project(":dsladapter-position"))
    implementation(project(":dsladapter-paging"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.arrow.core.data)

    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")

    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test:runner:1.1.1")
    androidTestImplementation("androidx.test:rules:1.1.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1")
}
