repositories.mavenCentral()

dependencies {
    api(project(":barebones-core"))
    api(project(":barebones-commons-file"))
    api(project(":barebones-viewer-api"))
    api(project(":barebones-os-api"))
    api(project(":barebones-translator"))
    api(project(":barebones-encoding"))
    api(project(":barebones-preferences"))
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.rsyntaxtextarea)

    testImplementation(libs.junit.jupiter)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
