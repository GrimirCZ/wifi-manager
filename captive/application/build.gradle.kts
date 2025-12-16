group = "cz.grimir.wifimanager.captive.application"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:events"))
    implementation(project(":captive:core"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
