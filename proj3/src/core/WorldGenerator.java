package core;



import tileengine.TETile;
import tileengine.Tileset;
import utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;

public class WorldGenerator {

    private final int WIDTH;
    private final int HEIGHT;
    private final long seed;
    private final Random random;

    private final TETile[][] world;
    private final List<Position> roomCenters = new ArrayList<>();
    private Position avatarPosition;

    private static final int COIN_ID =89676;

    private final TETile COIN = new TETile('$', Color.YELLOW, Color.BLACK, "coin",COIN_ID);
    private int coinsLeft = 0;

    public int getCoinsLeft() {
        return coinsLeft;
    }






    public WorldGenerator(int width, int height, long seed) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.seed = seed;
        this.random = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];

        fillWithNothing();
    }

    public Position getAvatarPosition(){
        return avatarPosition;
    }

    private void fillWithNothing() {
        for (int x = 0; x <WIDTH; x++) {
            for (int y = 0; y <HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    public TETile[][] generateWorld() {
        int numRooms = RandomUtils.uniform(random,6,13);
        int roomsAdded = 0;
        int attempts = 0;


        while (roomsAdded < numRooms && attempts < 1000) {
            int roomWidth = RandomUtils.uniform(random, 4, 9);
            int roomHeight =  RandomUtils.uniform(random, 4, 9);

            int x = RandomUtils.uniform(random, 1, WIDTH - roomWidth - 1);
            int y = RandomUtils.uniform(random, 1, HEIGHT - roomHeight - 1);

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
        for (int y = 0; y < HEIGHT;y++) {
            for (int x = 0; x <WIDTH;x++) {
                if (world[x][y] == Tileset.FLOOR) {
                    avatarPosition = new Position(x,y);
                    world[x][y] = Tileset.AVATAR;
                    break;
                }
            }
            if (avatarPosition != null) break;
        }
        placeCoins(6);


        return world;

    }

    public boolean tryMove(int dx ,int dy) {

        if (avatarPosition == null) return false;

        int nx = avatarPosition.x +dx;
        int ny = avatarPosition.y +dy;

        if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT) return false;

        TETile target = world[nx][ny];

        if (target == Tileset.WALL || target == Tileset.NOTHING) return false;

        boolean steppingOnCoin = target.id() == COIN_ID;
        if (steppingOnCoin) {
            coinsLeft = Math.max(0,coinsLeft-1);
        }

        world[avatarPosition.x][avatarPosition.y] = Tileset.FLOOR;
        avatarPosition.x = nx;
        avatarPosition.y = ny;
        world[nx][ny] = Tileset.AVATAR;
        return true;
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
        if (RandomUtils.bernoulli(random)) {
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


            if (y+1 < HEIGHT && world[x][y + 1] == Tileset.NOTHING){
                world[x][y +1] = Tileset.WALL;
            }
            if (y-1 >= 0 && world[x][y - 1] == Tileset.NOTHING){
                world[x][y - 1] = Tileset.WALL;
            }
        }

        if (start -1 >= 0 && world[start - 1][y] == Tileset.NOTHING){
            world[start - 1][y] = Tileset.WALL;

        }
        if (end + 1 < WIDTH && world[end +1][y] == Tileset.NOTHING){
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


            if (x-1 >= 0 && world[x - 1][y] == Tileset.NOTHING) {
                world[x - 1][y] = Tileset.WALL;
            }
            if (x + 1 < WIDTH && world[x + 1][y] == Tileset.NOTHING) {
                world[x + 1][y] = Tileset.WALL;
            }
        }


        if (start -1 >= 0 && world[x][start - 1] == Tileset.NOTHING) {
            world[x][start - 1] = Tileset.WALL;
        }

        if (end +1 < HEIGHT && world[x][end + 1] == Tileset.NOTHING) {
            world[x][end + 1] = Tileset.WALL;
        }
    }

    public long getSeed() {
        return seed;
    }

    public void forceAvatar(int x, int y) {

        if (avatarPosition != null && world[avatarPosition.x][avatarPosition.y] == Tileset.AVATAR) {
            world[avatarPosition.x][avatarPosition.y] = Tileset.FLOOR;

        }

        if (x >=0 && x <WIDTH && y >= 0 && y < HEIGHT && world[x][y] == Tileset.FLOOR) {
            avatarPosition = new Position(x, y);
            world[x][y] = Tileset.AVATAR;
        }
    }

    private void placeCoins(int count) {
        int placed = 0;
        int tries = 0;
        while(placed < count && tries < 5000) {
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
