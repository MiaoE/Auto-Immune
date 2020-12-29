/**
 * [Warrior.java]
 * Subclass of enemy
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 0.1 20/12/22
 */
public class Warrior extends Enemy implements Attackable {

    /**
     * Warrior Constructor
     * @param x x position
     * @param y y position
     * @param health health
     * @param attackDamage attack damage
     * @param attackRange attack range
     */
    Warrior(int x, int y, int health, int attackDamage, int attackRange){
        super(x, y, health, attackDamage, attackRange);
    }

    /**
     * attack
     *
     * @return the damage of the attack
     */
    @Override
    public int attack() {
        return getAttackDamage();
    }
}
