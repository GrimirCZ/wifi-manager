group = "cz.grimir.wifimanager.admin.events.local"

dependencies {
    implementation(project(":admin:application"))
    implementation(project(":shared:events"))
    implementation("org.springframework:spring-context")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
