plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.kotlin.jvm)
    java
    idea
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.itextpdf)
    implementation(libs.tess4j)
    implementation(libs.pdfbox)
    implementation(libs.levigo.jbig2)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.commons.math3)
    implementation(libs.commons.collections4)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.reflect)
}

tasks.test {
    environment("TESSDATA_PREFIX", "tessdata")
}
