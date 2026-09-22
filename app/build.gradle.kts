plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.madlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.madlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 240
        versionName = "0.9.116.11"
        val publicApk = (findProperty("MAD_PUBLIC_APK") as? String)?.equals("true", ignoreCase = true) == true
        val repoToken = if (publicApk) {
            ""
        } else {
            run {
                val fromProp = findProperty("MAD_REPO_TOKEN") as? String
                if (fromProp != null) return@run fromProp.trim()
                val f = rootProject.file("local.properties")
                if (!f.exists()) return@run ""
                f.readLines()
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("MAD_REPO_TOKEN=") }
                    ?.substringAfter("=")
                    ?.trim()
                    .orEmpty()
            }
        }
        val shareUrl = run {
            val fromProp = (findProperty("MAD_SHARE_APK_URL") as? String)?.trim().orEmpty()
            if (fromProp.isNotEmpty()) return@run fromProp
            val f = rootProject.file("local.properties")
            if (!f.exists()) return@run ""
            f.readLines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("MAD_SHARE_APK_URL=") }
                ?.substringAfter("=")
                ?.trim()
                .orEmpty()
        }
        buildConfigField(
            "String",
            "MAD_REPO_TOKEN",
            "\"${repoToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
        buildConfigField(
            "String",
            "MAD_SHARE_APK_URL",
            "\"${shareUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
        // Distributor builds only (token present). Public APKs: -PMAD_PUBLIC_APK=true
        buildConfigField("boolean", "SHOW_APK_SHARE", (!publicApk && repoToken.isNotEmpty()).toString())
    }

    flavorDimensions += "product"
    productFlavors {
        create("launcher") {
            dimension = "product"
            isDefault = true
            applicationId = "com.madlauncher"
            buildConfigField("boolean", "ACTION_BOX_APP", "false")
        }
        create("actionbox") {
            dimension = "product"
            applicationId = "com.madlauncher.actionbox"
            buildConfigField("boolean", "ACTION_BOX_APP", "true")
        }
    }

    signingConfigs {
        create("madDebug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("madDebug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("madDebug")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register<Copy>("syncLangPacks") {
    from(rootProject.file("Lang")) {
        include("*.json", "README.txt")
        exclude("eng.json", "en.json", "download-ids.json")
    }
    into("src/main/assets/lang")
    onlyIf { rootProject.file("Lang").isDirectory }
}

tasks.named("preBuild") {
    dependsOn("syncLangPacks")
}

fun archiveVersionedApk(product: String, apk: File) {
    val version = android.defaultConfig.versionName ?: "unknown"
    val dir = rootProject.file("versions")
    dir.mkdirs()
    dir.listFiles()
        ?.filter { it.isFile && it.name.startsWith("$product-") && it.name.endsWith("-latest.apk") }
        ?.forEach { latest ->
            val demoted = File(dir, latest.name.removeSuffix("-latest.apk") + ".apk")
            if (demoted.exists()) demoted.delete()
            latest.renameTo(demoted)
        }
    if (apk.exists()) {
        apk.copyTo(File(dir, "$product-$version-latest.apk"), overwrite = true)
    }
}

android.applicationVariants.configureEach {
    val apkFile = outputs.first().outputFile
    val pack = packageApplicationProvider
    when (name) {
        "launcherDebug" -> {
            val copy = tasks.register<Copy>("syncLauncherDebugApk") {
                dependsOn(pack)
                from(apkFile.parentFile)
                include(apkFile.name)
                into(layout.buildDirectory.dir("outputs/apk/debug"))
                rename { "app-debug.apk" }
            }
            val archive = tasks.register("archiveLauncherDebugApk") {
                dependsOn(copy)
                doLast {
                    val synced = rootProject.file("app/build/outputs/apk/debug/app-debug.apk")
                    archiveVersionedApk("MAD", if (synced.exists()) synced else apkFile)
                }
            }
            assembleProvider.configure { finalizedBy(copy, archive) }
        }
        "actionboxDebug" -> {
            val archive = tasks.register("archiveActionboxDebugApk") {
                dependsOn(pack)
                doLast { archiveVersionedApk("ActionBox", apkFile) }
            }
            assembleProvider.configure { finalizedBy(archive) }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
