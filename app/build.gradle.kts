plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "ca.lavagemobile.ops"
    compileSdk = 35

    defaultConfig {
        applicationId = "ca.lavagemobile.ops"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "17.0-test"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
