enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        // 详情请前往：https://github.com/HighCapable/maven-repository
        // 中国大陆用户请将下方的 "raw.githubusercontent.com" 修改为 "raw.gitmirror.com"
        maven("https://raw.githubusercontent.com/HighCapable/maven-repository/main/repository/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            content { includeGroup("com.highcapable.yukihookapi") }
            content { includeGroup("com.highcapable.kavaref") }
            content { includeGroup("com.highcapable.hikage") }
        }
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        // 详情请前往：https://github.com/HighCapable/maven-repository
        // 中国大陆用户请将下方的 "raw.githubusercontent.com" 修改为 "raw.gitmirror.com"
        maven("https://raw.githubusercontent.com/HighCapable/maven-repository/main/repository/releases")
    }
}

rootProject.name = "LuckyTool"
include(":app", ":colorpicker", ":hidden-api-stub")
