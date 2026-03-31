package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.Font;

import tileengine.Tileset;
import java.util.ArrayDeque;

/**
 * Entry point and top-level controller for the BYOW dungeon crawler.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Rendering the main menu, seed-entry screen, and win screen.</li>
 *   <li>Running the main game loop (keyboard input → avatar movement).</li>
 *   <li>Save/load: writing and reading {@value #SAVE_FILE}.</li>
 *   <li>Computing the field-of-view mask when FOV mode is active.</li>
 * </ul>
 *
 * <p>Controls:
 * <pre>
 *   W / A / S / D  – move avatar (up / left / down / right)
 *   V              – toggle field-of-view (fog-of-war)
 *   : then Q       – save and quit
 * </pre>
 */
public class Main {

    /** World width and height in tiles. */
    private static final int WORLD_WIDTH = 80;
    private static final int WORLD_HEIGHT = 40;

    /** Path to the save file, relative to the working directory. */
    private static final String SAVE_FILE = "save.txt";

    /** Whether field-of-view (fog-of-war) mode is currently active. */
    private static boolean fovOn = false;

    public static void main(String[] args) {
        showMainMenu();
    }

    /**
     * Displays the main menu and waits for the player to press N (new game),
     * L (load game), or Q (quit).
     */
    public static void showMainMenu() {
        StdDraw.setCanvasSize(640, 640);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.clear(StdDraw.BLACK);

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD, 30));
        StdDraw.text(0.5, 0.8, "CS61B: BYOW");

        StdDraw.setFont(new Font("Arial", Font.BOLD, 20));
        StdDraw.text(0.5, 0.5, "(N) New Game");
        StdDraw.text(0.5, 0.4, "(L) Load Game");
        StdDraw.text(0.5, 0.3, "(Q) Quit Game");

        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();

                if (key == 'N' || key == 'n') {
                    enterSeed();
                    break;
                } else if (key == 'L' || key == 'l') {
                    loadAndStart();
                    break;
                } else if (key == 'Q' || key == 'q') {
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Prompts the player to type a numeric seed followed by 'S', then
     * generates a new world with that seed and starts the game loop.
     * The screen updates after each digit so the player can see what they typed.
     */
    public static void enterSeed() {
        String seedInput = "";

        // Initial render of the seed-entry screen.
        drawSeedScreen(seedInput);

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();

                if (Character.isDigit(key)) {
                    seedInput += key;
                } else if (key == 's' || key == 'S') {
                    if (!seedInput.isEmpty()) {
                        long seed = Long.parseLong(seedInput);
                        TERenderer ter = new TERenderer();
                        ter.initialize(WORLD_WIDTH, WORLD_HEIGHT);

                        WorldGenerator generator = new WorldGenerator(WORLD_WIDTH, WORLD_HEIGHT, seed);
                        TETile[][] world = generator.generateWorld();

                        renderWithHUD(ter, world, generator);
                        gameLoop(generator, ter, world);
                        break;
                    }
                } else {
                    // Ignore non-digit, non-S keys.
                    continue;
                }

                // Re-render to show the updated seed string.
                drawSeedScreen(seedInput);
            }
        }
    }

    /** Draws the seed-entry prompt screen showing {@code currentSeed}. */
    private static void drawSeedScreen(String currentSeed) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD, 30));
        StdDraw.text(0.5, 0.8, "CS61B: BYOW");

        StdDraw.setFont(new Font("Arial", Font.BOLD, 20));
        StdDraw.text(0.5, 0.7, "Enter seed followed by S");

        StdDraw.setPenColor(StdDraw.YELLOW);
        StdDraw.text(0.5, 0.55, currentSeed);
        StdDraw.show();
    }

    /**
     * Main game loop. Reads keyboard input each frame and dispatches to the
     * appropriate action. Runs at ~60 fps (16 ms pause per frame).
     *
     * <p>Two-key sequences:
     * <ul>
     *   <li>{@code :Q} – save state to {@value #SAVE_FILE} and quit.</li>
     * </ul>
     *
     * @param generator the world generator holding game state
     * @param ter       the tile renderer
     * @param world     the 2-D tile array (mutated in place by the generator)
     */
    private static void gameLoop(WorldGenerator generator, TERenderer ter, TETile[][] world) {
        boolean colonMode = false; // true after ':' is pressed, waiting for a command key

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();

                if (colonMode) {
                    if (key == 'q' || key == 'Q') {
                        saveAndQuit(generator, world.length, world[0].length);
                    }
                    colonMode = false;
                } else {
                    if (key == 'w' || key == 'W') {
                        generator.tryMove(0, 1);
                    } else if (key == 'd' || key == 'D') {
                        generator.tryMove(1, 0);
                    } else if (key == 's' || key == 'S') {
                        generator.tryMove(0, -1);
                    } else if (key == 'a' || key == 'A') {
                        generator.tryMove(-1, 0);
                    } else if (key == ':') {
                        colonMode = true;
                    } else if (key == 'v' || key == 'V') {
                        fovOn = !fovOn;
                    }
                }
            }

            // Win condition: all coins collected.
            if (generator.getCoinsLeft() == 0) {
                showWinScreen();
                showMainMenu();
                return;
            }

            renderWithHUD(ter, world, generator);
            StdDraw.pause(16); // ~60 fps
        }
    }

    /**
     * Saves the current game state (seed, world dimensions, avatar position)
     * to {@value #SAVE_FILE}, then exits the application.
     *
     * <p>Save file format (one value per line):
     * <pre>
     *   &lt;seed&gt;
     *   &lt;width&gt; &lt;height&gt;
     *   &lt;avatarX&gt; &lt;avatarY&gt;
     * </pre>
     */
    private static void saveAndQuit(WorldGenerator gen, int width, int height) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(SAVE_FILE)) {
            Position p = gen.getAvatarPosition();
            out.println(gen.getSeed());
            out.println(width + " " + height);
            out.println(p.x + " " + p.y);
        } catch (Exception e) {
            // If saving fails, still exit cleanly.
        }
        System.exit(0);
    }

    /**
     * Reads {@value #SAVE_FILE}, regenerates the world with the stored seed,
     * repositions the avatar to the saved coordinates, and starts the game loop.
     * Exits the application if the save file is missing or malformed.
     */
    private static void loadAndStart() {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    java.nio.file.Paths.get(SAVE_FILE));

            long seed = Long.parseLong(lines.get(0).trim());

            String[] wh = lines.get(1).trim().split("\\s+");
            int width = Integer.parseInt(wh[0]);
            int height = Integer.parseInt(wh[1]);

            String[] xy = lines.get(2).trim().split("\\s+");
            int ax = Integer.parseInt(xy[0]);
            int ay = Integer.parseInt(xy[1]);

            TERenderer ter = new TERenderer();
            ter.initialize(width, height);

            WorldGenerator generator = new WorldGenerator(width, height, seed);
            TETile[][] world = generator.generateWorld();

            // Restore the avatar to the exact saved position.
            generator.forceAvatar(ax, ay);
            renderWithHUD(ter, world, generator);
            gameLoop(generator, ter, world);
        } catch (Exception e) {
            // No valid save file found; return to the main menu.
            System.exit(0);
        }
    }

    /**
     * Renders one frame: the tile world (with FOV mask applied if active) plus
     * a HUD overlay showing the tile description under the cursor, FOV status,
     * and the number of coins remaining.
     *
     * @param ter       the tile renderer
     * @param world     the canonical (unmasked) tile array
     * @param generator game state, used to query avatar position and coin count
     */
    private static void renderWithHUD(TERenderer ter, TETile[][] world, WorldGenerator generator) {
        if (fovOn) {
            // Apply fog-of-war: copy the world and hide tiles outside FOV radius.
            TETile[][] frame = TETile.copyOf(world);
            boolean[][] visible = computeVisibleMask(world, generator.getAvatarPosition(), 7);

            int W = world.length;
            int H = world[0].length;
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    if (!visible[x][y]) {
                        frame[x][y] = Tileset.NOTHING;
                    }
                }
            }
            ter.renderFrame(frame);
        } else {
            ter.renderFrame(world);
        }

        // HUD: tile description from mouse hover + FOV toggle hint + coin counter.
        int mx = (int) StdDraw.mouseX();
        int my = (int) StdDraw.mouseY();
        String label = "";
        if (mx >= 0 && mx < world.length && my >= 0 && my < world[0].length) {
            label = world[mx][my].description();
        }

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.PLAIN, 14));
        String fovText = fovOn ? "FOV: ON (V)" : "FOV: OFF (V)";
        StdDraw.textLeft(1, world[0].length - 1, label + "  " + fovText);
        StdDraw.textLeft(1, world[0].length - 2, "Coins Left: " + generator.getCoinsLeft());
        StdDraw.show();
    }

    /**
     * Computes a boolean visibility mask using BFS from the avatar's position.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>BFS expands through floor and avatar tiles within Manhattan distance
     *       {@code radius}. This lets the player see connected floor space but
     *       not through walls.</li>
     *   <li>Any wall tile adjacent to a visible floor tile is also marked
     *       visible, so the player can see the dungeon boundary.</li>
     * </ol>
     *
     * @param world  the tile array
     * @param start  the avatar's current position
     * @param radius maximum Manhattan distance from the avatar (in tiles)
     * @return a {@code W×H} array where {@code true} means the tile is visible
     */
    private static boolean[][] computeVisibleMask(TETile[][] world, Position start, int radius) {
        int W = world.length;
        int H = world[0].length;
        boolean[][] vis = new boolean[W][H];
        boolean[][] visited = new boolean[W][H];

        if (start == null) return vis;

        ArrayDeque<Position> q = new ArrayDeque<>();
        q.add(new Position(start.x, start.y));
        visited[start.x][start.y] = true;
        vis[start.x][start.y] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        // Phase 1: BFS through walkable tiles within the radius.
        while (!q.isEmpty()) {
            Position p = q.removeFirst();

            int dist = Math.abs(p.x - start.x) + Math.abs(p.y - start.y);
            if (dist > radius) continue;
            vis[p.x][p.y] = true;

            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];

                if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                if (visited[nx][ny]) continue;

                TETile t = world[nx][ny];
                if (t == Tileset.FLOOR || t == Tileset.AVATAR) {
                    visited[nx][ny] = true;
                    q.addLast(new Position(nx, ny));
                }
            }
        }

        // Phase 2: reveal wall tiles that border a visible floor tile.
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (world[x][y] == Tileset.WALL) {
                    boolean nearVisible = (x + 1 < W  && vis[x + 1][y])
                                      || (x - 1 >= 0  && vis[x - 1][y])
                                      || (y + 1 < H   && vis[x][y + 1])
                                      || (y - 1 >= 0  && vis[x][y - 1]);
                    if (nearVisible) vis[x][y] = true;
                }
            }
        }

        return vis;
    }

    /**
     * Displays the win screen for 12 seconds, then returns so the caller can
     * transition back to the main menu.
     */
    private static void showWinScreen() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD, 30));
        StdDraw.text(0.5, 0.6, "You got all the coins!!!!");

        StdDraw.setFont(new Font("Arial", Font.BOLD, 18));
        StdDraw.text(0.5, 0.5, "Returning to main menu...");

        StdDraw.show();
        StdDraw.pause(12000);
    }
}