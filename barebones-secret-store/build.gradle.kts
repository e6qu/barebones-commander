repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-file"))

    // JNA for the macOS Security.framework + Linux libsecret bindings.
    implementation(libs.jna)

    testImplementation(libs.testng)
}
