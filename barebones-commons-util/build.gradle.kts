repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-runtime"))

    testImplementation(libs.junit.jupiter)
}
