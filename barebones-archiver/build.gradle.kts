repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-file"))
    api(project(":barebones-format-zip"))

    implementation(libs.commons.compress)
    implementation(libs.xz)

    testImplementation(libs.junit.jupiter)
}
