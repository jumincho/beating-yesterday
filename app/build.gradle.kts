import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * The optional Food Safety Korea OpenAPI key, from `local.properties` or the `FOOD_API_KEY`
 * environment variable. Without it the app still builds; food lookup is simply disabled.
 */
val foodApiKey: String = run {
    val localProperties = Properties()
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(localProperties::load)
    localProperties.getProperty("FOOD_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.environmentVariable("FOOD_API_KEY").orNull?.trim()
        ?: ""
}

/**
 * `-PrecordScreenshots` makes the screenshot tests write the README screenshots to
 * `docs/screenshots` instead of only rendering the screens.
 */
val recordScreenshots: Boolean = providers.gradleProperty("recordScreenshots").isPresent

android {
    namespace = "com.jumincho.beatingyesterday"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jumincho.beatingyesterday"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        val escapedKey = foodApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "FOOD_API_KEY", "\"$escapedKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    testOptions {
        unitTests {
            // The Robolectric screenshot tests render the app's real resources.
            isIncludeAndroidResources = true
            all { test ->
                // JUnit Jupiter for the ViewModel tests, JUnit Vintage for the Robolectric tests.
                test.useJUnitPlatform()
                test.systemProperty("roborazzi.test.record", recordScreenshots.toString())
                test.systemProperty("screenshots.dir", rootDir.resolve("docs/screenshots").path)
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        textReport = true
        // Newer AndroidX releases need AGP 9 and compileSdk 37; the versions in the catalog are the newest
        // that work with AGP 8.13, so these network-based "newer version available" checks are noise here.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(testFixtures(project(":core")))
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}
