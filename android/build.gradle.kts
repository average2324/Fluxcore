import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jlleitschuh.gradle.ktlint")
}

val nativesArmeabiV7a by configurations.creating
val nativesArm64V8a by configurations.creating
val nativesX86 by configurations.creating
val nativesX8664 by configurations.creating
val freetypeArmeabiV7a by configurations.creating
val freetypeArm64V8a by configurations.creating
val freetypeX86 by configurations.creating
val freetypeX8664 by configurations.creating

val keystorePropertiesFile = project.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

fun envNonBlank(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = envNonBlank("ORBITFLUX_STORE_FILE")
    ?: keystoreProperties.getProperty("storeFile")
val releaseStorePassword = envNonBlank("ORBITFLUX_STORE_PASSWORD")
    ?: keystoreProperties.getProperty("storePassword")
val releaseKeyAlias = envNonBlank("ORBITFLUX_KEY_ALIAS")
    ?: keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = envNonBlank("ORBITFLUX_KEY_PASSWORD")
    ?: keystoreProperties.getProperty("keyPassword")
val releaseStoreFile = releaseStoreFilePath?.let { file(it) }
val hasReleaseKeystore =
    releaseStoreFile?.exists() == true &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

val runningOnCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
val useDebugSigningForRelease = !hasReleaseKeystore && runningOnCi

val configuredVersionCode = providers.gradleProperty("orbitfluxVersionCode").orNull?.toIntOrNull()
val configuredVersionName = providers.gradleProperty("orbitfluxVersionName").orNull

val resolvedVersionCode = envNonBlank("ORBITFLUX_VERSION_CODE")?.toIntOrNull()
    ?: configuredVersionCode
    ?: envNonBlank("GITHUB_RUN_NUMBER")?.toIntOrNull()
    ?: 2
val resolvedVersionName = envNonBlank("ORBITFLUX_VERSION_NAME")
    ?: configuredVersionName
    ?: "0.2.$resolvedVersionCode"

val admobAppId = envNonBlank("ORBITFLUX_ADMOB_APP_ID")
    ?: "ca-app-pub-3940256099942544~3347511713"
val admobBannerUnitId = envNonBlank("ORBITFLUX_ADMOB_BANNER_UNIT_ID")
    ?: "ca-app-pub-3940256099942544/6300978111"
val admobRewardedUnitId = envNonBlank("ORBITFLUX_ADMOB_REWARDED_UNIT_ID")
    ?: "ca-app-pub-3940256099942544/5224354917"
val premiumProductId = envNonBlank("ORBITFLUX_PREMIUM_PRODUCT_ID")
    ?: "fluxcore_premium"
val simulationStartLevel = (envNonBlank("ORBITFLUX_SIMULATION_START_LEVEL")?.toIntOrNull() ?: 1) - 1

android {
    namespace = "com.luminadigitale.fluxcore.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.orbitflux"
        minSdk = 26
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$admobBannerUnitId\"")
        buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$admobRewardedUnitId\"")
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"$premiumProductId\"")
        buildConfigField("boolean", "SIMULATION_MODE", "false")
        buildConfigField("int", "SIMULATION_START_LEVEL", "-1")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SIMULATION_MODE", "true")
            buildConfigField("int", "SIMULATION_START_LEVEL", simulationStartLevel.coerceAtLeast(0).toString())
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            } else if (useDebugSigningForRelease) {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(rootProject.file("assets"))
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    constraints {
        implementation("androidx.activity:activity:1.10.1") {
            because("Avoid bundling the outdated 1.2.3 activity runtime pulled transitively by Play libraries")
        }
        implementation("androidx.fragment:fragment:1.8.9") {
            because("Avoid bundling the outdated 1.1.0 fragment runtime flagged by the Play SDK Index")
        }
        implementation("androidx.core:core") {
            version {
                strictly("1.16.0")
            }
            because("Keep AndroidX core on the latest compileSdk 35-compatible release while using libGDX 1.14.2")
        }
        implementation("androidx.core:core-ktx") {
            version {
                strictly("1.16.0")
            }
            because("Keep AndroidX core on the latest compileSdk 35-compatible release while using libGDX 1.14.2")
        }
    }

    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:1.14.2")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.14.2")
    implementation("com.badlogicgames.gdx:gdx-freetype:1.14.2")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.android.billingclient:billing:7.1.1")

    nativesArmeabiV7a("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-armeabi-v7a")
    nativesArm64V8a("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-arm64-v8a")
    nativesX86("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-x86")
    nativesX8664("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-x86_64")
    freetypeArmeabiV7a("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-armeabi-v7a")
    freetypeArm64V8a("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-arm64-v8a")
    freetypeX86("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-x86")
    freetypeX8664("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-x86_64")
}

val extractAndroidNatives by tasks.registering(Sync::class) {
    into(layout.projectDirectory.dir("src/main/jniLibs"))

    from({ zipTree(nativesArmeabiV7a.singleFile) }) {
        include("*.so")
        into("armeabi-v7a")
    }
    from({ zipTree(freetypeArmeabiV7a.singleFile) }) {
        include("*.so")
        into("armeabi-v7a")
    }
    from({ zipTree(nativesArm64V8a.singleFile) }) {
        include("*.so")
        into("arm64-v8a")
    }
    from({ zipTree(freetypeArm64V8a.singleFile) }) {
        include("*.so")
        into("arm64-v8a")
    }
    from({ zipTree(nativesX86.singleFile) }) {
        include("*.so")
        into("x86")
    }
    from({ zipTree(freetypeX86.singleFile) }) {
        include("*.so")
        into("x86")
    }
    from({ zipTree(nativesX8664.singleFile) }) {
        include("*.so")
        into("x86_64")
    }
    from({ zipTree(freetypeX8664.singleFile) }) {
        include("*.so")
        into("x86_64")
    }
}

tasks.named("preBuild") {
    dependsOn(extractAndroidNatives)
}

val packageReleaseNativeDebugSymbols by tasks.registering(Zip::class) {
    group = "build"
    description = "Packages native debug symbols for Play Console upload."
    archiveFileName.set("native-debug-symbols.zip")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/native-debug-symbols/release"))
    from(layout.projectDirectory.dir("src/main/jniLibs")) {
        include("**/*.so")
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    finalizedBy(packageReleaseNativeDebugSymbols)
}
