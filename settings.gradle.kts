pluginManagement {
    repositories {
        mavenLocal()  // 添加本地Maven仓库
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()  // 添加本地Maven仓库
        google()
        mavenCentral()
        
        // 添加JitPack仓库以支持一些第三方库
        maven { url = uri("https://jitpack.io") }
        
        // 添加JCenter仓库（用于部分旧依赖）
        jcenter() // 对于旧依赖，但可能被逐渐废弃
        
        // 添加flatDir仓库，用于本地JAR/AAR文件
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "Lumina"
include(":app")