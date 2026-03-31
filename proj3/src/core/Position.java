package core;

/**
 * A simple (x, y) coordinate pair used throughout the world generation
 * and game logic to represent tile positions on the 2D grid.
 */
public class Position {
    public int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }
}