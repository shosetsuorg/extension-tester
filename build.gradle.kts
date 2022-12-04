plugins {
	kotlin("jvm") version "1.7.21"
	id("com.github.gmazzo.buildconfig") version "3.1.0"
	application
}

group = "com.github.doomsdayrs.lib"
version = "1.0.0"

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
}

buildConfig {
	buildConfigField("String", "VERSION", "\"$version\"")
}

dependencies {
	testImplementation(kotlin("test"))

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

	implementation("com.github.shosetsuorg:kotlin-lib:1.0.0")
	implementation(kotlin("stdlib"))
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.jsoup:jsoup:1.15.3")
	implementation("com.squareup.okhttp3:okhttp:4.10.0")
	implementation("org.luaj:luaj-jse:3.0.1")

	implementation(kotlin("reflect"))
}

tasks.test {
	useJUnit()
}

application {
	mainClass.set("TestKt")
}

tasks.register<Jar>("assembleJar") {
	val programVersion = archiveVersion.get()
	archiveVersion.set("")


	duplicatesStrategy = DuplicatesStrategy.INCLUDE

	manifest {
		attributes(
			"Main-Class" to application.mainClass,
			"Implementation-Title" to "Gradle",
			"Implementation-Version" to programVersion
		)
	}

	from(sourceSets.main.get().output)
	dependsOn(configurations.runtimeClasspath)
	from(
		configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
	)
}

