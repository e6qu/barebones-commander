repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-io"))
    api(project(":barebones-commons-runtime"))
    api(project(":barebones-commons-util"))

    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)

    implementation(libs.icu4j)

    testImplementation(libs.junit.jupiter)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":barebones-format-zip"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
