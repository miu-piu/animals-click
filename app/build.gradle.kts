plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.miupiu.zveryata"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miupiu.zveryata"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val verifyCanonicalAssets by tasks.registering {
    doLast {
        val imagesDir = file("src/main/assets/images")
        val images = imagesDir.listFiles()?.filter { it.extension.equals("webp", ignoreCase = true) } ?: emptyList()
        check(images.size == 59) {
            "Expected exactly 59 canonical WebP images in ${imagesDir.path}, found ${images.size}. Run: python3 scripts/sync_assets.py"
        }
        val icon = file("src/main/res/drawable-nodpi/zveryata_icon.png")
        check(icon.exists()) {
            "Missing canonical launcher icon ${icon.path}. Run: python3 scripts/sync_assets.py"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyCanonicalAssets)
}
