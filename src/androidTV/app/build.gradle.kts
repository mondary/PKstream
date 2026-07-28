import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val adultVersionCode = 43
val adultVersionName = "2026.08.43"
val kidsVersionCode = 43
val kidsVersionName = "2026.08.43-kids"

android {
    namespace = "design.mondary.pkstream"
    compileSdk = 35

    defaultConfig {
        applicationId = "design.mondary.pkstream"
        minSdk = 23
        targetSdk = 35
        versionCode = adultVersionCode
        versionName = adultVersionName
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions += "edition"
    productFlavors {
        create("adult") {
            dimension = "edition"
            versionCode = adultVersionCode
            versionName = adultVersionName
            manifestPlaceholders["appLabel"] = "PK Stream"
            manifestPlaceholders["appIcon"] = "adult_original_icon"
            manifestPlaceholders["appBanner"] = "adult_original_banner"
            buildConfigField("boolean", "IS_KIDS", "false")
        }
        create("kids") {
            dimension = "edition"
            applicationIdSuffix = ".kids"
            versionCode = kidsVersionCode
            versionName = kidsVersionName
            manifestPlaceholders["appLabel"] = "PK Stream Kids"
            manifestPlaceholders["appIcon"] = "pk_stream_icon"
            manifestPlaceholders["appBanner"] = "tv_banner"
            buildConfigField("boolean", "IS_KIDS", "true")
        }
    }

}

kotlin { jvmToolchain(17) }

androidComponents {
    onVariants(selector().all()) { variant ->
        val isKids = variant.productFlavors.any { it.second == "kids" }
        val outputName = if (isKids) "PK-Stream-Kids-TV-$kidsVersionName.apk" else "PK-Stream-TV-$adultVersionName.apk"
        variant.outputs.forEach { output ->
            (output as VariantOutputImpl).outputFileName.set(outputName)
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    debugImplementation(libs.compose.ui.tooling)
}
