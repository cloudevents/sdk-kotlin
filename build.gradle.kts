// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.testBalloon) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.axionRelease)
}

// Git-tag-driven versioning. With no release tags yet, axion derives 0.1.0-SNAPSHOT
// (0.1.0 is axion's default initial version). The release workflow passes the increment
// strategy via -PversionIncrement (incrementPatch | incrementMinor | incrementMajor).
scmVersion {
    versionIncrementer((project.findProperty("versionIncrement") ?: "incrementMinor").toString())
}

group = "io.cloudevents"
version = scmVersion.version
