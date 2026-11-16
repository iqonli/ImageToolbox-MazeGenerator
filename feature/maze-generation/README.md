# Maze Generation

A native Image Toolbox generator with seven grids and sixteen algorithms. Controls use the same
`ResizeImageField`, parameter container, `TitleItem`, `DataSelector`, `SeedSelector`,
`PreferenceRowSwitch`, color selector and format/quality controls as the existing generators.
Algorithm descriptions and the supported-algorithm count live inside the algorithm selector.
Endpoint and appearance controls continue in the same parameter panel without extra headings.

SVG is the first choice in the image-format selector. In SVG mode, the exported SVG document is
parsed by AndroidSVG and retained as a vector `Picture` through `MazeSvgPainter`; neither the
preview nor the host zoom sheet enlarges a bitmap. Raster and SVG exports share `MazeScene`.
The AndroidSVG dependency follows the root project's existing AndroidSVG substitution.

The background color's alpha controls transparency. Previous settings with the separate
transparent flag are migrated to a transparent background color. Seeds use −10000…10000;
the shared seed selector's optional save-time randomization is disabled only for this tool.
Output dimensions follow Noise Generation's per-side 8192 bound and allow temporarily empty
fields while editing. Save is disabled until both dimensions are positive.

Animation, named presets and recipe import/export are not part of the screen. Last parameters
are still restored locally. Production generation does not retain replay deltas; the graph
invariant tests can explicitly request them. Reverse Delete retains its 1600-cell algorithm
limit. Braiding and shortest-path solving remain available with all compatible grids.

## Fork builds

The FOSS workflow keeps Gradle's cache and uploads arm64-v8a, armeabi-v7a, x86_64 and universal
APKs separately using `actions/upload-artifact@v7` with `archive: false`.

Release application ID: `ru.tech.imageresizershrinker.iqonli`.
The FileProvider authorities also have `.iqonli`, so the fork can coexist with upstream.
To restore upstream identifiers, remove `.iqonli` from `applicationId` and the two
`file_provider` resource values in `app/build.gradle.kts`. The workflow prints this reminder
in the build summary.

构建包名为 `ru.tech.imageresizershrinker.iqonli`，可与原版共存。以后改回原版时，在
`app/build.gradle.kts` 中移除 `applicationId` 和两个 `file_provider` 值中的 `.iqonli`。
构建摘要中也保留了这条说明。四种已签名 APK 分别上传，不再套一层 ZIP。

## Validation

Builds run in the repository's FOSS Actions workflow. Unit and Android rendering tests remain
available under this module. Suggested manual checks: rotate the screen, clear and retype each
dimension, undo/redo formats and parameters, choose SVG and zoom deeply, set background alpha
to zero, save/share/edit each format, and install the fork alongside the original app.

Maze algorithms were implemented for this project without copying another maze app's source.
References: Jamis Buck's Maze Generation series
(https://weblog.jamisbuck.org/2011/2/7/maze-generation-algorithm-recap), Aldous's random-walk
spanning-tree construction (1990), Wilson's loop-erased random walk (STOC 1996), and Origin Shift
(https://github.com/CaptainLuma/New-Maze-Generating-Algorithm).
Growing Binary Tree is explicitly the generalized preceding-BFS-layer parent variant; Houston
makes no uniform-spanning-tree sampling claim.
