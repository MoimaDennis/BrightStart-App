// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
configurations.configureEach {
    resolutionStrategy {
        force("androidx.core:core:1.15.0", "androidx.core:core-ktx:1.15.0")
    }
}