plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android { namespace = "com.payanag2.pingtool"; compileSdk = 35
    defaultConfig { applicationId = "com.payanag2.pingtool"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "1.1" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
