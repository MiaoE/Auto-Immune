package com.summative.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import com.summative.game.list.List;

import java.util.Random;

/**
 * [GameScreen.java]
 * The game starts with providing context of the schematics prior to the gameplay.
 * It will place {@code Obstacle} on a board afterwards.
 * The objective of the game is to protect vitals at all cost.
 * {@code Enemy} places their units randomly, and {@code Player} places their units in any of
 * the specified empty tiles.
 *
 * Each unit has their own abilities and characteristics.
 * For instance, some units can move far but deals minimal damage while some other units can only move
 * one tile but deals a lot of damage. Furthermore, some units can perform their unique ability.
 *
 * At the end, the player has to strategically place their units and kill enemy players before they
 * destroy a specified number of vitals.
 *
 * @author Ayden Gao
 * @author Eric Miao
 * @version 4.3 2021/01/25
 */
public class GameScreen extends ScreenAdapter {

    MyGame game;

    Texture background = new Texture(Gdx.files.internal("Board2.png"));
    Texture context = new Texture(Gdx.files.internal("Text.png"));
    int windowWidth = Gdx.graphics.getWidth();
    int windowHeight = Gdx.graphics.getHeight();
    int size = 8;
    public GameObject[][] board = new GameObject[size][size];
    int tileSize = 64;
    int halfHeight = windowHeight / 2;

    //Game variables
    boolean playerTurn = false;
    boolean initialize = true;
    int round = 0;
    boolean modeAttack = false;
    Player unitSelected = null;
    int killed = 0;
    long startTime = System.currentTimeMillis();
    long moveElapsedStart;
    int prevEnemy = -1;
    boolean enemyMoving = false;

    //Game Lists
    List<Player> playerList = new List<>();
    List<Enemy> enemyList = generateEnemy(3);
    List<Vital> vitalList = generateVitals(3);
    List<SpawnTile> spawnList = new List<>();

    /**
     * GameScreen Constructor
     * Contains an inherited Game object which is capable of switching the screen.
     *
     * @param game the Game object
     */
    public GameScreen(MyGame game) {
        this.game = game;
    }

    /**
     * show
     * This method sets up a type of action listener while the game is displayed on screen.
     * It is ran only once when player opens the application.
     *
     * @see GameScreen#hide()
     */
    @Override
    public void show() {
        generateKillTiles(2);
        generateObstacles(2);
    }

    /**
     * render
     * This method is the pivot to drawing objects on the screen. This method is constantly running every tick.
     *
     * @param delta the serial number of the screen
     */
    @Override
    public void render(float delta) {
        windowHeight = Gdx.graphics.getHeight();
        windowWidth = Gdx.graphics.getWidth();
        halfHeight = windowHeight / 2;
        //background colour. RGB values = [0, 1]
        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (System.currentTimeMillis() - startTime < 10000) {
            game.batch.begin();
            game.batch.draw(context, 25, Gdx.graphics.getHeight() - 25 - 64 * 3, context.getWidth() * 3, context.getHeight() * 3);
            game.batch.end();
            return;
        }

        drawBoard(game.batch);

        if (initialize) {
            String text = "Currently Placing: ";
            if (playerList.size() < 2) {
                text += "Warrior (Eye-ball; standard movement range; melee; 2 damage; 1 knockback)";
            } else if (playerList.size() < 3) {
                text += "Artillery (Syringe; short movement range; ranged; 1 damage; 1 knockback)";
            } else if (playerList.size() < 5) {
                text += "Support (Pill; long movement range; melee; 0 damage; 5 knockback)";
            }

            game.batch.begin();
            game.font.getData().setScale(1.5f);
            game.font.setColor(Color.WHITE);
            game.font.draw(game.batch, text, Gdx.graphics.getWidth() * .25f, Gdx.graphics.getHeight() - 25);
            game.batch.end();
        }

        //player places down units
        if (initialize && Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            System.out.println(Gdx.input.getX() + " " + Gdx.input.getY());

            //(0, 0) is the top left corner
            Vector2 mousePressed = isoToCart(new Vector2(Gdx.input.getX() - 50, Gdx.input.getY() - halfHeight));
            int tileX = (int) (mousePressed.x / tileSize);
            int tileY = (int) (mousePressed.y / tileSize);

            if (tileX >= size || tileY >= size || tileX < 0 || tileY < 0) {
                System.out.println("Out of bound");
            } else if (board[tileY][tileX] != null) {
                System.out.println("Tile occupied");
            } else {
                if (playerList.size() < 2) {
                    PlayerWarrior unit = new PlayerWarrior(tileX, tileY, 3, 4, false, 2, 1);
                    board[tileY][tileX] = unit;
                    playerList.add(unit);
                } else if (playerList.size() < 3) {
                    PlayerArtillery unit = new PlayerArtillery(tileX, tileY, 2, 3, true, 1, 1);
                    board[tileY][tileX] = unit;
                    playerList.add(unit);
                } else if (playerList.size() < 5) {
                    PlayerSupport unit = new PlayerSupport(tileX, tileY, 2, 5, false, 5);
                    board[tileY][tileX] = unit;
                    playerList.add(unit);
                }
            }
            System.out.println(tileX + " " + tileY);
            System.out.println();

            if (playerList.size() == 5) {
                initialize = false;
                round++;
                enemyMoving = true;
                moveElapsedStart = System.currentTimeMillis();
            }
        }
        if (!initialize && !playerTurn) {
            if (!enemyMoving) {
                executeEnemyAttack();
                spawnEnemies();
                enemyMoving = true;
                moveElapsedStart = System.currentTimeMillis();
            }

            if (playerList.isEmpty() || vitalList.isEmpty()) {
                game.batch.begin();
                game.font.draw(game.batch, "LOSE", 50, 200);
                game.batch.end();
                game.setScreen(new EndScreen(game, false, killed, vitalList.size(), playerList.size()));
                //Gdx.app.exit();
            } else if (round > 5) {
                game.batch.begin();
                game.font.draw(game.batch, "WIN", 50, 200);
                game.batch.end();
                game.setScreen(new EndScreen(game, true, killed, vitalList.size(), playerList.size()));

            } else {
                int index = (int) ((System.currentTimeMillis() - moveElapsedStart) / 1000);
                Enemy enemy = enemyList.get(index);
                if (enemy != null && prevEnemy != index) {
                    prevEnemy = index;
                    enemyAttack(enemy);
                } else if (enemy == null) {
                    generateEnemySpawns();
                    enemyMoving = false;
                    playerTurn = true;
                }
            }

        } else if (playerTurn) {
            playerAction();
        }
    }

    /**
     * hide
     * This method saves the computer memory by disabling action listener while the application is minimized.
     *
     * @see GameScreen#show()
     */
    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    /**
     * drawBoard
     * Draws the board and all the objects on the screen.
     * Includes the red and green outline for attack locations and unit selection.
     *
     * @param batch variable to draw things
     */
    public void drawBoard(SpriteBatch batch) {
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        game.batch.begin();
        game.batch.draw(background, 50, halfHeight - 422, background.getWidth() * 4, background.getHeight() * 4);
        game.font.getData().setScale(1.5f);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "Round " + round, 25, Gdx.graphics.getHeight() - 25);
        game.batch.end();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Vector2[] points = {cartToIso(new Vector2(j * tileSize, i * tileSize)),
                      cartToIso(new Vector2(j * tileSize + tileSize, i * tileSize)),
                      cartToIso(new Vector2(j * tileSize + tileSize, i * tileSize + tileSize)),
                      cartToIso(new Vector2(j * tileSize, i * tileSize + tileSize))};
                float[] vertices = {points[0].x + 50, points[0].y + halfHeight, points[1].x + 50, points[1].y + halfHeight,
                      points[2].x + 50, points[2].y + halfHeight, points[3].x + 50, points[3].y + halfHeight};
                shapeRenderer.begin();
                shapeRenderer.set(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(0, 0, 0, 1);
                shapeRenderer.polygon(vertices);
                shapeRenderer.end();

                if (board[j][i] != null) {
                    game.batch.begin();
                    Vector2 centre = cartToIso(new Vector2(j * tileSize + tileSize / 2f, i * tileSize + tileSize / 2f));
                    game.batch.draw(board[j][i].getTexture(), centre.x + 50 - tileSize / 2f, centre.y + halfHeight - 16);

                    if (board[j][i] instanceof Damageable) {
                        game.font.setColor(Color.BLUE);
                        game.font.getData().setScale(1);
                        game.font.draw(batch, Integer.toString(((Damageable) board[j][i]).getHealth()), centre.x + 50, centre.y + halfHeight - tileSize / 4f);
                    }

                    batch.end();
                }
            }
        }

        shapeRenderer.begin();
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        for (Enemy enemy : enemyList) {
            Point attack;
            if ((attack = enemy.getAttack()) != null) {
                Vector2[] points = {cartToIso(new Vector2(attack.getY() * tileSize, attack.getX() * tileSize)),
                      cartToIso(new Vector2(attack.getY() * tileSize + tileSize, attack.getX() * tileSize)),
                      cartToIso(new Vector2(attack.getY() * tileSize + tileSize, attack.getX() * tileSize + tileSize)),
                      cartToIso(new Vector2(attack.getY() * tileSize, attack.getX() * tileSize + tileSize))};
                float[] vertices = {points[0].x + 50, points[0].y + halfHeight, points[1].x + 50, points[1].y + halfHeight,
                      points[2].x + 50, points[2].y + halfHeight, points[3].x + 50, points[3].y + halfHeight};
                shapeRenderer.polygon(vertices);
            }
        }
        shapeRenderer.end();

        if (playerTurn) {
            shapeRenderer.begin();
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.rect(1100, 150, 100, 50);
            shapeRenderer.rect(1100, 50, 100, 50);
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.set(ShapeRenderer.ShapeType.Line);
            shapeRenderer.rect(1100, 150, 100, 50);
            shapeRenderer.rect(1100, 50, 100, 50);

            if (unitSelected != null) {
                shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Color.GREEN);
                Vector2[] points = {cartToIso(new Vector2(unitSelected.getY() * tileSize, unitSelected.getX() * tileSize)),
                      cartToIso(new Vector2(unitSelected.getY() * tileSize + tileSize, unitSelected.getX() * tileSize)),
                      cartToIso(new Vector2(unitSelected.getY() * tileSize + tileSize, unitSelected.getX() * tileSize + tileSize)),
                      cartToIso(new Vector2(unitSelected.getY() * tileSize, unitSelected.getX() * tileSize + tileSize))};
                float[] vertices = {points[0].x + 50, points[0].y + halfHeight, points[1].x + 50, points[1].y + halfHeight,
                      points[2].x + 50, points[2].y + halfHeight, points[3].x + 50, points[3].y + halfHeight};
                shapeRenderer.polygon(vertices);
            }
            shapeRenderer.end();

            game.batch.begin();
            game.font.setColor(Color.WHITE);
            game.font.getData().setScale(1);
            game.font.draw(batch, "End Turn", 1120, 80);
            if (modeAttack) {
                game.font.draw(batch, "Attack: ON", 1110, 180);
            } else {
                game.font.draw(batch, "Attack: OFF", 1110, 180);
            }
            game.batch.end();
        }
        //shapeRenderer.end();
    }

    /**
     * isoToCart
     * Converts a {@code Vector2} isometric coordinate on cartesian plane to cartesian coordinate
     *
     * @param iso the isometric coordinate
     * @return the cartesian coordinate
     */
    public static Vector2 isoToCart(Vector2 iso) {
        return new Vector2(iso.x / 2 - iso.y, iso.x / 2 + iso.y);
    }

    /**
     * cartToIso
     * Converts a {@code Vector2} cartesian coordinate to isometric coordinate on cartesian plane
     *
     * @param cart the cartesian coordinate
     * @return the isometric coordinate
     */
    public static Vector2 cartToIso(Vector2 cart) {
        return new Vector2(cart.x + cart.y, (cart.y - cart.x) / 2);
    }

    /**
     * generateEnemySpawns
     * Will choose spawn locations for new enemies
     * Method will contain an algorithm to find the number of enemies to spawn
     * Right now method will generate maxEnemyNum - number of enemies on board
     */
    private void generateEnemySpawns() {
        Random rand = new Random();
        int enemyWeightRemaining = 10;
        int generate;
        int x, y;

        for (Enemy enemy : enemyList) {
            enemyWeightRemaining -= enemy.getWeight();
        }

        while (enemyWeightRemaining > 0) {
            SpawnTile temp;
            do {
                x = rand.nextInt(8);
                y = rand.nextInt(8);
            } while (board[y][x] != null);

            generate = rand.nextInt(3);

            if (generate == 0) {
                temp = new SpawnTile(x, y, new EnemyWarrior(x, y, 3, 5, false, 2, 3));
                enemyWeightRemaining -= 2;
            } else if (generate == 1) {
                temp = new SpawnTile(x, y, new EnemyArtillery(x, y, 2, 4, true, 1, 1));
                enemyWeightRemaining -= 1;
            } else {
                temp = new SpawnTile(x, y, new EnemyDestructor(x, y, 3, 5, false, 2, 10));
                enemyWeightRemaining -= 2;
            }
            board[y][x] = temp;
            spawnList.add(temp);
        }
    }

    /**
     * spawnEnemies
     * Spawns enemies onto the game board
     */
    private void spawnEnemies() {
        for (SpawnTile tile : spawnList) {
            board[tile.getY()][tile.getX()] = tile.getEnemy();
            enemyList.add(tile.getEnemy());
        }
        spawnList.clear();
    }

    /**
     * enemyAttack
     * Creates an array list containing possible moves for the enemy
     * Selects an option from the list and executes the move
     */
    private void enemyAttack(Enemy enemy) {
        List<Point[]> options = new List<>();
        GameObject closestObject = null;
        for (Player player : playerList) {
            Point[] cord = enemyAttackable(player, enemy);
            if (cord[0] != null) {
                options.add(cord);
            }

            if (closestObject == null) {
                closestObject = player;
            } else if (distance(player.getCoordinate(), enemy.getCoordinate()) < distance(closestObject.getCoordinate(), enemy.getCoordinate())) {
                closestObject = player;
            }
        }
        for (Vital vital : vitalList) {
            Point[] cord = enemyAttackable(vital, enemy);
            if (cord[0] != null) {
                options.add(cord);
            }

            if (closestObject == null) {
                closestObject = vital;
            } else if (distance(vital.getCoordinate(), enemy.getCoordinate()) < distance(closestObject.getCoordinate(), enemy.getCoordinate())) {
                closestObject = vital;
            }
        }

        //This section will move the enemy
        if (!options.isEmpty()) {
            Random rand = new Random();
            Point[] option = options.get(rand.nextInt(options.size()));

            board[enemy.getY()][enemy.getX()] = null;//makes prev position null
            enemy.move(option[0].getX(), option[0].getY());//changes x and y THERE IS AN ERROR HERE IDK WHY BUT LOIEK WE GO FIC LSOL
            board[option[0].getY()][option[0].getX()] = enemy;//changes position on the game board

            enemy.setAttack(option[1]);//changes attack X and Y
        } else {
            Point moveTo = enemyMove(enemy, closestObject);

            board[enemy.getY()][enemy.getX()] = null;//makes prev position null
            enemy.move(moveTo.getX(), moveTo.getY());//changes x and y
            board[moveTo.getY()][moveTo.getX()] = enemy;//changes position on the game board

            if (enemy.getX() == closestObject.getX() && Math.abs(enemy.getY() - closestObject.getY()) == 1) {
                enemy.setAttack(closestObject.getCoordinate());
                System.out.println("enemy at " + enemy.getCoordinate().toString() + " will attack " + enemy.getAttack().toString());
            } else if (enemy.getY() == closestObject.getY() && Math.abs(enemy.getX() - closestObject.getX()) == 1) {
                enemy.setAttack(closestObject.getCoordinate());
                System.out.println("enemy at " + enemy.getCoordinate().toString() + " will attack " + enemy.getAttack().toString());
            } else {
                enemy.setAttack(null);
            }
        }

    }

    /**
     * distance
     * This helper checks the distance between object a and object b.
     *
     * @param a first object
     * @param b second object
     * @return distance between a and b, stored in double
     */
    private double distance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    /**
     * enemyMove
     * This method is only executed if enemy cannot move to a nearby object and can only move
     * closer to an object.
     *
     * @param enemy  the enemy unit
     * @param object the object closest to the enemy (but still out of range for 1 turn)
     * @return the furthest point where enemy can move to
     */
    private static Point enemyMove(Enemy enemy, GameObject object) {
        int moveToX, moveToY;

        //if x within movement range, y is out of range
        if (Math.abs(enemy.getX() - object.getX()) <= enemy.getMovementRange()) {//NULL ERROR HERE, MAYBE OBJECT GETS DESTROYED
            moveToX = object.getX();

            int yDisplacement = (int) Math.sqrt(Math.pow(enemy.getMovementRange(), 2) - Math.pow(enemy.getX() - object.getX(), 2));
            //if enemy is below object, enemy moves up
            if (enemy.getY() < object.getY()) {
                moveToY = enemy.getY() + yDisplacement;
            } else {
                moveToY = enemy.getY() - yDisplacement;
            }
            return new Point(moveToX, moveToY);
        }

        //if y within movement range, x is out of range
        if (Math.abs(enemy.getY() - object.getY()) <= enemy.getMovementRange()) {
            moveToY = object.getY();

            int xDisplacement = (int) Math.sqrt(Math.pow(enemy.getMovementRange(), 2) - Math.pow(enemy.getY() - object.getY(), 2));
            //if enemy is to the left of object, enemy moves right
            if (enemy.getX() < object.getX()) {
                moveToX = enemy.getX() + xDisplacement;
            } else {
                moveToX = enemy.getX() - xDisplacement;
            }
            return new Point(moveToX, moveToY);
        }

        //x and y are both out of range, enemy has to move furthest diagonally
        int distance = (int) Math.sqrt(Math.pow(enemy.getMovementRange(), 2) / 2);

        //determine direction
        if (enemy.getX() < object.getX() && enemy.getY() < object.getY()) {
            moveToX = enemy.getX() + distance;
            moveToY = enemy.getY() + distance;
        } else if (enemy.getX() < object.getX()) {
            moveToX = enemy.getX() + distance;
            moveToY = enemy.getY() - distance;
        } else if (enemy.getY() < object.getY()) {
            moveToX = enemy.getX() - distance;
            moveToY = enemy.getY() + distance;
        } else {
            moveToX = enemy.getX() - distance;
            moveToY = enemy.getY() - distance;
        }

        return new Point(moveToX, moveToY);
    }

    /**
     * enemyAttackable
     * This method determines whether the inputted enemy can attack the inputted object
     * if not then the method returns null
     *
     * @param object the object getting attacked
     * @param enemy  the attacker
     * @return a size 2 Point array, [0] move location, [1] attack location
     */
    private Point[] enemyAttackable(GameObject object, Enemy enemy) {
        Point[] point = new Point[2];

        Point enemyCord = enemy.getCoordinate();
        int xO = object.getX();
        int yO = object.getY();

        point[1] = object.getCoordinate();
        if (!enemy.getAttackRange()) {//melee attacker
            double distance, shortestDistance = 100;
            //for loop to check directly adjacent tiles instead of multiple if statements
            if ((xO + 1 < size) && (board[yO][xO + 1] == null) && (distance = distance(enemyCord, new Point(xO + 1, yO))) <= enemy.getMovementRange()) {
                if (point[0] == null) {
                    shortestDistance = distance;
                    point[0] = new Point(xO + 1, yO);
                }
            }
            if ((xO - 1 > -1) && (board[yO][xO - 1] == null) && (distance = distance(enemyCord, new Point(xO - 1, yO))) <= enemy.getMovementRange()) {
                if (point[0] == null || distance < shortestDistance) {
                    shortestDistance = distance;
                    point[0] = new Point(xO - 1, yO);
                }
            }
            if ((yO + 1 < size) && (board[yO + 1][xO] == null) && (distance = distance(enemyCord, new Point(xO, yO + 1))) <= enemy.getMovementRange()) {
                if (point[0] == null || distance < shortestDistance) {
                    shortestDistance = distance;
                    point[0] = new Point(xO, yO + 1);
                }
            }
            if ((yO - 1 > -1) && (board[yO - 1][xO] == null) && (distance = distance(enemyCord, new Point(xO, yO - 1))) <= enemy.getMovementRange()) {
                if (point[0] == null || distance < shortestDistance) {
                    point[0] = new Point(xO, yO - 1);
                }
            }
            //if enemy is at adjacent tile
            if ((Math.abs(enemyCord.getX() - xO) == 1 && enemyCord.getY() == yO) || (Math.abs(enemyCord.getY() - yO) == 1 && enemyCord.getX() == xO)) {
                point[0] = new Point(enemy.getX(), enemy.getY());
            }
            return point;
        } else {//ranged attacker new Point(xO+1, yO)new Point(xO+1, yO)
            if (yO == enemyCord.getY() || xO == enemyCord.getX()) {
                point[0] = enemyCord;
                return point;
            }

            for (int i = 0; i < size; i++) {
                if ((board[yO][i] == null) && (distance(enemyCord, new Point(i, yO)) <= enemy.getMovementRange())) {//horizontal axis
                    point[0] = new Point(i, yO);
                    return point;
                } else if ((board[i][xO] == null) && (distance(enemyCord, new Point(xO, i)) <= enemy.getMovementRange())) {//vertical axis
                    point[0] = new Point(xO, i);
                    return point;
                }
            }
        }
        point[1] = null;
        point[0] = enemyMove(enemy, object);
        return point;//if enemy can't attack the object
    }

    /**
     * executeEnemyAttack
     * Executes the attack of the selected enemy.
     * This method should include enemy list as enemies will be pushed.
     * Enemies won't target themselves and their allies but they can be damaged if pushed by player.
     */
    private void executeEnemyAttack() {
        int bound = enemyList.size();
        Enemy enemy;
        for (int i = 0; i < bound; i++) {//for each loop is wack, if you remove something it breaks
            enemy = enemyList.get(i);
            Point attack = enemy.getAttack();

            if (attack == null) {//enemy doesn't attack
                continue;//goes to the next enemy
            }
            System.out.println("Enemy " + enemy.getCoordinate().toString() + " attacks " + attack.toString());
            GameObject object = board[attack.getY()][attack.getX()];
            if (object instanceof Damageable) {
                ((Damageable) object).damageTaken(((Attackable) enemy).attack());//takes damage

                //removes the object from board if health <= 0
                if (((Damageable) object).getHealth() <= 0) {//if object is destroyed or killed
                    board[attack.getY()][attack.getX()] = null;
                    System.out.println("Object at " + object.getCoordinate().toString() + " is obliterated");
                    if (object instanceof Player) {
                        playerList.remove(((Player) object));
                    } else if (object instanceof Enemy) {
                        killed++;
                        enemyList.remove(((Enemy) object));
                        if (enemyList.indexOf(((Enemy) object)) < i) {
                            i = i - 1;
                        }
                        bound -= 1;
                    } else if (object instanceof Vital) {
                        vitalList.remove(((Vital) object));
                    }
                }
            }
            if (enemy instanceof EnemyDestructor) {
                board[attack.getY()][attack.getX()] = new KillTile(attack.getX(), attack.getY());
                System.out.println("Kill tile created at " + attack.getX() + " " + attack.getY());
            }
        }
    }

    /**
     * playerAction
     * Allows players to select a unit and choose a specified action for that unit.
     * Player's turn will not end until the player insists on ending their turn.
     */
    public void playerAction() {
        if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.input.getY();

            Vector2 mousePressed = isoToCart(new Vector2(mouseX - 50, mouseY - halfHeight));
            int tileX = (int) (mousePressed.x / tileSize);
            int tileY = (int) (mousePressed.y / tileSize);

            //if attack button pressed
            if (mouseX < 1200 && mouseX > 1100 && mouseY < windowHeight - 150 && mouseY > windowHeight - 200) {
                if (modeAttack) {
                    modeAttack = false;
                    System.out.println("attack: off");
                } else {
                    modeAttack = true;
                    System.out.println("attack: on");
                }
            }

            //end turn
            if (mouseX < 1200 && mouseX > 1100 && mouseY < windowHeight - 50 && mouseY > windowHeight - 100) {
                modeAttack = false;
                resetPlayers();
                unitSelected = null;
                playerTurn = false;
                round++;
            }

            //if clicks nothing
            if (tileX < 0 || tileX >= size || tileY < 0 || tileY >= size || mousePressed.x < 0 || mousePressed.y < 0) {
                return;
            }

            //if clicks to select a player
            if (board[tileY][tileX] instanceof Player && unitSelected == null) {//select unit
                unitSelected = (Player) board[tileY][tileX];

                System.out.println("Select a tile to move or attack");

            } else if (unitSelected == null) {
                System.out.println("Please select unit first before doing other actions");
            } else if (board[tileY][tileX] == null) {
                if (modeAttack) {
                    playerAttack(unitSelected, tileX, tileY);
                } else if (unitSelected.isMoved()) {
                    System.out.println("Unit has already moved");
                } else {
                    playerMove(unitSelected, tileX, tileY);
                }
                unitSelected = null;
                System.out.println("Select unit");
            } else if (board[tileY][tileX] instanceof SpawnTile) {
                if (modeAttack) {
                    playerAttack(unitSelected, tileX, tileY);
                } else if (unitSelected.isMoved()) {
                    System.out.println("Unit has already moved");
                } else {
                    playerMove(unitSelected, tileX, tileY);
                }
                unitSelected = null;
                System.out.println("Select unit");
            } else {
                if (modeAttack) {
                    playerAttack(unitSelected, tileX, tileY);
                } else {
                    System.out.println("You cannot move there");
                }
                unitSelected = null;
                System.out.println("Select unit");
            }
        }

    }

    /**
     * playerAttack
     * Player will select a x and y coordinate to attack,
     * Player unit will attack the coordinate if the unit has not attacked previously in the same turn.
     *
     * @param player the player attacking
     * @param x      x coordinate
     * @param y      y coordinate
     */
    private void playerAttack(Player player, int x, int y) {
        if (player.isAttacked()) {
            System.out.println("Unit has already attacked");
            return;
        }

        if (!player.getAttackRange()) {//melee
            if (!((Math.abs(player.getX() - x)) <= 1 && Math.abs(player.getY() - y) <= 1)) {
                System.out.println("out of range");
                return;
            }
        } else {
            if (player.getX() != x && player.getY() != y) {
                System.out.println("out of range");
                return;
            }
        }

        if (player.getX() == x && player.getY() == y) {
            System.out.println("you can't attack yourself");
            return;
        }

        System.out.println("Player at " + player.getCoordinate().toString() + " attacks " + x + " " + y);
        GameObject target = board[y][x];
        if (target instanceof Damageable && player instanceof Attackable) {
            ((Damageable) target).damageTaken(((Attackable) player).attack());

            if (((Damageable) target).getHealth() <= 0) {//if object is destroyed or killed
                board[target.getY()][target.getX()] = null;
                System.out.println("Object at " + target.getCoordinate().toString() + " is destroyed or killed");
                if (target instanceof Enemy) {//removes enemy from enemy list
                    killed++;
                    enemyList.remove(((Enemy) target));
                } else if (target instanceof Player) {
                    playerList.remove(((Player) target));
                } else if (target instanceof Vital) {
                    vitalList.remove(((Vital) target));
                }
            } else if (target instanceof Movable) {
                takeKnockback(player, target);
            }
        } else if (target instanceof Movable) {//For player support
            takeKnockback(player, target);
        }

        player.setAttacked(true);

    }

    /**
     * playerMove
     * Moves the Player unit to the inputted coordinate.
     *
     * @param unit the unit to move
     * @param x    x coordinate
     * @param y    y coordinate
     */
    private void playerMove(Player unit, int x, int y) {
        if (unit.isMoved()) {//if player has moved already
            System.out.println("player has moved already");
            return;
        }

        if (Math.sqrt(Math.pow(x - unit.getX(), 2) + Math.pow(y - unit.getY(), 2)) > unit.getMovementRange()) {
            System.out.println("Out of movement range");
            return;
        }

        if (board[y][x] instanceof SpawnTile) {
            spawnList.remove(((SpawnTile) board[y][x]));
        }
        board[unit.getY()][unit.getX()] = null;
        board[y][x] = unit;
        unit.move(x, y);
        unit.setMoved(true);
    }

    /**
     * takeKnockback
     * Will move a unit int knockback units away from its original position
     * If the knocked back unit is an enemy, the location of its attack will change accordingly
     *
     * @param player the object exerting the knockback
     * @param object the object getting knocked back
     */
    private void takeKnockback(Player player, GameObject object) {
        //when this method is called the object are already adjacent to one another
        int attackerX = player.getX();//the object attacking
        int attackerY = player.getY();
        int attackeeX = object.getX();//the object getting attacked
        int attackeeY = object.getY();

        int direction;//if the movement is in a positive or negative direction
        boolean vertical;//if the movement is vertical

        if (attackerX == attackeeX && attackerY < attackeeY) {//DOWN
            direction = 1;
            vertical = true;
        } else if (attackerX == attackeeX && attackerY > attackeeY) {//UP
            direction = -1;
            vertical = true;
        } else if (attackerY == attackeeY && attackerX > attackeeX) {//LEFT
            direction = -1;
            vertical = false;
        } else {//RIGHT
            direction = 1;
            vertical = false;
        }

        int x, y;
        for (int i = 1; i <= player.getKnockback(); i++) {//loops 'knockback' amount of times
            if (vertical) {//vertical knockback
                x = attackeeX;
                y = attackeeY + (i * direction);
            } else {//horizontal knockback
                x = attackeeX + (i * direction);
                y = attackerY;
            }

            if (x >= size || y >= size || y < 0 || x < 0) {//checks if values are in bound
                break;
            }
            if (board[y][x] == null) {// if the tile can be moved on
                //moves the object onto that tile
                board[object.getY()][object.getX()] = null;
                ((Movable) object).move(x, y);
                board[y][x] = object;

                if (object instanceof Enemy) {//if object is an attackable enemy
                    if (((Enemy) object).getAttack() != null) {//if there is an attack
                        if (vertical) {//vertical push
                            //if out of bounds, cancel the enemy attack
                            if (((Enemy) object).getAttack().getY() + direction < 0 || ((Enemy) object).getAttack().getY() + direction >= size) {
                                ((Enemy) object).setAttack(null);
                                System.out.println("Attack cancelled");
                            } else {
                                ((Enemy) object).setAttack(new Point(((Enemy) object).getAttack().getX(), ((Enemy) object).getAttack().getY() + (direction)));
                            }
                        } else {//horizontal push
                            if (((Enemy) object).getAttack().getX() + direction < 0 || ((Enemy) object).getAttack().getX() + direction >= size) {
                                ((Enemy) object).setAttack(null);
                                System.out.println("Attack cancelled");
                            } else {
                                ((Enemy) object).setAttack(new Point(((Enemy) object).getAttack().getX() + (direction), ((Enemy) object).getAttack().getY()));
                            }
                        }
                    }
                }
            } else if (board[y][x] instanceof KillTile) {
                if (object instanceof Enemy) {
                    killed++;
                    enemyList.remove(((Enemy) object));//remove object from list
                } else if (object instanceof Player) {
                    playerList.remove(((Player) object));//remove object from list
                }
                board[object.getY()][object.getX()] = null;//remove object from board
                System.out.println("object at " + attackeeX + " " + attackeeY + " is killed by kill tile at " + x + " " + y);
                break;
            } else if (board[y][x] instanceof SpawnTile) {
                if (i == player.getKnockback()) {//does not go back
                    board[object.getY()][object.getX()] = null;
                    spawnList.remove(((SpawnTile) board[y][x]));
                    board[y][x] = object;
                    ((Movable) object).move(x, y);
                } else {//still goes back
                    int nextX, nextY;
                    if (vertical) {
                        nextX = attackeeX;
                        nextY = attackeeY + ((i + 1) * direction);
                    } else {//horizontal knockback
                        nextX = attackeeX + ((i + 1) * direction);
                        nextY = attackerY;
                    }

                    if (nextX >= size || nextY >= size || nextY < 0 || nextX < 0) {//if out of bound, the enemy stays at the spawn tile location
                        board[object.getY()][object.getX()] = null;
                        spawnList.remove(((SpawnTile) board[y][x]));
                        board[y][x] = object;
                        ((Movable) object).move(x, y);
                    } else if (board[nextY][nextX] != null && !(board[nextY][nextX] instanceof KillTile)) {//if next tile has object that's not kill
                        board[object.getY()][object.getX()] = null;
                        spawnList.remove(((SpawnTile) board[y][x]));
                        board[y][x] = object;
                        ((Movable) object).move(x, y);
                    }
                }
            } else {
                break;
            }
        }

        if (object instanceof Enemy) {//prints out new position and attack position
            if (((Enemy) object).getAttack() != null) {
                System.out.println("object at " + object.getX() + " " + object.getY() + " will attack at " + ((Enemy) object).getAttack().getX() + " " + ((Enemy) object).getAttack().getY());
            }
        }
    }

    /**
     * resetPlayers
     * Resets the moved and attacked boolean variables.
     * This method runs after the Player ends their turn.
     */
    private void resetPlayers() {
        for (Player unit : playerList) {
            unit.setAttacked(false);
            unit.setMoved(false);
        }
    }

    /**
     * generateKillTiles
     * Adds {@code KillTile} to the game board.
     *
     * @param killTileNum the number of kill tiles to generate
     * @see Obstacle
     */
    private void generateKillTiles(int killTileNum) {
        Random random = new Random();
        int x, y;
        for (int i = 0; i < killTileNum; i++) {
            do {
                x = random.nextInt(8);
                y = random.nextInt(8);
            } while (board[y][x] != null);
            board[y][x] = new KillTile(x, y);
        }
    }

    /**
     * generateObstacles
     * Adds {@code Obstacle} to the game board.
     *
     * @param obstructionNum the number of obstacles to add
     * @see Obstacle
     */
    private void generateObstacles(int obstructionNum) {
        Random random = new Random();
        int x, y;
        for (int i = 0; i < obstructionNum; i++) {
            do {
                x = random.nextInt(8);
                y = random.nextInt(8);
            } while (board[y][x] != null);
            board[y][x] = new Obstruction(x, y, 3);
        }
    }

    /**
     * generateVitals
     * Adds {@code Vital} to the game board.
     *
     * @param vitalNum the number of vitals to add
     * @return the List of vitals
     * @see Vital
     */
    private List<Vital> generateVitals(int vitalNum) {
        Random random = new Random();
        int x, y;
        List<Vital> vitalList = new List<>();
        for (int i = 0; i < vitalNum; i++) {
            do {
                x = random.nextInt(8);
                y = random.nextInt(8);
            } while (board[y][x] != null);
            Vital vital = new Vital(x, y, 2);
            board[y][x] = vital;
            vitalList.add(vital);
        }
        return vitalList;
    }

    /**
     * generateEnemy
     * Adds {@code Enemy} to the game board. Additionally,
     * the method creates and returns an List containing the enemies generated.
     *
     * @param enemyNum the number of enemies to be created
     * @return the List of enemies
     * @see Enemy
     */
    private List<Enemy> generateEnemy(int enemyNum) {
        Random random = new Random();
        List<Enemy> enemyList = new List<>();
        int x, y;
        int generate;
        Enemy temp;
        for (int i = 0; i < enemyNum; i++) {
            do {
                x = random.nextInt(8);
                y = random.nextInt(8);
            } while (board[y][x] != null);

            generate = random.nextInt(3);

            if (generate == 0) {
                temp = new EnemyArtillery(x, y, 2, 4, true, 1, 1);
            } else if (generate == 1) { //--------end
                temp = new EnemyWarrior(x, y, 3, 5, false, 2, 2);
            } else {
                temp = new EnemyDestructor(x, y, 3, 5, false, 2, 10);
            }
            board[y][x] = temp;
            enemyList.add(temp);
        }
        return enemyList;
    }
}
