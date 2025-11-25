plugins {
    `java-library`
}

dependencies {
    api(platform("org.junit:junit-bom:6.0.1"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.junit.platform:junit-platform-launcher")


    api(libs.asm.util)
    api(libs.asm.commons)

}