group = "cz.grimir.wifimanager.captive.application"

dependencies {
    implementation(project(":captive:core"))

    implementation("org.springframework:spring-context")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}