repositories.mavenCentral()

dependencies {
    api(project(":barebones-commons-file"))
    api(project(":barebones-protocol-api"))
    api(project(":barebones-translator"))

    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.s3.transfer.manager)
    implementation(libs.aws.sdk.crt.client)

    testImplementation(libs.testng)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.localstack)
}
