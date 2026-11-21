import extension.setupDependencyInjection
import extension.testCommonDependencies

/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
    id("io.element.android-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.element.android.features.scheduling.impl"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

setupDependencyInjection()

dependencies {
    implementation(projects.libraries.core)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.sessionStorage.api)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.network)
    implementation(platform(libs.network.retrofit.bom))
    implementation(libs.network.retrofit)
    implementation(libs.serialization.json)

    testCommonDependencies(libs)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.sessionStorage.test)
    testImplementation(platform(libs.network.okhttp.bom))
    testImplementation(libs.network.mockwebserver)
    testImplementation(libs.network.retrofit.converter.serialization)
}
