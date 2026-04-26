import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

val publishedModules = setOf(
    ":kpdf-core",
    ":kpdf-compose",
)

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
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

group = providers.gradleProperty("GROUP").orNull ?: "io.github.mahmoud947"
version = providers.gradleProperty("VERSION_NAME").orNull ?: "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    if (path in publishedModules) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            apply(plugin = "com.vanniktech.maven.publish")
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()

            configureBasedOnAppliedPlugins(
                javadocJar = JavadocJar.Empty(),
                sourcesJar = SourcesJar.Sources(),
            )

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
    }
}
