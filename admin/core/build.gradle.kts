group = "cz.grimir.wifimanager.admin.core"

dependencies {
    implementation(project(":shared"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}