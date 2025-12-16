group = "cz.grimir.wifimanager.admin.application"


dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:events"))
    implementation(project(":admin:core"))

    implementation("org.springframework:spring-context")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
