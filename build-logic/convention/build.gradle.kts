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
    implementation(libs.mavenPublish.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
}
