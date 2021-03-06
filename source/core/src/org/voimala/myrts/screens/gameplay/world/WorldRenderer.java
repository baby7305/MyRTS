package org.voimala.myrts.screens.gameplay.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import org.voimala.myrts.app.GameMain;
import org.voimala.myrts.audio.SoundContainer;
import org.voimala.myrts.graphics.SpriteContainer;
import org.voimala.myrts.networking.ChatContainer;
import org.voimala.myrts.screens.gameplay.effects.AbstractEffect;
import org.voimala.myrts.screens.gameplay.ammunition.AbstractAmmunition;
import org.voimala.myrts.screens.gameplay.multiplayer.MultiplayerSynchronizationManager;
import org.voimala.myrts.screens.gameplay.units.AbstractUnit;
import org.voimala.myrts.screens.gameplay.units.turrets.AbstractTurret;
import org.voimala.utility.ArrayHelper;

public class WorldRenderer implements Disposable {

    private static final String TAG = WorldRenderer.class.getName();
    private SpriteBatch batch;
    private SpriteBatch hudBatch;
    private WorldController worldController;
    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private int chatMessagesXScreen = 80;
    private int chatMessagesYScreen = Gdx.graphics.getHeight() - 70;

    private BitmapFont defaultFont;

    /** Remember to call setWorldController soon after constructing this object! */
    public WorldRenderer() {
        initialize();
    }

    private void initialize () {
        initializeSprites();
        initializeAudioEffects();
        initializePointer();
        initializeFonts();
        initializeBatches();
    }

    private void initializeSprites() {
        initializeGroundSprites();
        initializeUnitSprites();
        initializeAmmunitionSprites();
        initializeEffectSprites();
    }

    private void initializeGroundSprites() {
        Texture texture = new Texture("graphics/textures/ground/grass1.jpg");
        Sprite sprite = new Sprite(texture);
        SpriteContainer.getInstance().addSprite("grass1", sprite);
    }

    private void initializeUnitSprites() {
        Texture texture = new Texture("graphics/units/m4/m4-stopped-0.png");
        Sprite sprite = new Sprite(texture);
        SpriteContainer.getInstance().addSprite("m4-stopped-0", sprite);

        Texture texture2 = new Texture("graphics/units/m4/m4-stopped-0_enemytemp.png");
        Sprite sprite2 = new Sprite(texture2);
        SpriteContainer.getInstance().addSprite("m4-stopped-0_enemytemp", sprite2);
    }

    private void initializeAmmunitionSprites() {
        Texture texture = new Texture("graphics/weapons/m4-bullet.png");
        Sprite sprite = new Sprite(texture);
        SpriteContainer.getInstance().addSprite("m4-bullet", sprite);
    }

    private void initializeEffectSprites() {
        Texture texture1 = new Texture("graphics/effects/muzzle-fires/general-muzzle-fire1.png");
        Sprite sprite1 = new Sprite(texture1);
        SpriteContainer.getInstance().addSprite("general-muzzle-fire1", sprite1);

        Texture texture2 = new Texture("graphics/effects/muzzle-fires/general-muzzle-fire2.png");
        Sprite sprite2 = new Sprite(texture2);
        SpriteContainer.getInstance().addSprite("general-muzzle-fire2", sprite2);

        Texture texture3 = new Texture("graphics/effects/muzzle-fires/general-muzzle-fire3.png");
        Sprite sprite3 = new Sprite(texture3);
        SpriteContainer.getInstance().addSprite("general-muzzle-fire3", sprite3);
    }

    private void initializeAudioEffects() {
        initializeM4AudioEffects();
    }

    private void initializeM4AudioEffects() {
        Sound m4 = Gdx.audio.newSound(Gdx.files.internal("sound/weapons/m4.ogg"));
        SoundContainer.getInstance().addSound("m4", m4);

        initializeUnitCommandAudioEffects("m4");
    }

    private void initializeUnitCommandAudioEffects(final String unitName) {
        /* Unit command sound effects are located in:
        * sound/units/UNIT_NAME/COMMAND_NAME/NUMBER
        * Audio files should be named NUMBER.ogg starting from 1, for example
        * 1.ogg, 2.ogg, 3.ogg etc. */
        loadUnitCommandAudioEffectsFromFolder(unitName, "select");
        loadUnitCommandAudioEffectsFromFolder(unitName, "move");
        loadUnitCommandAudioEffectsFromFolder(unitName, "attack");
    }

    private void loadUnitCommandAudioEffectsFromFolder(final String unitName, final String commandName) {
        int i = 1;
        while (true) {
            try {
                Sound sound = Gdx.audio.newSound(Gdx.files.internal("sound/units/m4/" + commandName + "/" + i + ".ogg"));
                SoundContainer.getInstance().addSound(unitName + "-" + commandName + i, sound);
                i++;
            } catch (Exception e) {
                break; // No more new audio files available.
            }
        }
    }

    private void initializePointer() {
        Pixmap pixelmap = new Pixmap(Gdx.files.internal("graphics/pointers/pointer-basic-0.png"));
        Gdx.input.setCursorImage(pixelmap, 0, 0);
        pixelmap.dispose();
    }

    private void initializeFonts() {
        defaultFont = new BitmapFont();
        defaultFont.setColor(Color.WHITE);
    }

    private void initializeBatches() {
        batch = new SpriteBatch();
        hudBatch = new SpriteBatch();
    }

    public void render(RenderMode renderMode, final float deltaTime) {
        /* This game uses the standard mathematic circle where 0 degrees point to right,
         * 90 degrees point to up etc. Libgdx seems to use a circle where 0 degrees
         * point to up, 90 degrees point to left etc. WorldRenderer makes the conversion
         * automatically. */

        batch.setProjectionMatrix(worldController.getGameplayScreen().getWorldCamera().combined);

        renderMode = RenderMode.WORLD_STATE; // For testing purposes only


        batch.begin();
        renderGround();
        renderUnits(worldController);
        renderAmmunition(worldController);
        renderEffects(worldController);
        batch.end();
        renderUnitEnergyBars(worldController);
        renderHud();
        renderUnitSelectionRectangle();
        renderInfoText(renderMode);
        renderDebugHelpers(worldController);
        renderNetworkText();
        renderChat();
    }

    private void renderGround() {
        for (int i = 0; i < 60; i++) { // TODO Map size
            for (int j = 0; j < 60; j++) {

                Sprite sprite = SpriteContainer.getInstance().getSprite("grass1");
                sprite.setPosition(i * worldController.TILE_SIZE_PIXELS, j * worldController.TILE_SIZE_PIXELS);
                sprite.draw(batch);
            }
        }
    }

    private void renderUnits(final WorldController worldController) {
        for (AbstractUnit unit : worldController.getUnitContainer().getAllUnits()) {
            // Draw unit
            Sprite unitSprite = unit.getSprite();
            if (unitSprite != null) {

                unitSprite.setOrigin(unitSprite.getWidth() / 2, unitSprite.getHeight() / 2 - 70);
                unitSprite.setPosition(unit.getX() - unitSprite.getWidth() / 2, unit.getY() - unitSprite.getWidth() / 2 + 70);
                unitSprite.setRotation(unit.getAngle() - 90);
                unitSprite.draw(batch);

            }

            // Draw turrets
            for (AbstractTurret turret : unit.getTurrets()) {
                Sprite turretSprite = turret.getSprite();
                if (turretSprite != null) {
                    turretSprite.setOrigin(turretSprite.getWidth() / 2, turretSprite.getHeight() / 2 - 70);
                    turretSprite.setPosition(turret.getX() - turretSprite.getWidth() / 2, turret.getY() - turretSprite.getWidth() / 2 + 70);
                    turretSprite.setRotation(turret.getAngle() - 90);
                    turretSprite.draw(batch);
                }
            }
        }
    }

    private void renderAmmunition(final WorldController worldController) {
        for (AbstractAmmunition ammunition : worldController.getAmmunitionContainer()) {

            Sprite sprite = ammunition.getSprite();

            // Draw unit
            sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
            sprite.setPosition(ammunition.getX() - sprite.getWidth() / 2, ammunition.getY() - sprite.getWidth() / 2);
            sprite.setRotation(ammunition.getAngle() - 90);
            sprite.draw(batch);


        }
    }

    private void renderEffects(final WorldController worldController) {
        for (AbstractEffect effect : worldController.getEffectsContainer()) {

            Sprite sprite = effect.getSprite();

            // Draw unit
            sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2 - 40);
            sprite.setPosition(effect.getX() - sprite.getWidth() / 2, effect.getY() - sprite.getWidth() / 2 + 40);
            sprite.setRotation(effect.getAngle() - 90);
            sprite.setAlpha(1 - effect.getLivedLifeAsPercent());
            sprite.draw(batch);
        }
    }

    private void renderUnitEnergyBars(final WorldController worldController) {
        for (AbstractUnit unit : worldController.getUnitContainer().getAllUnits()) {
            if (unit.isSelected()) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Color.WHITE);
                Vector3 unitTopLeftWorldCoordinates = new Vector3(unit.getX() - unit.getWidth() / 2,
                        unit.getY() + unit.getHeight() / 2, 0);
                Vector3 unitTopRightWorldCoordinates = new Vector3(unit.getX() + unit.getWidth() / 2,
                        unit.getY() + unit.getHeight() / 2, 0);

                Vector3 unitTopLeftScreenCoordinates = worldController.getGameplayScreen().getWorldCamera().project(unitTopLeftWorldCoordinates);
                Vector3 unitTopRightScreenCoordinates = worldController.getGameplayScreen().getWorldCamera().project(unitTopRightWorldCoordinates);

                shapeRenderer.rect(unitTopLeftScreenCoordinates.x,
                        unitTopLeftScreenCoordinates.y,
                        ((float) unit.getEnergy() / (float) unit.getMaxEnergy()) * (unitTopRightScreenCoordinates.x - unitTopLeftScreenCoordinates.x),
                        10);
                shapeRenderer.end();
            }
        }
    }

    private float calculateDeltaTimeBetweenLastWorldUpdateAndCurrentTime() {
        return (System.currentTimeMillis() - worldController.getGameplayScreen().getLastWorldUpdateTimestamp()) / (float) 1000;
    }

    private void renderHud() {
        // TODO Implement hud
    }

    private void renderUnitSelectionRectangle() {
        Rectangle selectionRectangle = worldController.getGameplayScreen().getLocalGameplayInputManager().getUnitSelectionRectangle();

        if (selectionRectangle != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 255, 0, (float) 0.5);
            shapeRenderer.rect(selectionRectangle.x,
                    selectionRectangle.y,
                    selectionRectangle.width,
                    selectionRectangle.height);
            shapeRenderer.end();
        }
    }

    private void renderInfoText(final RenderMode renderMode) {
        hudBatch.begin();
        defaultFont.draw(hudBatch,
                "Project \"MyRTS\", early alpha version",
                10,
                Gdx.graphics.getHeight() - 10);
        String renderModeText = renderMode == RenderMode.WORLD_STATE ? "World state" : "Physics prediction";
        defaultFont.draw(hudBatch,
                String.valueOf(Gdx.graphics.getFramesPerSecond()) + "fps (frame "
                        + worldController.getGameplayScreen().getRenderTick() + ", mode: " + renderModeText + ")",
                10,
                Gdx.graphics.getHeight() - 10 - defaultFont.getLineHeight());
        defaultFont.draw(hudBatch, "Units: " + worldController.getUnitContainer().getAllUnits().size(),
                10,
                Gdx.graphics.getHeight() - 10 - defaultFont.getLineHeight() * 2);
        defaultFont.draw(hudBatch,
                "World Update Tick: " + worldController.getWorldUpdateTick(),
                10,
                Gdx.graphics.getHeight() - 10 - defaultFont.getLineHeight() * 3);
        defaultFont.draw(hudBatch,
                "SimTick: " + MultiplayerSynchronizationManager.getInstance().getSimTick(),
                10,
                Gdx.graphics.getHeight() - 10 - defaultFont.getLineHeight() * 4);
        hudBatch.end();
    }

    private void renderDebugHelpers(WorldController worldController) {
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            for (AbstractUnit unit : worldController.getUnitContainer().getAllUnits()) {
                for (AbstractTurret turret : unit.getTurrets()) {
                    if (turret.hasTarget()) {
                        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                        shapeRenderer.setColor(0, 255, 0, (float) 0.5);
                        shapeRenderer.line(worldController.getGameplayScreen().getWorldCamera().project(
                                        new Vector3(turret.getX(), turret.getY(), 0)),
                                worldController.getGameplayScreen().getWorldCamera().project(
                                        new Vector3(turret.getTarget().getX(), turret.getTarget().getY(), 0))
                        );
                        shapeRenderer.end();
                    }
                }
            }
        }

    }

    private void renderNetworkText() {
        if (MultiplayerSynchronizationManager.getInstance().isWaitingInputForNextSimTick()
                && MultiplayerSynchronizationManager.getInstance().getTimeStamptWaitingInputFromNetworkMs() > 2000) {
            hudBatch.begin();
            defaultFont.draw(hudBatch,
                    "Waiting for player...",
                    10,
                    Gdx.graphics.getHeight() - 200);
            hudBatch.end();
        }
    }

    private void renderChat() {
        renderUserMessage();
        renderChatMessages();
    }

    private void renderUserMessage() {
        if (worldController.getGameplayScreen().getGameplayChatInputManager().isChatTypingOn()) {
            hudBatch.begin();
            defaultFont.draw(hudBatch,
                    "[ALL]" + " " + GameMain.getInstance().getPlayer().getName() + ": " +
                            worldController.getGameplayScreen().getGameplayChatInputManager().getUserChatMessage(),
                    chatMessagesXScreen,
                    Gdx.graphics.getHeight() - chatMessagesYScreen);
            hudBatch.end();
        }
    }

    private void renderChatMessages() {
        if (ChatContainer.getInstance().getMillisecondsPassedSinceLastMessageReceived() < 10000
                || worldController.getGameplayScreen().getGameplayChatInputManager().isChatTypingOn()) {
            hudBatch.begin();
            int numberOfMessages = 10;
            String[] chatMessages = ChatContainer.getInstance().getNewestChatMessagesAsStrings(numberOfMessages);
            chatMessages = ArrayHelper.reverseArray(chatMessages);
            for (int i = 0; i < chatMessages.length; i++) {
                defaultFont.draw(hudBatch,
                        chatMessages[i],
                        chatMessagesXScreen,
                        Gdx.graphics.getHeight() - chatMessagesYScreen + defaultFont.getLineHeight() + i * defaultFont.getLineHeight());

            }
            hudBatch.end();
        }
    }

    public void resize(int width, int height) {
        worldController.getGameplayScreen().getWorldCamera().viewportWidth = width;
        worldController.getGameplayScreen().getWorldCamera().viewportHeight = height;
        worldController.getGameplayScreen().getWorldCamera().update();

        // Resize hud
        hudBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void dispose() {
        defaultFont.dispose();
    }

    public void setWorldController(final WorldController worldController) {
        this.worldController = worldController;
    }
}