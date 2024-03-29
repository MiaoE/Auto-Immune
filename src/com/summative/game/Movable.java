package com.summative.game;

/**
 * [Movable.java]
 * An interface that is implemented by objects that can move to other coordinates on a plane.
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 4.3 2021/01/25
 */
public interface Movable {

    /**
     * move
     * This method moves an object to a specified coordinate of a board.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    void move(int x, int y);
}
