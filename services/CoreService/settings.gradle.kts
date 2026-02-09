pluginManagement {
    plugins {
        val kotlinVersion = "2.3.0"
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        id("org.springframework.boot") version "3.5.10"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

rootProject.name = "CoreService"
