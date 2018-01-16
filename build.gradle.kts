import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.wrapper.Wrapper

version = "0.1.0"
group = "com.github.samcarlberg"

buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.1")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.7.3")
    }
}

val PUBLISHED_CONFIGURATION_NAME = "published"
val publicationName = "publication-$name"

apply {
    plugin("com.jfrog.bintray")
}

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.16.20")
    implementation(group = "com.github.zafarkhaja", name = "java-semver", version = "0.9.0")
    fun junitJupiter(name: String, version: String = "5.0.0") =
            create(group = "org.junit.jupiter", name = name, version = version)
    testCompile(junitJupiter(name = "junit-jupiter-api"))
    testCompile(junitJupiter(name = "junit-jupiter-engine"))
    testCompile(junitJupiter(name = "junit-jupiter-params"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val sourceJar = task<Jar>("sourceJar") {
    description = "Creates a JAR that contains the source code."
    from(java.sourceSets["main"].allSource)
    classifier = "sources"
}

val javadocJar = task<Jar>("javadocJar") {
    dependsOn("javadoc")
    description = "Creates a JAR that contains the javadocs."
    from(java.docsDir)
    classifier = "javadoc"
}

bintray {
    user = properties["bintray.publish.user"].toString()
    key = properties["bintray.publish.key"].toString()
    setPublications(publicationName)
    with(pkg) {
        repo = "maven-artifacts"
        name = "update-checker"
        publish = true
        desc = "A library for checking for software updates from Maven (and other) repositories"
        setLicenses("MIT")
        vcsUrl = "https://github.com/samcarlberg/update-checker.git"
        githubRepo = "https://github.com/samcarlberg/update-checker"
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>(publicationName) {
                groupId = "com.github.samcarlberg"
                artifactId = "update-checker"
                version = project.version as String
                val jar: Jar by tasks
                artifact(jar)
                artifact(sourceJar)
                artifact(javadocJar)
            }
        }
    }
}

configurations.create(PUBLISHED_CONFIGURATION_NAME)

task<Wrapper>("wrapper") {
    description = "Configure the version of gradle to download and use"
    gradleVersion = "4.1"
    distributionType = Wrapper.DistributionType.ALL
}

/**
 * Retrieves or configures the [bintray][com.jfrog.bintray.gradle.BintrayExtension] project extension.
 */
fun Project.`bintray`(configure: com.jfrog.bintray.gradle.BintrayExtension.() -> Unit = {}) =
        extensions.getByName<com.jfrog.bintray.gradle.BintrayExtension>("bintray").apply { configure() }

/**
 * Retrieves or configures the [publishing][org.gradle.api.publish.PublishingExtension] project extension.
 */
fun Project.`publishing`(configure: org.gradle.api.publish.PublishingExtension.() -> Unit = {}) =
        extensions.getByName<org.gradle.api.publish.PublishingExtension>("publishing").apply { configure() }
