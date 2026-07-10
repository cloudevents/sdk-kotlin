plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.testBalloon.gradlePlugin)
}
