plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    js(IR) { browser() }
    sourceSets {
        commonMain.dependencies { }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

android { namespace = "com.learncraft.spacephysics.shared"; compileSdk = 35; defaultConfig { minSdk = 26 } }
