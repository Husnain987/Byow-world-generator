# BYOW — Build Your Own World

A procedurally generated dungeon crawler written in Java, built as the final project for
CS 61BL (Data Structures & Programming Methodology, UC Berkeley, Summer 2024). Each
playthrough generates a unique dungeon from a numeric seed — the same seed always produces
the same world, enabling reproducible exploration and save/load.

---

## Gameplay

Navigate your avatar (`@`) through a randomly generated dungeon and collect all 6 coins (`$`)
scattered throughout the rooms and hallways.

### Controls

**In-game**

| Key | Action |
|---|---|
| `W` / `A` / `S` / `D` | Move up / left / down / right |
| `V` | Toggle field-of-view (fog-of-war) |
| `:` then `Q` | Save and quit |

**Main menu**

| Key | Action |
|---|---|
| `N` | New game (enter a seed) |
| `L` | Load saved game |
| `Q` | Quit |

---

## Features

- **Procedural dungeon generation** — 6–12 rectangular rooms placed without overlap, connected
  by L-shaped hallways. Seed-driven, so worlds are fully reproducible.
- **Coin collection** — 6 coins placed on random floor tiles. Collect them all to win.
- **Field of view** — BFS-based fog-of-war (radius 7) reveals only connected floor space around
  the avatar plus adjacent walls.
- **Save / Load** — Game state (seed, dimensions, avatar position) written to `save.txt` so you
  can resume exactly where you left off.
- **HUD** — Displays tile description under the mouse cursor, FOV status, and coins remaining.

---

## How It Works

### World generation

1. **Room placement** — Random rooms (4–8 tiles wide/tall) are attempted up to 1,000 times;
   only non-overlapping ones are kept.
2. **Hallway routing** — Consecutive rooms are connected with L-shaped corridors; bend direction
   (horizontal-first vs. vertical-first) is randomized.
3. **Avatar placement** — Placed at the first floor tile found via bottom-left scan.
4. **Coin placement** — 6 coins scattered on random floor tiles, skipping the avatar's start.

### Field of view

The FOV mask is computed with a BFS walking through floor and avatar tiles within Manhattan
distance 7 of the avatar. Wall tiles adjacent to any visible floor tile are also revealed so
dungeon boundaries stay visible.

### Save format

`save.txt` stores three lines:
