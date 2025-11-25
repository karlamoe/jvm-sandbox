plugins {
    java
    `java-library`
}

dependencies {
    api(libs.asm.analysis)

    testImplementation(libs.asm.util)
    testImplementation(libs.asm.commons)
}
