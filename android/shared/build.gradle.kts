plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    js(IR) { browser() }
    sourceSets {
        commonMain.dependencies { }
        jsMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

android { namespace = "com.learncraft.spacephysics.shared"; compileSdk = 35; defaultConfig { minSdk = 26 } }
