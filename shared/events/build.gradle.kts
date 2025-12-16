group = "cz.grimir.wifimanager.shared.events"

dependencies {
    implementation(project(":shared:core"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
