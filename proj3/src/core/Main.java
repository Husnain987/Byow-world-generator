package core;


import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;


import java.awt.Font;

import tileengine.Tileset;
import java.util.ArrayDeque;


public class Main {
    private static boolean fovOn = false;


    public static void main(String[] args) {
        showMainMenu();
    }



    public static void showMainMenu() {
        StdDraw.setCanvasSize(640,640);
        StdDraw.setXscale(0,1);
        StdDraw.setYscale(0,1);
        StdDraw.clear(StdDraw.BLACK);

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD,30));
        StdDraw.text(0.5,0.8,"CS61B: BYOW");


        StdDraw.setFont(new Font("Arial", Font.BOLD,20));
        StdDraw.text(0.5,0.5,"(N) New Game");
        StdDraw.text(0.5,0.4,"(L) Load Game");
        StdDraw.text(0.5,0.3,"(Q) Quit Game");

        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();


                if (key == 'N' || key == 'n') {
                    enterSeed();
                    break;
                } else if (key == 'L' || key  == 'l') {
                    loadAndStart();
                    break;
                } else if (key == 'Q' || key == 'q') {
                    System.exit(0);
                }
            }
        }
    }

    public static void enterSeed(){

        String seedInput = "";

        StdDraw.clear(StdDraw.BLACK);

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial",Font.BOLD,30));
        StdDraw.text(0.5,0.8,"CS61B: BYOW");

        StdDraw.setFont(new Font("Arial", Font.BOLD,20));
        StdDraw.text(0.5,0.7,"Enter seed followed by S");

        StdDraw.setPenColor(StdDraw.YELLOW);
        StdDraw.text(0.5,0.55,seedInput);
        StdDraw.show();



        while (true) {

            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();

                if (Character.isDigit(key)) {
                    seedInput += key;
                } else if (key == 's' || key == 'S') {
                    if (!seedInput.equals("")) {
                        long seed = Long.parseLong(seedInput);
                        int width = 80;
                        int height = 40;
                        TERenderer ter = new TERenderer();
                        ter.initialize(width,height);

                        WorldGenerator generator = new WorldGenerator(width,height,seed);
                        TETile[][] world = generator.generateWorld();


                        renderWithHUD(ter,world,generator);
                        gameLoop(generator,ter,world);
                        break;
                    }

                } else {
                    continue;
                }

                StdDraw.clear(StdDraw.BLACK);
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.setFont(new Font("Arial",Font.BOLD,30));
                StdDraw.text(0.5,0.8,"CS61B: BYOW");

                StdDraw.setFont(new Font("Arial", Font.BOLD,20));
                StdDraw.text(0.5,0.7,"Enter seed followed by S");

                StdDraw.setPenColor(StdDraw.YELLOW);
                StdDraw.text(0.5,0.55,seedInput);
                StdDraw.show();

            }
        }

    }
    private static void gameLoop(WorldGenerator generator, TERenderer ter, TETile[][] world) {
        boolean colonMode = false;
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();

                if (colonMode) {
                    if (key == 'q' || key == 'Q') {
                        saveAndQuit(generator,world.length,world[0].length);
                    }
                    colonMode = false;
                }else {

                    if (key == 'w' || key == 'W') {
                        generator.tryMove(0,1);
                    } else if (key == 'd' || key == 'D') {
                        generator.tryMove(1,0);
                    } else if (key == 's' || key == 'S') {
                        generator.tryMove(0,-1);
                    } else if (key == 'a' || key == 'A') {
                        generator.tryMove(-1,0);
                    } else if (key == ':'){
                        colonMode = true;
                    } else if (key == 'v' || key == 'V'){
                        fovOn = !fovOn;
                    }

                }

            }
            if (generator.getCoinsLeft() == 0) {
                showWinScreen();
                showMainMenu();
                return;
            }
            renderWithHUD(ter,world,generator);
            StdDraw.pause(16);
        }

    }

    private static final String SAVE_FILE = "save.txt";

    private static void saveAndQuit(WorldGenerator gen, int width, int height) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(SAVE_FILE)) {
            Position p = gen.getAvatarPosition();
            out.println(gen.getSeed());
            out.println(width+ " " + height);
            out.println(p.x + " " + p.y);
        } catch (Exception e) {

        }
        System.exit(0);
    }

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
            ter.initialize(width,height);

            WorldGenerator generator = new WorldGenerator(width,height,seed);
            TETile[][] world = generator.generateWorld();

            generator.forceAvatar(ax,ay);
            renderWithHUD(ter,world,generator);
            gameLoop(generator,ter,world);
        } catch (Exception e) {
            System.exit(0);
        }
    }




    private static void renderWithHUD(TERenderer ter,TETile[][] world, WorldGenerator generator) {
        if (fovOn) {
            TETile[][] frame = TETile.copyOf(world);
            boolean[][] visible = computeVisibleMask(world, generator.getAvatarPosition(),7);

            int W = world.length;
            int H =  world[0].length;


            for (int x = 0; x < W; x++) {
                for (int y =0; y < H; y++) {
                    if (!visible[x][y]) {
                        frame[x][y] = Tileset.NOTHING;
                    }
                }
            }
            ter.renderFrame(frame);
        } else {
            ter.renderFrame(world);
        }


        int mx = (int) (StdDraw.mouseX());
        int my = (int) (StdDraw.mouseY());
        String label = "";
        if (mx >= 0 && mx < world.length && my >= 0 && my < world[0].length) {
            label = world[mx][my].description();
        }

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.PLAIN,14));
        String fovText;
        if (fovOn) {
            fovText = "FOV: ON (V)";
        } else {
            fovText = "FOV: OFF (V)";
        }
        StdDraw.textLeft(1,world[0].length-1,label+"  " + fovText);
        StdDraw.textLeft(1,world[0].length -2, "Coins Left: " + generator.getCoinsLeft());
        StdDraw.show();
    }

    private static boolean[][] computeVisibleMask(TETile[][] world, Position start, int radius) {
        int W = world.length;
        int H = world[0].length;
        boolean[][] vis = new boolean[W][H];
        boolean[][] visited = new boolean[W][H];

        if (start == null) return vis;

        ArrayDeque<Position> q = new ArrayDeque<>();
        q.add(new Position(start.x,start.y));
        visited[start.x][start.y] = true;
        vis[start.x][start.y] = true;

        int[] dx = {1,-1,0,0};
        int[] dy = {0,0,1,-1};

        while (!q.isEmpty()) {
            Position p = q.removeFirst();

            int dist =  Math.abs(p.x-start.x) + Math.abs(p.y-start.y);
            if (dist>radius) continue;
            vis[p.x][p.y] = true;

            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];

                if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;

                if (visited[nx][ny]) continue;

                TETile t = world[nx][ny];

                if (t == Tileset.FLOOR || t == Tileset.AVATAR) {
                    visited[nx][ny] = true;
                    q.addLast(new Position(nx,ny));
                }
            }
        }

        for (int x = 0; x < W; x++) {
            for (int y =0; y < H; y++) {
                if (world[x][y] == Tileset.WALL) {
                    boolean nearVisible = false;
                    if(x+1 < W && vis[x+1][y]) nearVisible = true;
                    if (x-1 >= 0 && vis[x-1][y]) nearVisible = true;
                    if(y+1 < H && vis[x][y+1]) nearVisible = true;
                    if (y-1 >= 0 && vis[x][y-1]) nearVisible = true;
                    if (nearVisible) vis[x][y] = true;
                }
            }
        }

        return vis;
    }

    private static void showWinScreen() {


        StdDraw.clear(StdDraw.BLACK);

        StdDraw.setXscale(0,1);
        StdDraw.setYscale(0,1);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD,30));
        StdDraw.text(0.5,0.6,"You got all the coins!!!!");

        StdDraw.setFont(new Font("Arial", Font.BOLD,18));
        StdDraw.text(0.5,0.5,"Returning to main menu...");

        StdDraw.show();
        StdDraw.pause(12000);


    }
}
