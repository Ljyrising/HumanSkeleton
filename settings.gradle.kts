pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 显式声明 dl.google.com：本机代理对 maven.google.com 返回 502，
        // 而 MediaPipe / CameraX / datatransport 都发布在该地址。
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        google()
        mavenCentral()
    }
}

rootProject.name = "HumanSkeleton"
include(":app")
 