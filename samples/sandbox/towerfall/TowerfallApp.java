/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sandbox.towerfall;

import com.almasb.ents.Entity;
import com.almasb.ents.component.UserDataComponent;
import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.entity.GameEntity;
import com.almasb.fxgl.entity.component.CollidableComponent;
import com.almasb.fxgl.entity.component.TypeComponent;
import com.almasb.fxgl.gameplay.Level;
import com.almasb.fxgl.gameplay.rpg.quest.Quest;
import com.almasb.fxgl.gameplay.rpg.quest.QuestObjective;
import com.almasb.fxgl.gameplay.rpg.quest.QuestPane;
import com.almasb.fxgl.gameplay.rpg.quest.QuestWindow;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.parser.TextLevelParser;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.settings.GameSettings;
import com.almasb.fxgl.ui.InGamePanel;
import com.almasb.fxgl.ui.InGameWindow;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class TowerfallApp extends GameApplication {

    private TowerfallFactory factory = new TowerfallFactory();
    private GameEntity player;
    private CharacterControl playerControl;

    public GameEntity getPlayer() {
        return player;
    }

    public TowerfallFactory getFactory() {
        return factory;
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Towerfall");
        settings.setVersion("0.1");
        settings.setFullScreen(false);
        settings.setIntroEnabled(false);
        settings.setMenuEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setCloseConfirmation(false);
        settings.setApplicationMode(ApplicationMode.DEVELOPER);
    }

    @Override
    protected void initInput() {
        Input input = getInput();

        input.addAction(new UserAction("Left") {
            @Override
            protected void onAction() {
                playerControl.left();
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Right") {
            @Override
            protected void onAction() {
                playerControl.right();
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Jump") {
            @Override
            protected void onActionBegin() {
                playerControl.jump();
                jumps.set(jumps.get() + 1);
            }
        }, KeyCode.W);

        input.addAction(new UserAction("Break") {
            @Override
            protected void onActionBegin() {
                playerControl.stop();
            }
        }, KeyCode.S);

        input.addAction(new UserAction("Shoot") {
            @Override
            protected void onActionBegin() {
                playerControl.shoot(input.getMousePositionWorld());
                shotArrows.set(shotArrows.get() + 1);
            }
        }, KeyCode.F);

        input.addAction(new UserAction("Open/Close Panel") {
            @Override
            protected void onActionBegin() {
                if (panel.isOpen())
                    panel.close();
                else
                    panel.open();
            }
        }, KeyCode.TAB);
    }

    @Override
    protected void initAssets() {}

    @Override
    protected void initGame() {
        shotArrows = new SimpleIntegerProperty(0);
        jumps = new SimpleIntegerProperty(0);
        enemiesKilled = new SimpleIntegerProperty(0);

        TextLevelParser parser = new TextLevelParser(factory);
        Level level = parser.parse("towerfall_level.txt");

        player = (GameEntity) level.getEntities()
                .stream()
                .filter(e -> e.hasComponent(TypeComponent.class))
                .filter(e -> e.getComponentUnsafe(TypeComponent.class).isType(EntityType.PLAYER))
                .findAny()
                .get();

        playerControl = player.getControlUnsafe(CharacterControl.class);

        getGameWorld().setLevel(level);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.ARROW, EntityType.PLATFORM) {
            @Override
            protected void onCollisionBegin(Entity arrow, Entity platform) {
                // necessary since we can collide with two platforms in the same frame
                if (arrow.hasControl(ArrowControl.class)) {
                    arrow.getComponentUnsafe(CollidableComponent.class).setValue(false);
                    arrow.removeControl(ArrowControl.class);
                }
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.ARROW, EntityType.ENEMY) {
            @Override
            protected void onCollisionBegin(Entity arrow, Entity enemy) {
                if (arrow.getComponentUnsafe(UserDataComponent.class).getValue() == enemy)
                    return;

                arrow.removeFromWorld();
                enemy.removeFromWorld();
                enemiesKilled.set(enemiesKilled.get() + 1);

                getGameWorld().addEntity(factory.newEnemy(27, 6));
            }
        });
    }

    private InGamePanel panel;

    private IntegerProperty shotArrows;
    private IntegerProperty jumps;
    private IntegerProperty enemiesKilled;

    @Override
    protected void initUI() {
        panel = new InGamePanel();
        getGameScene().addUINode(panel);

        QuestPane questPane = new QuestPane(350, 450);
        QuestWindow window = new QuestWindow(questPane);

        getGameScene().addUINode(window);

        List<Quest> quests = Arrays.asList(
                new Quest("Test Quest", Arrays.asList(
                        new QuestObjective("Shoot Arrows", shotArrows, 15),
                        new QuestObjective("Jump", jumps)
                )),

                new Quest("Test Quest 2", Arrays.asList(
                        new QuestObjective("Shoot Arrows", shotArrows, 25, Duration.seconds(3))
                )),

                new Quest("Test Quest 2", Arrays.asList(
                        new QuestObjective("Kill an enemy", enemiesKilled)
                )),

                new Quest("Test Quest 2", Arrays.asList(
                        new QuestObjective("Shoot Arrows", shotArrows, 25)
                )),

                new Quest("Test Quest 2", Arrays.asList(
                        new QuestObjective("Shoot Arrows", shotArrows, 25)
                ))
        );

        quests.forEach(getQuestManager()::addQuest);
    }

    @Override
    protected void onUpdate(double tpf) {}

    public static void main(String[] args) {
        launch(args);
    }
}
