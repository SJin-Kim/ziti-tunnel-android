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
                // Build only armeabi-v7a for faster builds
                abiFilters("armeabi-v7a")
            }
        }
        // Also set ndk abiFilters
        ndk {
            abiFilters.add("armeabi-v7a")
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
    val ndkRoot = android.ndkDirectory.absolutePath
    val vcpkgRoot = System.getenv("VCPKG_ROOT") ?: "${System.getProperty("user.home")}/vcpkg"
    val cmakePath = System.getenv("CMAKE_PATH") ?: "${System.getenv("ANDROID_SDK_ROOT")}/cmake/3.30.3/bin"
    val ninjaPath = System.getenv("NINJA_PATH") ?: "${System.getProperty("user.home")}/bin"

    println("using NDK: $ndkRoot")
    println("using VCPKG_ROOT: $vcpkgRoot")
    println("using CMAKE_PATH: $cmakePath")
    println("using NINJA_PATH: $ninjaPath")

    presets.forEach { triplet ->
        val task = tasks.register<Exec>("build-native-deps-${triplet}") {
            executable("cmake")
            args("--preset", triplet)
            workingDir(file("${projectDir}"))
            environment("ANDROID_NDK_ROOT", ndkRoot)
            environment("VCPKG_ROOT", vcpkgRoot)
            environment("PATH", "${cmakePath}${File.pathSeparator}${ninjaPath}${File.pathSeparator}${System.getenv("PATH")}")
        }
        buildNative.dependsOn(task)
    }
}