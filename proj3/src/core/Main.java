package core;

import tileengine.TERenderer;
import tileengine.TETile;

public class Main {
    public static void main(String[] args) {

        TERenderer ter = new TERenderer();
        int width = 80;
        int height = 40;

        ter.initialize(width,height);

        long seed =6578897764558030256L;

        WorldGenerator generator = new WorldGenerator(width,height,seed);
        TETile[][] world = generator.generateWorld();

        ter.renderFrame(world);

    }
}
