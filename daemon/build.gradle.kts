dependencies {
    implementation(project(":protocol"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.ktor:ktor-server-core-jvm:3.5.0")
    implementation("io.ktor:ktor-server-cio-jvm:3.5.0")
    testImplementation("com.squareup.okhttp3:okhttp:5.4.0")
}
