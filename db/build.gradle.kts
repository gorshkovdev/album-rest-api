import org.springframework.boot.gradle.tasks.bundling.BootJar

description = "db"

dependencies {
    implementation(project(":service"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    /* postgresql */
    implementation("org.postgresql:postgresql")

    /* flyway core */
    implementation("org.flywaydb:flyway-core")
}

tasks {
    named<BootJar>("bootJar") {
        enabled = false
    }

    named<Jar>("jar") {
        enabled = true
    }
}
