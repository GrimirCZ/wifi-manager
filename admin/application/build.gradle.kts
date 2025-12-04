group = "cz.grimir.wifimanager.admin.application"


dependencies {
    implementation(project(":shared"))
    implementation(project(":admin:core"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}