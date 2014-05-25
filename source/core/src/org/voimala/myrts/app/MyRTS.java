package org.voimala.myrts.app;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import org.voimala.myrts.multiplayer.RTSProtocolManager;
import org.voimala.myrts.scenes.gameplay.GameMode;
import org.voimala.myrts.scenes.gameplay.WorldController;
import org.voimala.myrts.scenes.gameplay.WorldRenderer;
import org.voimala.myrts.multiplayer.ClientThread;
import org.voimala.myrts.multiplayer.ServerThread;

import java.util.HashMap;
import java.util.logging.Logger;

public class MyRTS extends ApplicationAdapter {

    public static int LOG_LEVEL = Application.LOG_DEBUG;

    private static final String TAG = MyRTS.class.getName();

    private WorldController worldController; // TODO Move to gameplay scene
    private WorldRenderer worldRenderer; // TODO Move to gameplay scene

    private boolean paused = false;

    private ServerThread serverThread;
    private ClientThread clientThread;

    private GameMode gameMode = GameMode.MULTIPLAYER;
    private long fixedPhysicsFps = 10;
    private long lastWorldUpdateTimestamp = 0;

    private HashMap<String, String> commandLineArguments = new HashMap<String, String>();

    public MyRTS(String[] commandLineArguments) {
        super();
        saveCommandLineArguments(commandLineArguments);
    }

    private void saveCommandLineArguments(String[] commandLineArguments) {
        for (int i = 0; i < commandLineArguments.length; i = i + 2) {
            try {
                this.commandLineArguments.put(commandLineArguments[i], commandLineArguments[i + 1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(MyRTS.LOG_LEVEL);

        handleCommandLineArguments();

        // TODO Create scene
        worldController = new WorldController(this);
        RTSProtocolManager.getInstance().setWorldController(worldController);
        worldRenderer = new WorldRenderer(worldController);
    }

    private void handleCommandLineArguments() {
        if (commandLineArguments.get("-multiplayer").equals("host")) {
            hostGame();
            joinGame();
        } else if (commandLineArguments.get("-multiplayer").equals("join")) {
            joinGame();
        }
    }

    private void joinGame() {
        clientThread = new ClientThread(
                commandLineArguments.get("-ip"),
                Integer.valueOf(commandLineArguments.get("-port")));
        clientThread.start();
    }

    private void hostGame() {
        serverThread = new ServerThread(
                Integer.valueOf(commandLineArguments.get("-port")));
        serverThread.start();
    }

    @Override
    public void render() {
        // Update game world
        if (!paused) {
            float deltaTime = Gdx.graphics.getDeltaTime();
            deltaTime = fixDeltaTimeMinAndMaxValues(deltaTime);

            updateInput(deltaTime);
            updateWorld(deltaTime);
            renderWorld();
        }
    }

    private float fixDeltaTimeMinAndMaxValues(float deltaTime) {
        if (deltaTime > 0.06) {
            deltaTime = (float) 0.06;
        }

        return deltaTime;
    }

    private void updateInput(final float deltaTime) {
        worldController.updateInputManager(deltaTime);
    }

    private void updateWorld(float deltaTime) {
        if (gameMode == GameMode.SINGLEPLAYER) {
            updateWorldUsingVariablePhysics(deltaTime);
        } else if (gameMode == GameMode.MULTIPLAYER) {
            updateWorldUsingFixedPhysics();
        }
    }

    private void updateWorldUsingVariablePhysics(final float deltaTime) {
        worldController.updateWorld(deltaTime);
    }

    private void updateWorldUsingFixedPhysics() {
        // Update game world when 1 / fixedPhysicsFps seconds have passed.
        if (System.currentTimeMillis() >= lastWorldUpdateTimestamp + (long) (((float) 1 / (float) fixedPhysicsFps) * 1000)) {
            float deltaTime = (float) 1 / (float) fixedPhysicsFps;
            worldController.updateWorld(deltaTime);

            lastWorldUpdateTimestamp = System.currentTimeMillis();
        }
    }

    private void renderWorld() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        worldRenderer.render();
    }

    @Override
    public void resize(int width, int height) {
        worldRenderer.resize(width, height);
    }

    @Override
    public void pause() {
        /* TODO This is called on the desktop when the windows is minimized.
         * This is not the desired case. */
        //paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void dispose() {
        if (clientThread != null) {
            clientThread.die();
        }

        if (serverThread != null) {
            serverThread.die();
        }

        worldRenderer.dispose();
    }

    public ServerThread getServerThread() {
        return serverThread;
    }

    public ClientThread getClientThread() {
        return clientThread;
    }
}
