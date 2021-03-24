

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt") apply true

}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    implementation("io.arrow-kt:arrow-core:0.11.0")
    implementation("io.arrow-kt:arrow-syntax:0.11.0")
}