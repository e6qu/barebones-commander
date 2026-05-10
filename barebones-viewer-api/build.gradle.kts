repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-file"))
    api(project(":barebones-commons-util"))

    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.testng)
}
