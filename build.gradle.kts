plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.mavenPublish).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.jetbrainsKotlinAndroid).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
}