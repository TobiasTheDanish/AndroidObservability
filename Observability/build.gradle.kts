import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dk.tobiasthedanish.observability"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testOptions.targetSdk = 35
        version = "1.0.0-alpha.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.all {
            it.testLogging {
                events (
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT
                )
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true

                debug {
                    events (
                        TestLogEvent.STARTED,
                        TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_ERROR,
                        TestLogEvent.STANDARD_OUT
                    )
                    exceptionFormat = TestExceptionFormat.FULL
                }
                info.events = debug.events
                info.exceptionFormat = debug.exceptionFormat
            }
            val failedTests = mutableListOf<Pair<String, String>>()
            it.afterTest(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (result.resultType == TestResult.ResultType.FAILURE) {
                    val parentDisplay = if(desc.parent != null) "${desc.parent?.displayName}." else ""
                    failedTests.add("${parentDisplay}${desc.displayName}" to (result.exception?.toString() ?: "Unknown reason"))
                }
            }))
            it.afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (desc.parent == null) {
                    val output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    val (startItem, endItem) = ("|  " to "  |")
                    val repeatLength = startItem.length + output.length + endItem.length
                    println("\n${"-".repeat(repeatLength)}\n${startItem}${output}${endItem}\n${"-".repeat(repeatLength)}")
                    if (failedTests.size > 0) {
                        val maxNameLen = failedTests.maxOf { it.first.length }

                        println("Failed tests:")
                        failedTests.forEach { (name, msg) ->
                            println("   $name:${" ".repeat((maxNameLen - name.length) + 1)}$msg")
                        }
                    }
                }
            }))
        }
    }
}

dependencies {
    compileOnly(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.material3.android)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.junit.ktx)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.squareup.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.runtime.android)
    androidTestImplementation(libs.androidx.compose.ui)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.squareup.okhttp.mockwebserver)
}