package org.voimala.myrts.networking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerThread extends Thread {
    private static final String TAG = ServerThread.class.getName();
    private ServerSocketHints serverSocketHints;
    private ServerSocket serverSocket;
    private boolean running = true;
    private int port = 0;
    private ArrayList<ClientThread> connectedClients = new ArrayList<ClientThread>();
    private HashMap<Integer, SlotContent> slots = new HashMap<Integer, SlotContent>();
    private String motd = "Welcome to the server!";

    public ServerThread(final int port) {
        super(ServerThread.class.getName());

        initializeGameSlots();
        initializeSocketSettings(port);
    }

    private void initializeGameSlots() {
        for (int i = 1; i <= 8; i++) {
            slots.put(i, SlotContent.OPEN);
        }
    }

    private void initializeSocketSettings(int port) {
        serverSocketHints = new ServerSocketHints();
        serverSocketHints.acceptTimeout = 100000;
        serverSocketHints.receiveBufferSize = 90000;
        this.port = port;
    }

    public void run() {
        NetworkManager.getInstance().setHost(true);

        createServer();
        acceptConnections();

        Gdx.app.debug(TAG, "Server stopped.");
        NetworkManager.getInstance().setHost(false);
    }

    private void createServer() {
        try {
            Gdx.app.debug(TAG, "Creating a server...");
            serverSocket = Gdx.net.newServerSocket(Net.Protocol.TCP, port, serverSocketHints);
            Gdx.app.debug(TAG, "Server created");
        } catch (Exception e) {
            Gdx.app.debug(TAG, "Error creating a server: " + e.getMessage());
            running = false; // TODO Multiplayer lobby does not notice this?
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Gdx.app.debug(TAG, "Listening connections...");
                SocketHints socketHints = new SocketHints();
                socketHints.connectTimeout = 10000;
                socketHints.receiveBufferSize = 90000;
                socketHints.sendBufferSize = 90000;
                socketHints.tcpNoDelay = true;
                Socket clientSocket = serverSocket.accept(socketHints);

                Gdx.app.debug(TAG, "Client connected from" + " " + clientSocket.getRemoteAddress());

                ClientThread client = new ClientThread(this, clientSocket);
                connectedClients.add(client);
                assignSlotToPlayer(client);
                client.sendMessage(RTSProtocolManager.getInstance().createNetworkMessageOfTheDay(motd));
                client.start();
            } catch (Exception e) {
                Gdx.app.debug(TAG, "Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private void assignSlotToPlayer(ClientThread client) {
        // Find the next free slot
        for (int i = 1; i <= 8; i++) {
            if (slots.get(i) == SlotContent.OPEN) {
                client.getPlayerInfo().setNumber(i);
                client.sendMessage(RTSProtocolManager.getInstance().createNetworkMessageSlot(i));
                break;
            }
        }

        // No free slot found
        // TODO
    }

    public void sendMessageToAllClients(final String message) {
        try {
            for (ClientThread client : connectedClients) {
                client.getSocket().getOutputStream().write(message.getBytes());
            }
        } catch (IOException e) {
            Gdx.app.debug(TAG, "WARNING: Unable to send message to client." + " " + e.getMessage());
        }
    }

    public void die() {
        for (ClientThread client : connectedClients) {
            client.die();
        }

        running = false;
        serverSocket.dispose();
    }

    public void removeClient(final ClientThread clientThread) {
        connectedClients.remove(clientThread);
    }
}
