# Maze Generator

An offline generator integrated with Image Toolbox's generator screen, Decompose navigation,
undo/redo history, format/quality selection, save-directory picker, sharing, clipboard and
"edit in another tool" sheet. It adds no native library or third-party maze implementation.

## Grids and algorithms

The graph and its shared-wall geometry are built together. Every interior wall has one edge
index used by generation, replay, solving, preview and export. Polar walls retain true arcs.

| Grid | Algorithms |
| --- | --- |
| Square | All 16 |
| Hexagonal, triangular, polar, octagon/square, rhombic, masked square | All except Binary Tree, Sidewinder, Eller and rectangular Recursive Division |

Algorithms: Recursive Backtracker, randomized Prim, randomized Kruskal, Aldous–Broder,
Wilson, Hunt-and-Kill, Growing Tree, Eller, Sidewinder, Binary Tree, Recursive Division,
Blobby Division, Houston, Reverse Delete, generalized Growing Binary Tree, Origin Shift.

The generalized Growing Binary Tree implementation chooses a parent in the previous BFS
layer. It is described as that variant in the UI; it is not presented as an identical copy
of another project's algorithm. Aldous–Broder and Wilson implement uniform spanning-tree
sampling over the graph. Houston deliberately makes no uniformity claim. Random choices
use a versioned xorshift32 stream with rejection sampling rather than modulo reduction.

## Controls

| Control | Range / behavior |
| --- | --- |
| Rows, columns | 2–100; polar uses rows as ring count |
| Seed | Integer; built-in selector covers −1,000,000…1,000,000, plus copy and lock |
| Dead-end removal | 0–100%; zero preserves the perfect maze |
| Growing Tree | Newest, random, oldest, middle relative weights, each 0–100 |
| Binary Tree | Four diagonal orientations; horizontal bias 0–100% |
| Sidewinder | Horizontal extension probability 0–100% |
| Eller | Horizontal joins and additional downward passages, each 0–100% |
| Houston | Switch after visiting 0–100% of cells |
| Origin Shift | 1–100 shifts per cell |
| Recursive Division | Split-direction bias and leaf area 1–20 cells; leaves remain trees |
| Masks | Ellipse, diamond, heart, cross, image; image luminance 0–255 and invert |
| Endpoints | Distant boundary pair, geometrically opposite boundary cells, random boundary cells, center/boundary, manual |
| Manual endpoints | Accessible numeric indices or selecting an endpoint and tapping the preview |
| Walls | Color including alpha, cap, width 0.01–0.5 cell units |
| Solution | Optional shortest route (off by default), drawn above walls; color including alpha and width 0.02–0.8 cell units |
| Markers | Separate start/end colors, radius 0.05–0.6 cell units, visibility |
| Background | Color including alpha, transparency |
| Margin | 0.1–5 cell units |
| Heatmap | BFS distance from the start |
| Animation | Generation edge changes or solution, pause/play, seek, single step, restart, finish; 20–10,000 steps/s |
| Raster | Existing image format and quality controls; up to 8192 per side and 32 million total pixels |
| SVG | Same scene and contain transform; recipe JSON embedded as XML metadata |
| Recipes | Versioned standalone JSON import/export, last recipe restoration, up to 50 named local presets, built-in examples |

Disconnected image masks retain their largest four-connected component. The recipe stores
sampled mask cells, not a content URI, so it can be restored on another device. Resizing a
mask resamples that grid; the original image is only needed to reapply its luminance threshold.
Empty masks and unsupported recipe versions are rejected.

Reverse Delete checks reachability for each candidate edge and is capped at 1600 cells.
Other random-walk algorithms remain cancellable rather than silently switching algorithms.
Grid switching with an incompatible selected algorithm asks the user to select a compatible
algorithm first. No algorithm is substituted behind the user's back.

Distant boundary endpoints use two BFS sweeps. On a tree this returns a boundary diameter;
on braided graphs it is a heuristic, not a claimed exact graph diameter. Opposite endpoints
are the geometrically most distant boundary-cell pair. The chosen route is always computed
with BFS. Interior endpoints do not punch unrelated holes in the boundary.

Generation, solving, rendering and replay run on the host dispatcher. Parameter changes cancel
obsolete preview work; publication checks a revision token. Style and endpoint changes reuse
the cached graph. Playback stores signed edge deltas in primitive arrays instead of snapshots.
Export captures its recipe, dimensions, format and quality before starting and always uses the
completed maze, regardless of playback position. Presets and last recipes are feature-local.

## Validation status

Source was reviewed; resource references and XML structure were checked during preparation.
No Gradle build, unit test, instrumentation test or emulator run was performed, at the user's
request. The included tests are source for the recipient to execute, not recorded test passes.

Suggested commands from the repository root:

```sh
./gradlew :feature:maze-generation:testFossDebugUnitTest
./gradlew :app:assembleFossDebug
./gradlew :feature:maze-generation:connectedFossDebugAndroidTest
```

Tests cover the supported algorithm/grid matrix, tree invariants, deterministic passages and
replay deltas, parameter extremes, braid connectivity, single-cell masks, topology geometry,
mask islands, invalid recipes, cancellation, disconnected-solver rejection, optional route
visibility, raster route layering, SVG route layering, SVG XML/locale/metadata and Android raster output.

Manual checks: discover the tool through search; favorite/reorder it; rotate the device;
undo/redo dimensions, style and recipe changes; change parameters during a slow random walk;
try all endpoint modes; scrub each playback mode; save/share/copy/edit both raster and SVG;
import a recipe with an image mask; restart the app and restore the last recipe; try large fonts,
light/dark themes and a transparent image format.

## Algorithm references and provenance

All maze code in this module was written for this change. No source was copied from MazeR,
Maze Forge or Image Online Tools. Existing Image Toolbox screen scaffolding is adapted under
the repository's Apache-2.0 license and retains its headers.

Algorithm descriptions and terminology:

- Jamis Buck, *Maze Generation* series: https://weblog.jamisbuck.org/2011/2/7/maze-generation-algorithm-recap
- Growing Tree: https://weblog.jamisbuck.org/2011/1/27/maze-generation-growing-tree-algorithm
- Wilson: https://weblog.jamisbuck.org/2011/1/20/maze-generation-wilson-s-algorithm
- Blobby recursive subdivision: https://weblog.jamisbuck.org/2015/1/15/better-recursive-division-algorithm.html
- Aldous, *The random walk construction of uniform spanning trees and uniform labelled trees*, SIAM J. Discrete Math. 3(4), 1990.
- Wilson, *Generating random spanning trees more quickly than the cover time*, STOC, 1996.
- Origin Shift explanation: https://github.com/CaptainLuma/New-Maze-Generating-Algorithm

Product behavior references (no code incorporated):
https://www.imageonlinetools.com/maze-generator and https://github.com/jmisabella/mazer.

PDF printing and layered weave mazes are not exposed by this tool. SVG provides vector output;
weave crossings require an additional layered topology rather than a rendering checkbox.
