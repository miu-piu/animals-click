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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 20001
        versionName = "2.0.0-preview.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val verifyCanonicalAssets by tasks.registering {
    doLast {
        val imagesDir = file("src/main/assets/images")
        val runtimeCatalog = file("src/main/assets/catalog.json")
        check(runtimeCatalog.exists()) {
            "Missing staged catalog.json. Run scripts/package_content_v2.py first."
        }
        val assets = Regex("\"asset\"\\s*:\\s*\"([^\"]+\\.webp)\"")
            .findAll(runtimeCatalog.readText(Charsets.UTF_8))
            .map { it.groupValues[1] }.toList()
        check(assets.isNotEmpty() && assets.size == assets.distinct().size) {
            "Runtime catalog is empty or contains duplicate image filenames"
        }
        val actual = imagesDir.listFiles()
            ?.filter { it.extension.equals("webp", ignoreCase = true) }
            ?.map { it.name }?.toSet() ?: emptySet()
        check(actual == assets.toSet()) {
            "Runtime images differ from catalog: missing=" + (assets.toSet() - actual) +
                "; extra=" + (actual - assets.toSet())
        }
        check(file("src/main/res/drawable-nodpi/zveryata_icon.png").exists()) {
            "Missing approved launcher icon"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyCanonicalAssets)
}
