plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun gitValue(vararg arguments: String): String = runCatching {
    providers.exec {
        commandLine("git", *arguments)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("")

val sourceCommit = gitValue("rev-parse", "HEAD").ifBlank { "unknown" }
val sourceDirty = gitValue("status", "--porcelain").isNotBlank()
val releaseVersionName = "0.2.0-alpha07"
val releaseVersionCode = 8
val exactBuildId = "$releaseVersionName+$releaseVersionCode-${sourceCommit.take(12)}${if (sourceDirty) "-dirty" else ""}"

android {
    namespace = "com.shadow.calorietracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shadow.calorietracker"
        minSdk = 23
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        buildConfigField("String", "SOURCE_COMMIT", "\"$sourceCommit\"")
        buildConfigField("boolean", "SOURCE_DIRTY", sourceDirty.toString())
        buildConfigField("String", "BUILD_ID", "\"$exactBuildId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "CalorieQuick-${output.versionName.get()}-${variant.buildType}.apk",
            )
        }
    }
}
