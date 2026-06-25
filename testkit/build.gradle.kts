dependencies {
    implementation(project(":protocol"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.ktor:ktor-client-core-jvm:3.5.0")
    testImplementation("io.ktor:ktor-client-mock-jvm:3.5.0")
}
