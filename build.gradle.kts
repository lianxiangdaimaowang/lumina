
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
}

buildscript {
    repositories {
        mavenLocal()  // 添加本地Maven仓库 
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}

subprojects {
    configurations.all {
        // 为所有子项目排除bouncycastle，避免冲突
        exclude(group = "org.bouncycastle")
    }
}

// 添加清理Gradle缓存的任务
tasks.register("cleanGradleCache", Delete::class) {
    group = "build"
    description = "清理Gradle缓存，解决依赖冲突问题"
    delete(
        fileTree("${rootProject.buildDir}") {
            include("**/*.lock")
            include("**/*.bin")
        },
        "${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1/org.bouncycastle"
    )
}
