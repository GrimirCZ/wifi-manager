import com.google.protobuf.gradle.id

group = "cz.grimir.wifimanager.captive.routeragent"

plugins {
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":shared:application"))
    implementation(project(":shared:events"))
    implementation(project(":captive:core"))
    implementation(project(":captive:application"))
    implementation(libs.springBootStarterWeb)
    implementation(libs.kotlinLogging)
    implementation(libs.bundles.grpc)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.bundles.testMockito)
    testImplementation(libs.bundles.jackson)
    testRuntimeOnly(libs.bundles.testRuntime)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                id("grpc")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
