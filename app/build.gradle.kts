import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.lsparanoid)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.navigation.safe.args)
}

lsparanoid {
    includeDependencies = false
    variantFilter = { variant -> variant.name == "release" }
}

android {
    namespace = "com.luckyzyx.luckytool"
    compileSdk {
        version = release(rootProject.extra.get("compileSdkVersion") as Int) {
//            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = "com.luckyzyx.luckytool"

        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int

        versionCode = getVersionCode()
        versionName = "1.3.5_beta"
        ndk.abiFilters.addAll(arrayOf("arm64-v8a"/*, "armeabi-v7a", "x86", "x86_64"*/))
    }
    signingConfigs {
        all {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = null

            storeFile = file(rootProject.extra.get("storeFile") as String)
            storePassword = rootProject.extra.get("storePassword") as String
            keyAlias = rootProject.extra.get("keyAlias") as String
            keyPassword = rootProject.extra.get("keyPassword") as String
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = false
            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        aidl = true
        viewBinding = true
        buildConfig = true
    }
    androidResources.additionalParameters.addAll(
        arrayOf("--allow-reserved-package-id", "--package-id", "0x64")
    )
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    jvmToolchain(rootProject.extra.get("jdkVersion") as Int)
}

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: "None"
        variant.outputs.forEach { output ->
            val versionName = output.versionName.get()
            val versionCode = output.versionCode.get()
            val version = "$versionName($versionCode)"
            println("buildVersion -> $version ($buildType)")

            output.outputFileName.set("LuckyTool_v${version}_${buildType}.apk")
            println("outputFileName -> ${output.outputFileName.get()}")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenApiStub)
    implementation(projects.colorpicker)

    @Suppress("AvoidDuplicateDependencies")
    platform(libs.yukihook.bom).apply {
        implementation(this)
        ksp(this)
    }
    implementation(libs.yukihook.core)
    ksp(libs.yukihook.compiler)
    implementation(libs.yukihook.runtime.libxposed)

    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    implementation(libs.kavaref.core)
    implementation(libs.kavaref.android)
    implementation(libs.kavaref.extension)

    implementation(libs.dexkit)
    implementation(libs.hiddenapibypass)

    //AndroidX
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //Android UI
    implementation(libs.material)
    implementation(libs.betterandroid.ui.component)
    implementation(libs.betterandroid.ui.component.adapter)
    implementation(libs.betterandroid.ui.extension)
    implementation(libs.betterandroid.system.extension)
    implementation(libs.hikage.core)
    implementation(libs.hikage.compiler)
    implementation(libs.hikage.extension)
    implementation(libs.hikage.extension.betterandroid)
    implementation(libs.hikage.widget.androidx)
    implementation(libs.hikage.widget.material)

    //KotlinX
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    //OkHttp3
    implementation(libs.okhttp3)
    implementation(libs.net)

    //LibSU
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.io)

    //MarkDown
    implementation(libs.markwon.core)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.ext.tables)

    //Tools
    implementation(libs.deviceCompat)
    implementation(libs.xxpermissions)
    implementation(libs.spiderman)
    implementation(libs.fastscroll)
    implementation(libs.android.image.cropper)
}

fun getVersionCode(): Int {
    val propsFile = file("version.properties")
    if (propsFile.canRead()) {
        val properties = Properties()
        properties.load(FileInputStream(propsFile))
        var vCode = properties["versionCode"].toString().toInt()
        properties["versionCode"] = (++vCode).toString()
        properties.store(propsFile.writer(), null)
        println("versionCode -> $vCode")
        return vCode
    } else throw GradleException("Can't read version.properties!")
}