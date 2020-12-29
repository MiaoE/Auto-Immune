/**
 * [Vital.java]
 * Sub class of obstacle
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 0.1 20/12/21
 */
public class Vital extends Obstacle implements Damageable {
    private int health;

    Vital(int x, int y, int health) {
        super(x, y);
        this.health = health;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public void damageTaken(int damage) {
        health -= damage;
    }
}
