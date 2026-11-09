import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
    alias(libs.plugins.ksp)
}

android {
    ndkVersion = "28.2.13676358"
    namespace = "com.example.animepahedl"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.anilili.shortdrama"
        minSdk = 35
        targetSdk = 37
        versionCode = 6
        versionName = "1.2.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["listener"] = "com.example.animepahedl.ThreadTraceListener"
    }

    val releaseKeystore = providers.environmentVariable("ANIMEPAHE_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) create("personalRelease") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("ANIMEPAHE_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("ANIMEPAHE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("ANIMEPAHE_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("personalRelease")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        jniLibs {
            // FFmpeg is an executable launched from nativeLibraryDir, not loaded via JNI.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libffmpeg_exec.so"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:${libs.versions.okhttp.get()}")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.media3:media3-transformer:${libs.versions.media3.get()}")
    implementation("androidx.media3:media3-session:${libs.versions.media3.get()}")
    implementation("androidx.media3:media3-exoplayer-dash:${libs.versions.media3.get()}")
    implementation("androidx.media3:media3-datasource-cronet:${libs.versions.media3.get()}")
    implementation("androidx.media3:media3-cast:${libs.versions.media3.get()}")
    implementation("com.google.android.gms:play-services-cronet:18.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.materialkolor:material-color-utilities:2.1.1")
    implementation("androidx.compose.material:material:1.7.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-android:0.19.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("dev.chrisbanes.haze:haze:1.2.2")
    implementation(libs.work.runtime)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.media3.common)
    implementation(libs.media3.exoplayer)
    implementation("androidx.media3:media3-ui:${libs.versions.media3.get()}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:${libs.versions.okhttp.get()}")
    testImplementation("com.squareup.okhttp3:okhttp-tls:${libs.versions.okhttp.get()}")
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Fail packaging early if a checkout omitted the native exporter.
val verifyBundledFfmpeg = tasks.register("verifyBundledFfmpeg") {
    val executable = layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libffmpeg_exec.so")
    inputs.file(executable)
    doLast {
        val digest = MessageDigest.getInstance("SHA-256").digest(executable.asFile.readBytes()).joinToString("") { "%02x".format(it) }
        require(digest == "9085507b0dc32643b4d6d084a7e7d3469ef17907a7ba15c22d3997ed09c932aa") { "Bundled FFmpeg checksum mismatch" }
        require(executable.asFile.length() > 1_000_000) {
            "Missing ARM64 FFmpeg executable: ${executable.asFile}"
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyBundledFfmpeg) }

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
