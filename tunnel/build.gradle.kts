import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val ndk = libs.versions.ndk.get()

val cmakeArgs = mutableListOf(
    "-DDEPS_DIR=${project.layout.buildDirectory.get()}/cmake",
    "-Dtunnel_sdk_VERSION=${libs.versions.ziti.tunnel.sdk.get()}",
    )

// use local checkouts if desired
// set in local.properties
// ziti.dir = /home/ziggy/work/ziti-sdk-c
with(gradleLocalProperties(parent!!.projectDir, providers)) {
    this["ziti.dir"]?.let { cmakeArgs.add("-DZITI_SDK_DIR=$it") }
    this["tlsuv.dir"]?.let { cmakeArgs.add("-Dtlsuv_DIR=$it") }
    this["tunnel.dir"]?.let { cmakeArgs.add("-Dtunnel_DIR=$it") }
}

android {
    namespace = "org.openziti.tunnel"
    compileSdk = 36
    ndkVersion = ndk

    defaultConfig {
        // VCPKG default triplets target 28 (as of 2025.04.09)
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                arguments(*cmakeArgs.toTypedArray())
            }
        }
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
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = properties["cmake.version"].toString()
        }
    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

val presets = listOf("arm-android", "arm64-android", "x64-android", "x86-android")

val buildNative = tasks.register("build-native-dependencies") {}

tasks.named("preBuild").dependsOn(buildNative)

if (!hasProperty("skipDependentBuild")) {
    val androidHome = System.getenv("ANDROID_SDK_ROOT") ?: """${System.getenv("HOME")}/Android/Sdk"""
    val ndkRoot = """$androidHome/ndk/${ndk}"""
    require(File(ndkRoot).isDirectory) {
        """NDK[$ndk] not found in ANDROID_HOME[$androidHome]"""
    }
    println("using NDK: $ndkRoot")
    presets.forEach { triplet ->
        val task = tasks.register<Exec>("build-native-deps-${triplet}") {
            executable("env")
            args("ANDROID_NDK_ROOT=${ndkRoot}", "cmake", "--preset", triplet)
        }
        buildNative.dependsOn(task)
    }
}