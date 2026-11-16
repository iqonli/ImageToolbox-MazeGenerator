# ezMaze

An independent maze generator fork of [Image Toolbox](https://github.com/T8RIN/ImageToolbox), created by IQ Online Studio. Apache License 2.0.

The app opens directly in the maze editor. It retains all 16 generation algorithms and seven topologies, deterministic seeds, algorithm parameters, braiding, masks including imported images, endpoint selection, solutions, distance heatmaps, statistics, appearance controls, undo/redo and the last-used recipe.

SVG is the first image format and uses vector data for preview and zoom. Raster export uses the original Image Toolbox encoders. Tap Save to use the current destination (Documents/ezMaze by default), or long-press Save to choose a destination. The toolbar preferences control the original filename template, prefix, suffix and timestamp settings. Conflicting names receive a numbered suffix. Written bytes are read back before success is reported.

Languages: English, Simplified Chinese and Traditional Chinese. Language and filename preferences are available through the toolbar settings button.

## Build

JDK 21 and an Android SDK are required. The existing FOSS workflow preserves Gradle caching and builds signed, separate ABI APKs.

```sh
./gradlew :core:domain:testFossDebugUnitTest :core:data:testFossDebugUnitTest :feature:maze-generation:testFossDebugUnitTest :app:assembleFossRelease
```

Release application ID: `com.iqonli.ezmaze`. Debug builds append `.debug`. CI signing alias: `iqonli-1`; keystore and password remain GitHub secrets. No signing material is included in the source.

## Source structure

`app` contains the direct Maze host, native theme providers and minimal preferences. `feature/maze-generation` retains the algorithms, recipe/version model, last-recipe persistence, renderers and editor. The shared `core` modules retain image loading/export, settings persistence, Compose components, DI and foreground-save lifecycle. The remaining `lib` modules provide the existing color picker, theme, gestures, sheets, cropper internals and zoom components.

The original tool root, other feature modules, product settings UI, promotions, analytics, update checks, scanner, neural/OpenCV/PDF initialization and unrelated assets have been removed. Existing settings data types remain where shared components or serialization require them; they do not expose other tools.

The FOSS workflow also starts the signed release on an Android 35 emulator, checks the visible ezMaze window, and runs the SVG storage and maze bitmap instrumentation tests. SVG tests cover configured templates, extension handling, repeated same-name saves, unique URIs, MIME type and read-back verification. Test reports and a startup screenshot are uploaded separately from the APKs.

## Attribution

Based on Image Toolbox by T8RIN (Malik Mukhametzyanov) and contributors. See [LICENSE](LICENSE) and [NOTICE](NOTICE). The ezMz launcher lettering follows the ezCD icon layout; font attribution is included in NOTICE-FONT.txt. Third-party dependencies retain their own licenses.
