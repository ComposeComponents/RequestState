import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

val desc = "A helper library for representing the state of a request."

kotlin {
    val currentOs = OperatingSystem.current()
    val strictBuild: Boolean by project.extra {
        (project.findProperty("build.strictPlatform") as? String)?.toBooleanStrictOrNull() ?: false
    }

    if (!strictBuild || currentOs.isLinux) {
        androidTarget {
            publishLibraryVariants("release")
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_11)
                    }
                }
            }
        }
        jvm()

        js(IR) {
            browser()
            useCommonJs()
            generateTypeScriptDefinitions()
        }
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs()
    }
    if (!strictBuild || currentOs.isMacOsX) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
    }

    cocoapods {
        summary = desc
        homepage = findProperty("pom.url") as String
        version = findProperty("version") as String
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "requeststate"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.livedata.ktx)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.emily.errorwidget)
            implementation(libs.emily.units)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.coroutines)
        }
    }
}

description = desc
group = "cl.emilym.compose"

android {
    namespace = "cl.emilym.compose.requeststate"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    sourceSets.all {
//        java.srcDirs(file("src/androidMain/kotlin"))
//        res.srcDirs(file("src/androidMain/res"))
//        resources.srcDirs(file("src/androidMain/resources"))
//        manifest.srcFile(file("src/androidMain/AndroidManifest.xml"))
//    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    coordinates("cl.emilym.compose", "requeststate", findProperty("version") as String)

    pom {
        name.set("RequestState")
        description.set(desc)
        url.set(findProperty("pom.url") as String)
        licenses {
            license {
                name.set(findProperty("pom.license.name") as String)
                url.set(findProperty("pom.license.url") as String)
            }
        }
        developers {
            developer {
                name.set(findProperty("pom.developer.name") as String)
                email.set(findProperty("pom.developer.email") as String)
            }
        }
        scm {
            connection.set(findProperty("pom.scm.connection") as String)
            developerConnection.set(findProperty("pom.scm.developerConnection") as String)
            url.set(findProperty("pom.scm.url") as String)
        }
    }

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )

    signAllPublications()
}