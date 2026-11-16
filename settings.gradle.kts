/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        includeBuild("build-logic")
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io") { name = "JitPack" }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://androidx.dev/storage/compose-compiler/repository") {
            name = "Compose Compiler Snapshots"
            content { includeGroup("androidx.compose.compiler") }
        }
        mavenCentral()
        maven("https://jitpack.io") { name = "JitPack" }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Snapshots"
            mavenContent {
                snapshotsOnly()
            }
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ImageToolbox"

include(":app")
include(":core:crash")
include(":core:data")
include(":core:di")
include(":core:domain")
include(":core:resources")
include(":core:settings")
include(":core:ui")
include(":core:utils")
include(":feature:maze-generation")
include(":lib:colors")
include(":lib:cropper")
include(":lib:dynamic-theme")
include(":lib:gesture")
include(":lib:modalsheet")
include(":lib:zoomable")
