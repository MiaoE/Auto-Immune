package com.summative.game;

/**
 * [Movable.java]
 * An interface that is implemented by objects that can move to other coordinates on a plane.
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 2.0 21/01/07
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

    /**
     * takeKnockback
     * Takes the knockback and moves back a specific knockback tiles.
     *
     */
    void takeKnockback(int dx, int dy);
}
