package com.summative.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/**
 * [Warrior.java]
 * An object representing an infantry unit of the {@code Enemy} side.
 * Infantry is able to attack other objects.
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 2.0 21/01/07
 */
public class EnemyWarrior extends Enemy implements Attackable {
    private final int attackDamage;

    /**
     * Warrior Constructor
     * Contains all the essential information to create a unit.
     *
     * @param x            x position
     * @param y            y position
     * @param health       health
     * @param attackRange  attack range
     * @param attackDamage the damage of the attack
     */
    EnemyWarrior(int x, int y, int health, double movementRange, boolean attackRange, int weight, int attackDamage) {
        super(x, y, new Texture(Gdx.files.internal("EnemyWarrior.png")), health, movementRange, attackRange, weight);
        this.attackDamage = attackDamage;
    }

    /**
     * attack
     * Attacks a tile.
     *
     * @return the damage of the attack
     */
    @Override
    public int attack() {
        return attackDamage;
    }

    @Override
    public void takeKnockback(int x, int y) {

    }

}
