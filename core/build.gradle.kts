plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.mheenotes.core"
        compileSdk = 37
        minSdk = 29
    }
    // JVM target runs the engine tests on the host and is the future desktop target.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(libs.sqlite)
            api(libs.sqlite.bundled)
            api(libs.kotlinx.io)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
