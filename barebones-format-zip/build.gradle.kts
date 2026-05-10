repositories.mavenCentral()

evaluationDependsOn(":barebones-commons-file")

val commonsFileTestOutput = project(":barebones-commons-file")
    .extensions.getByType(JavaPluginExtension::class.java)
    .sourceSets["test"].output

dependencies {
    api(project(":barebones-commons-file"))

    testImplementation(libs.junit.jupiter)
    testImplementation(project(":barebones-commons-file"))
    testImplementation(files(commonsFileTestOutput))
}
