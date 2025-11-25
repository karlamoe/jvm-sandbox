allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
        failOnNoDiscoveredTests = false
    }

    pluginManager.withPlugin("java") {
        repositories {
            mavenCentral()
        }

        dependencies {
            "implementation"("org.jetbrains:annotations:26.0.2-1")

            "testImplementation"(platform("org.junit:junit-bom:6.0.1"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testImplementation"("org.junit.platform:junit-platform-launcher")
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

    }
}

