package com.summative.game;

import com.badlogic.gdx.graphics.Texture;

/**
 * [Entity.java]
 * This class is the superclass of all beings.
 * All beings are able to move and take damage, but not all can attack.
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 4.3 2021/01/25
 */
abstract class Entity extends GameObject implements Damageable, Movable {

    private int health;
    private final double movementRange;
    private final boolean attackRange;

    /**
     * Entity Constructor
     * The only constructor of the class.
     * In addition to {@link GameObject}, health value is also essential for this object.
     *
     * @param x             the x coordinate
     * @param y             the y coordinate
     * @param texture       the texture of the sprite
     * @param health        the initial health value
     * @param movementRange the movement range
     * @param attackRange   the attack range
     */
    Entity(int x, int y, Texture texture, int health, double movementRange, boolean attackRange) {
        super(x, y, texture);
        this.health = health;
        this.movementRange = movementRange;
        this.attackRange = attackRange;
    }

    /**
     * getHealth
     * Returns the remaining health of the object
     *
     * @return an integer represents the health of an object
     */
    @Override
    public int getHealth() {
        return health;
    }

    /**
     * setHealth
     * Assigns a new health value to the object
     *
     * @param health the new health value
     */
    public void setHealth(int health) {
        this.health = health;
    }

    /**
     * getAttackRange
     * Returns the attack range of the unit
     *
     * @return the attack range
     */
    public boolean getAttackRange() {
        return attackRange;
    }

    /**
     * getMovementRange
     * Returns the movement range of the unit
     *
     * @return the movement range
     */
    public double getMovementRange() {
        return movementRange;
    }

    /**
     * damageTaken
     * This method is used to take away a certain damage caused by {@code attack} method from {@link Attackable}
     *
     * @param damage the damage taken
     * @see Attackable#attack()
     */
    @Override
    public void damageTaken(int damage) {
        setHealth(getHealth() - damage);
    }

    /**
     * move
     * This method moves an object to a specified coordinate of a board.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    @Override
    public void move(int x, int y) {
        setCoordinate(x, y);
    }
}
