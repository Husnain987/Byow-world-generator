package core;


import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGenerator {

    private final int WIDTH;
    private final int HEIGHT;
    private final long seed;
    private final Random random;

    private final TETile[][] world;
    List<Position> roomCenters = new ArrayList<>();



    public WorldGenerator(int width, int height, long seed) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.seed = seed;
        this.random = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];

        fillWithNothing();
    }

    private void fillWithNothing() {
        for (int x = 0; x <WIDTH; x++) {
            for (int y = 0; y <HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }
    private static class Position {
        int x, y;
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public TETile[][] generateWorld() {
        int numRooms = random.nextInt(7) + 6;
        int roomsAdded = 0;
        int attempts = 0;


        while (roomsAdded < numRooms && attempts < 1000) {
            int roomWidth = random.nextInt(5) + 4;
            int roomHeight =  random.nextInt(5) + 4;

            int x = random.nextInt(WIDTH - roomWidth - 2) + 1;
            int y = random.nextInt(HEIGHT - roomHeight -2) + 1;

            if (!roomOverlaps(x,y,roomWidth,roomHeight)) {
                addRoom(x,y,roomWidth,roomHeight);
                roomCenters.add(new Position(x + roomWidth / 2, y + roomHeight / 2));
                roomsAdded++;
            }

            attempts++;
        }

        for (int i = 1; i < roomCenters.size();i++) {
            Position p1 = roomCenters.get(i-1);
            Position p2 = roomCenters.get(i);
            connectRooms(p1,p2);
        }

        return world;

    }


    private void addRoom(int x, int y, int roomWidth, int roomHeight) {
        for (int dx = -1; dx <= roomWidth; dx++) {
            for (int dy = -1; dy <= roomHeight; dy++) {
                int worldX = x + dx;
                int worldY = y + dy;

                if (worldX > 0 && worldX < WIDTH - 1 && worldY > 0 && worldY < HEIGHT - 1) {
                    if (dx >= 0 && dx < roomWidth && dy >=0 && dy <roomHeight) {
                        world[worldX][worldY] = Tileset.FLOOR;
                    } else {
                        if (world[worldX][worldY] == Tileset.NOTHING) {
                            world[worldX][worldY] = Tileset.WALL;
                        }
                    }
                }
            }
        }
    }

    private boolean roomOverlaps(int x,  int y, int roomWidth, int roomHeight) {
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


    private void connectRooms(Position p1,Position p2) {
        if (random.nextBoolean()) {
            addHorizontalHallway(p1.x,p2.x,p1.y);
            addVerticalHallway(p1.y,p2.y,p2.x);
        } else {
            addVerticalHallway(p1.y,p2.y,p1.x);
            addHorizontalHallway(p1.x,p2.x,p2.y);
        }

    }


    private void addHorizontalHallway(int x1, int x2, int y) {
        int start;
        int end;

        if (x1 < x2) {
            start = x1;
            end = x2;
        }else{
            start = x2;
            end = x1;
        }

        for (int x =start; x <=end; x++) {
            world[x][y] = Tileset.FLOOR;


            if (world[x][y + 1] == Tileset.NOTHING){
                world[x][y +1] = Tileset.WALL;
            }
            if (world[x][y - 1] == Tileset.NOTHING){
                world[x][y - 1] = Tileset.WALL;
            }
        }

        if (world[start - 1][y] == Tileset.NOTHING){
            world[start - 1][y] = Tileset.WALL;

        }
        if (world[end +1][y] == Tileset.NOTHING){
            world[end + 1][y] = Tileset.WALL;
        }
    }


    private void addVerticalHallway(int y1, int y2, int x) {
        int start;
        int end;

        if (y1 < y2) {
            start = y1;
            end = y2;
        } else {
            start = y2;
            end = y1;
        }


        for (int y = start; y <= end; y++) {
            world[x][y] = Tileset.FLOOR;


            if (world[x - 1][y] == Tileset.NOTHING) {
                world[x - 1][y] = Tileset.WALL;
            }
            if (world[x + 1][y] == Tileset.NOTHING) {
                world[x + 1][y] = Tileset.WALL;
            }
        }


        if (world[x][start - 1] == Tileset.NOTHING) {
            world[x][start - 1] = Tileset.WALL;
        }

        if (world[x][end + 1] == Tileset.NOTHING) {
            world[x][end + 1] = Tileset.WALL;
        }
    }


}
