plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    compile(group = "com.github.zafarkhaja", name = "java-semver", version = "0.9.0")
    compileOnly(group = "org.projectlombok", name = "lombok", version = "+")
    fun junitJupiter(name: String, version: String = "5.0.0") =
            create(group = "org.junit.jupiter", name = name, version = version)
    testCompile(junitJupiter(name = "junit-jupiter-api"))
    testCompile(junitJupiter(name = "junit-jupiter-engine"))
    testCompile(junitJupiter(name = "junit-jupiter-params"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
