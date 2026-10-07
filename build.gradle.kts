import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile: File = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.lsplugin.lsparanoid) apply false
}

extra["compileSdkVersion"] = 37
extra["targetSdkVersion"] = 28
extra["minSdkVersion"] = 30

extra["jdkVersion"] = 21

extra["storeFile"] = keystoreProperties["storeFile"]
extra["storePassword"] = keystoreProperties["storePassword"]
extra["keyAlias"] = keystoreProperties["keyAlias"]
extra["keyPassword"] = keystoreProperties["keyPassword"]

tasks {
    register("clean", Delete::class) {
        delete(layout.buildDirectory)
        description = ""
    }
}