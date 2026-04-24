import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
}

group = providers.gradleProperty("GROUP").orNull ?: "com.mahmoud.kpdf"
version = providers.gradleProperty("VERSION_NAME").orNull ?: "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    plugins.withId("maven-publish") {
        val javadocJar = tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
        }

        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)

                pom {
                    name.set(
                        providers.gradleProperty("POM_NAME")
                            .orElse(project.name)
                            .map { rootName ->
                                when (project.path) {
                                    ":kpdf-core" -> "$rootName Core"
                                    ":kpdf-compose" -> "$rootName Compose"
                                    else -> rootName
                                }
                            }
                    )
                    description.set(
                        providers.gradleProperty("POM_DESCRIPTION")
                            .orElse(project.description ?: project.name)
                    )
                    url.set(
                        providers.gradleProperty("POM_URL")
                            .orElse("https://github.com/your-org/KPDF")
                    )

                    licenses {
                        license {
                            name.set(
                                providers.gradleProperty("POM_LICENSE_NAME")
                                    .orElse("Set POM_LICENSE_NAME before publishing")
                            )
                            url.set(
                                providers.gradleProperty("POM_LICENSE_URL")
                                    .orElse("https://example.com/license")
                            )
                            distribution.set(
                                providers.gradleProperty("POM_LICENSE_DIST")
                                    .orElse("repo")
                            )
                        }
                    }

                    developers {
                        developer {
                            id.set(
                                providers.gradleProperty("POM_DEVELOPER_ID")
                                    .orElse("maintainer")
                            )
                            name.set(
                                providers.gradleProperty("POM_DEVELOPER_NAME")
                                    .orElse("Set POM_DEVELOPER_NAME before publishing")
                            )
                            url.set(
                                providers.gradleProperty("POM_DEVELOPER_URL")
                                    .orElse("https://github.com/your-org")
                            )
                        }
                    }

                    scm {
                        connection.set(
                            providers.gradleProperty("POM_SCM_CONNECTION")
                                .orElse("scm:git:git://github.com/your-org/KPDF.git")
                        )
                        developerConnection.set(
                            providers.gradleProperty("POM_SCM_DEV_CONNECTION")
                                .orElse("scm:git:ssh://git@github.com/your-org/KPDF.git")
                        )
                        url.set(
                            providers.gradleProperty("POM_SCM_URL")
                                .orElse("https://github.com/your-org/KPDF")
                        )
                    }
                }
            }

            val releaseRepositoryUrl = providers.gradleProperty("mavenReleasesUrl")
                .orElse("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepositoryUrl = providers.gradleProperty("mavenSnapshotsUrl")
                .orElse("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            val repositoryUsername = providers.gradleProperty("mavenCentralUsername")
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
            val repositoryPassword = providers.gradleProperty("mavenCentralPassword")
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))

            if (repositoryUsername.isPresent && repositoryPassword.isPresent) {
                repositories.maven {
                    name = "mavenCentral"
                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT")) {
                            snapshotRepositoryUrl.get()
                        } else {
                            releaseRepositoryUrl.get()
                        }
                    )
                    credentials {
                        username = repositoryUsername.get()
                        password = repositoryPassword.get()
                    }
                }
            }
        }
    }

    plugins.withId("signing") {
        val signingKey = providers.gradleProperty("signingInMemoryKey")
            .orElse(providers.environmentVariable("SIGNING_KEY"))
        val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword")
            .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

        extensions.configure<SigningExtension> {
            val shouldSign = signingKey.isPresent &&
                signingPassword.isPresent &&
                !version.toString().endsWith("SNAPSHOT")

            setRequired { shouldSign }

            if (shouldSign) {
                useInMemoryPgpKeys(
                    signingKey.get(),
                    signingPassword.get(),
                )
                sign(
                    extensions.getByType(PublishingExtension::class.java).publications
                )
            }
        }
    }
}
