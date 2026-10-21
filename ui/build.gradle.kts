plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.mheenotes.ui"
        compileSdk = 37
        minSdk = 29
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            implementation(libs.lucide)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.graphics.core)
            // PDF text for search on every Android version. BouncyCastle is only needed for
            // certificate-encrypted PDFs; leaving it out saves ~8 MB, and those files are skipped.
            implementation(libs.pdfbox.android.get().toString()) {
                exclude(group = "org.bouncycastle")
            }
        }
    }
}

