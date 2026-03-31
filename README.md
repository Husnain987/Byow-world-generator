# BYOW — Build Your Own World

A procedurally generated dungeon crawler written in Java, built as the final project for **CS61BL** (Data Structures, Summer 2024). Each playthrough generates a unique dungeon from a numeric seed — the same seed always produces the same world, enabling reproducible exploration and save/load.

## Gameplay

Navigate your avatar (`@`) through a randomly generated dungeon and collect all 6 coins (`$`) scattered throughout the rooms and hallways.

### Controls

| Key | Action |
|-----|--------|
| `W` / `A` / `S` / `D` | Move up / left / down / right |
| `V` | Toggle field-of-view (fog-of-war) |
| `:` then `Q` | Save and quit |

From the **main menu**:

| Key | Action |
|-----|--------|
| `N` | New game (enter a seed) |
| `L` | Load saved game |
| `Q` | Quit |

## Features

- **Procedural dungeon generation** — 6–12 rectangular rooms placed without overlap, connected by L-shaped hallways. The algorithm is seed-driven, so worlds are fully reproducible.
- **Coin collection** — 6 coins are randomly placed on floor tiles. Collect them all to win.
- **Field of View** — Toggle a BFS-based fog-of-war effect (radius 7) that reveals only the connected floor space around the avatar, plus any adjacent walls.
- **Save / Load** — Game state (seed, world dimensions, avatar position) is written to `save.txt` so you can resume exactly where you left off.
- **HUD** — Displays the tile description under the mouse cursor, current FOV status, and coins remaining.

## How It Works

### World Generation

1. **Room placement** — Random rooms (4–8 tiles wide/tall) are attempted up to 1000 times; only non-overlapping ones are kept.
2. **Hallway routing** — Consecutive rooms are connected with L-shaped corridors. The bend direction (horizontal-first vs. vertical-first) is randomized.
3. **Avatar placement** — The avatar is placed at the first floor tile found (bottom-left scan).
4. **Coin placement** — 6 coins are scattered on random floor tiles, skipping the avatar's starting position.

### Field of View

The FOV mask is computed with a BFS that walks through floor and avatar tiles within Manhattan distance 7 of the avatar. After the BFS, any wall tile adjacent to a visible floor tile is also revealed so dungeon boundaries remain visible.

### Save Format

`save.txt` stores three lines:

```
<seed>
<width> <height>
<avatarX> <avatarY>
```

## Project Structure

```
proj3/
├── src/
│   ├── core/
│   │   ├── Main.java            # Entry point, UI, game loop, save/load
│   │   ├── WorldGenerator.java  # Dungeon generation and avatar movement
│   │   └── Position.java        # Simple (x, y) coordinate pair
│   ├── tileengine/
│   │   ├── TERenderer.java      # StdDraw-based tile renderer (library)
│   │   ├── TETile.java          # Tile data model (library)
│   │   └── Tileset.java         # Predefined tile constants
│   └── utils/
│       └── RandomUtils.java     # Statistical RNG utilities (library)
└── save.txt                     # Auto-generated on save
```

## Dependencies

- [Princeton `algs4.jar`](https://algs4.cs.princeton.edu/code/) — provides `StdDraw` for rendering and input, and `StdRandom`-style utilities.

## Building and Running

This project was developed inside the IntelliJ-based CS61BL skeleton. To run it:

1. Open the `proj3` directory as an IntelliJ project.
2. Ensure `algs4.jar` is on the classpath (it is included in the skeleton).
3. Run `core.Main`.

Or compile and run from the command line (adjust the classpath as needed):

```bash
javac -cp lib/algs4.jar -d out src/core/*.java src/tileengine/*.java src/utils/*.java
java  -cp out:lib/algs4.jar core.Main
```