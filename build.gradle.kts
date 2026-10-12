// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("androidx.room") version "2.7.0-alpha13" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}