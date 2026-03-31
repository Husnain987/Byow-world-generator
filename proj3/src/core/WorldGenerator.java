package core;

import tileengine.TETile;
import tileengine.Tileset;
import utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;

/**
 * Handles procedural dungeon generation and runtime game state (avatar movement,
 * coin collection).
 *
 * <p>Generation algorithm:
 * <ol>
 *   <li>Place 6–12 randomly sized, non-overlapping rectangular rooms.</li>
 *   <li>Connect consecutive rooms with L-shaped hallways (horizontal-first or
 *       vertical-first, chosen randomly).</li>
 *   <li>Place the avatar on the first floor tile found.</li>
 *   <li>Scatter 6 coins on random floor tiles.</li>
 * </ol>
 *
 * <p>The same {@code seed} always produces the same world, enabling save/load.
 */
public class WorldGenerator {

    private final int WIDTH;
    private final int HEIGHT;
    private final long seed;
    private final Random random;

    private final TETile[][] world;
    /** Centers of successfully placed rooms, used to route hallways. */
    private final List<Position> roomCenters = new ArrayList<>();
    private Position avatarPosition;

    /**
     * Unique tile ID used to identify coin tiles without a direct reference
     * to the COIN TETile object (TETile equality compares by identity).
     */
    private static final int COIN_ID = 89676;
    private final TETile COIN = new TETile('$', Color.YELLOW, Color.BLACK, "coin", COIN_ID);

    /** Number of coins still on the board; reaches 0 when the player wins. */
    private int coinsLeft = 0;

    /** @return the number of coins remaining to be collected. */
    public int getCoinsLeft() {
        return coinsLeft;
    }

    /**
     * Creates a WorldGenerator for a world of the given dimensions.
     * The tile array is allocated and filled with {@link Tileset#NOTHING}.
     *
     * @param width  number of columns in the world grid
     * @param height number of rows in the world grid
     * @param seed   RNG seed; identical seeds produce identical worlds
     */
    public WorldGenerator(int width, int height, long seed) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.seed = seed;
        this.random = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];

        fillWithNothing();
    }

    /** @return the avatar's current grid position, or {@code null} before generation. */
    public Position getAvatarPosition() {
        return avatarPosition;
    }

    /** @return the seed used to generate this world. */
    public long getSeed() {
        return seed;
    }

    /** Fills every tile in the world array with {@link Tileset#NOTHING}. */
    private void fillWithNothing() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    /**
     * Generates the complete dungeon and returns the tile array.
     * Rooms are placed first, then connected by hallways, then the avatar
     * and coins are placed.
     *
     * @return the fully populated 2-D tile array representing the world
     */
    public TETile[][] generateWorld() {
        int numRooms = RandomUtils.uniform(random, 6, 13);
        int roomsAdded = 0;
        int attempts = 0;

        // Keep trying to place rooms until we hit the target count or the attempt cap.
        while (roomsAdded < numRooms && attempts < 1000) {
            int roomWidth = RandomUtils.uniform(random, 4, 9);
            int roomHeight = RandomUtils.uniform(random, 4, 9);

            int x = RandomUtils.uniform(random, 1, WIDTH - roomWidth - 1);
            int y = RandomUtils.uniform(random, 1, HEIGHT - roomHeight - 1);

            if (!roomOverlaps(x, y, roomWidth, roomHeight)) {
                addRoom(x, y, roomWidth, roomHeight);
                roomCenters.add(new Position(x + roomWidth / 2, y + roomHeight / 2));
                roomsAdded++;
            }

            attempts++;
        }

        // Connect each room to the next with an L-shaped hallway.
        for (int i = 1; i < roomCenters.size(); i++) {
            Position p1 = roomCenters.get(i - 1);
            Position p2 = roomCenters.get(i);
            connectRooms(p1, p2);
        }

        // Place avatar on the first floor tile found (bottom-left scan order).
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (world[x][y] == Tileset.FLOOR) {
                    avatarPosition = new Position(x, y);
                    world[x][y] = Tileset.AVATAR;
                    break;
                }
            }
            if (avatarPosition != null) break;
        }

        placeCoins(6);

        return world;
    }

    /**
     * Attempts to move the avatar by {@code (dx, dy)}.
     * Movement is blocked by walls and empty space. Stepping on a coin
     * automatically collects it (decrements {@link #coinsLeft}).
     *
     * @param dx horizontal delta (-1 = left, +1 = right)
     * @param dy vertical delta   (-1 = down, +1 = up)
     * @return {@code true} if the move succeeded
     */
    public boolean tryMove(int dx, int dy) {
        if (avatarPosition == null) return false;

        int nx = avatarPosition.x + dx;
        int ny = avatarPosition.y + dy;

        if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT) return false;

        TETile target = world[nx][ny];

        // Only floor and coin tiles are walkable.
        if (target == Tileset.WALL || target == Tileset.NOTHING) return false;

        if (target.id() == COIN_ID) {
            coinsLeft = Math.max(0, coinsLeft - 1);
        }

        world[avatarPosition.x][avatarPosition.y] = Tileset.FLOOR;
        avatarPosition.x = nx;
        avatarPosition.y = ny;
        world[nx][ny] = Tileset.AVATAR;
        return true;
    }

    /**
     * Draws a rectangular room (floor interior + wall border) at the given
     * top-left corner. Walls are only written over {@link Tileset#NOTHING}
     * tiles so that overlapping hallways/rooms are not accidentally walled off.
     *
     * @param x          left column of the room's floor area
     * @param y          bottom row of the room's floor area
     * @param roomWidth  number of floor columns
     * @param roomHeight number of floor rows
     */
    private void addRoom(int x, int y, int roomWidth, int roomHeight) {
        for (int dx = -1; dx <= roomWidth; dx++) {
            for (int dy = -1; dy <= roomHeight; dy++) {
                int worldX = x + dx;
                int worldY = y + dy;

                if (worldX > 0 && worldX < WIDTH - 1 && worldY > 0 && worldY < HEIGHT - 1) {
                    if (dx >= 0 && dx < roomWidth && dy >= 0 && dy < roomHeight) {
                        world[worldX][worldY] = Tileset.FLOOR;
                    } else {
                        // Border tile — only place a wall if the space is currently empty.
                        if (world[worldX][worldY] == Tileset.NOTHING) {
                            world[worldX][worldY] = Tileset.WALL;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if the proposed room footprint (including its
     * 1-tile border) would overlap any already-placed tile.
     *
     * @param x          left column of the room's floor area
     * @param y          bottom row of the room's floor area
     * @param roomWidth  number of floor columns
     * @param roomHeight number of floor rows
     */
    private boolean roomOverlaps(int x, int y, int roomWidth, int roomHeight) {
        for (int dx = -1; dx <= roomWidth; dx++) {
            for (int dy = -1; dy <= roomHeight; dy++) {
                int worldX = x + dx;
                int worldY = y + dy;

                if (worldX > 0 && worldX < WIDTH - 1 && worldY > 0 && worldY < HEIGHT - 1) {
                    if (world[worldX][worldY] != Tileset.NOTHING) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Connects two room centers with an L-shaped hallway. The bend direction
     * (horizontal-first vs. vertical-first) is chosen randomly so the dungeon
     * layout varies even with the same set of room positions.
     *
     * @param p1 center of the first room
     * @param p2 center of the second room
     */
    private void connectRooms(Position p1, Position p2) {
        if (RandomUtils.bernoulli(random)) {
            // Horizontal segment first, then vertical.
            addHorizontalHallway(p1.x, p2.x, p1.y);
            addVerticalHallway(p1.y, p2.y, p2.x);
        } else {
            // Vertical segment first, then horizontal.
            addVerticalHallway(p1.y, p2.y, p1.x);
            addHorizontalHallway(p1.x, p2.x, p2.y);
        }
    }

    /**
     * Carves a 1-tile-wide horizontal hallway along row {@code y} from column
     * {@code x1} to column {@code x2}, adding wall tiles above and below where
     * the space is currently empty.
     */
    private void addHorizontalHallway(int x1, int x2, int y) {
        int start = Math.min(x1, x2);
        int end = Math.max(x1, x2);

        for (int x = start; x <= end; x++) {
            world[x][y] = Tileset.FLOOR;

            if (y + 1 < HEIGHT && world[x][y + 1] == Tileset.NOTHING) {
                world[x][y + 1] = Tileset.WALL;
            }
            if (y - 1 >= 0 && world[x][y - 1] == Tileset.NOTHING) {
                world[x][y - 1] = Tileset.WALL;
            }
        }

        // Cap both ends of the hallway with a wall tile if the space is empty.
        if (start - 1 >= 0 && world[start - 1][y] == Tileset.NOTHING) {
            world[start - 1][y] = Tileset.WALL;
        }
        if (end + 1 < WIDTH && world[end + 1][y] == Tileset.NOTHING) {
            world[end + 1][y] = Tileset.WALL;
        }
    }

    /**
     * Carves a 1-tile-wide vertical hallway along column {@code x} from row
     * {@code y1} to row {@code y2}, adding wall tiles left and right where
     * the space is currently empty.
     */
    private void addVerticalHallway(int y1, int y2, int x) {
        int start = Math.min(y1, y2);
        int end = Math.max(y1, y2);

        for (int y = start; y <= end; y++) {
            world[x][y] = Tileset.FLOOR;

            if (x - 1 >= 0 && world[x - 1][y] == Tileset.NOTHING) {
                world[x - 1][y] = Tileset.WALL;
            }
            if (x + 1 < WIDTH && world[x + 1][y] == Tileset.NOTHING) {
                world[x + 1][y] = Tileset.WALL;
            }
        }

        // Cap both ends of the hallway with a wall tile if the space is empty.
        if (start - 1 >= 0 && world[x][start - 1] == Tileset.NOTHING) {
            world[x][start - 1] = Tileset.WALL;
        }
        if (end + 1 < HEIGHT && world[x][end + 1] == Tileset.NOTHING) {
            world[x][end + 1] = Tileset.WALL;
        }
    }

    /**
     * Teleports the avatar to {@code (x, y)} without using movement rules.
     * Used when loading a saved game to restore the exact avatar position.
     * The destination tile must be a floor tile.
     *
     * @param x target column
     * @param y target row
     */
    public void forceAvatar(int x, int y) {
        // Clear the avatar's previous tile back to floor.
        if (avatarPosition != null && world[avatarPosition.x][avatarPosition.y] == Tileset.AVATAR) {
            world[avatarPosition.x][avatarPosition.y] = Tileset.FLOOR;
        }

        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && world[x][y] == Tileset.FLOOR) {
            avatarPosition = new Position(x, y);
            world[x][y] = Tileset.AVATAR;
        }
    }

    /**
     * Randomly places {@code count} coin tiles on unoccupied floor tiles
     * (excluding the avatar's starting tile). Updates {@link #coinsLeft} with
     * the actual number of coins placed (may be less than {@code count} if the
     * dungeon has very few floor tiles).
     *
     * @param count desired number of coins to place
     */
    private void placeCoins(int count) {
        int placed = 0;
        int tries = 0;
        while (placed < count && tries < 5000) {
            int x = RandomUtils.uniform(random, WIDTH);
            int y = RandomUtils.uniform(random, HEIGHT);
            if (world[x][y] == Tileset.FLOOR) {
                if (avatarPosition == null || !(avatarPosition.x == x && avatarPosition.y == y)) {
                    world[x][y] = COIN;
                    placed++;
                }
            }
            tries++;
        }
        coinsLeft = placed;
    }
}