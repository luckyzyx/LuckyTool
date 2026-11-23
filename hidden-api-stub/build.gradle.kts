plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = gropify.project.stub.groupName
    compileSdk {
        version = release(gropify.project.android.compileSdk) {
            minorApiLevel = gropify.project.android.compileSdkMinor
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.annotation)
}
