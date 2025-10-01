plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("maven-publish")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.meta.brain"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        version = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ADS_LIVE", "true")
            buildConfigField("boolean", "APP_DEBUG", "false")
        }
        debug{
            buildConfigField("boolean", "ADS_LIVE", "false")
            buildConfigField("boolean", "APP_DEBUG", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])

                    groupId = "com.github.quanvh"
                    artifactId = "metabrain"
                    version = "1.0.3"
                }
            }
            repositories {
                maven {
                    val githubUser = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER")
                    val githubToken = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

                    url = uri("https://maven.pkg.github.com/quanvh/metabrain_base")
                    credentials {
                        username = githubUser
                        password = githubToken
                    }
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.hilt.android)
    implementation(libs.glide)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.airbnb.android:lottie:6.6.9")
    implementation(libs.circleimageview)
    implementation(libs.progressview)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    // Firebase BoM
    implementation(platform ("com.google.firebase:firebase-bom:33.2.0"))

    // Firebase SDKs
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // Admob
    implementation(libs.user.messaging.platform)
    implementation(libs.play.services.ads)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.gson)
    implementation(libs.billing)
    implementation(libs.billing.ktx)

    implementation(libs.review)
    implementation(libs.review.ktx)

    implementation(libs.material)
}