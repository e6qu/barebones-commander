repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-file"))

    implementation(libs.commons.compress)

    testImplementation(libs.junit.jupiter)
}
