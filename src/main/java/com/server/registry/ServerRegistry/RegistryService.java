package com.server.registry.ServerRegistry;

import com.server.registry.ServerRegistry.Exception.NoServersFound;
import com.server.registry.ServerRegistry.Model.Room;
import com.server.registry.ServerRegistry.Model.Server;
import com.server.registry.ServerRegistry.Protocol.*;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Zakariyya Raji 04/01/2018.
 * <p>
 * Warning: The Chatservers must be provisioned with enough processing
 * capacity to handle the number of maximum assigned users at MAX load.
 * This is because already connected users are not relocated during heavy load.
 */
public class RegistryService implements Verticle {
    private Map<String, Server> servers = new HashMap<>();
    private Map<String, EventHandler> eventHandler = new HashMap<>();
    private Map<String, MessageHandler> messageHandler = new HashMap<>();
    private Map<String, String> lastPolled = new HashMap<>();
    private Vertx vertx;
    private IOLogger logger = new IOLogger();

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        this.vertx = vertx;

        eventHandler.put(RoomEvent.ACTION, EventHandler.HandleRoom);
        eventHandler.put(ServerEvent.ACTION, EventHandler.HandleServer);

        messageHandler.put(Lookup.ACTION, MessageHandler.HandleLookup);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        startRegistryEventListener();
        startRegistryLookupService();
        startHitCountLog();
    }

    private void startRegistryEventListener() {
        vertx.createHttpServer().websocketHandler(event -> {
            System.out.println("SERVER BRIDGE INITIATES CONNECTION ");
            event.handler(data -> {
                Packet packet = (Packet) Serializer.unpack(data.toString(), Packet.class);

                if (eventHandler.get(packet.getAction()) != null) {
                    eventHandler.get(packet.getAction()).handle(data.toString(), this);
                    logger.in();

                }
            });

            event.closeHandler(close -> {
                servers.clear();
            });

        }).listen(Configuration.CONNECTOR_PORT);
        System.out.println("Registry running on port " + Configuration.CONNECTOR_PORT);
    }

    private void startRegistryLookupService() {
        vertx.createHttpServer().websocketHandler(event -> {

            System.out.println("SERVER INITIATES LOOKUP CONNECTION");

            event.handler(data -> {
                Packet packet = (Packet) Serializer.unpack(data.toString(), Packet.class);

                if (messageHandler.get(packet.getAction()) != null) {
                    messageHandler.get(packet.getAction()).handle(
                            event.textHandlerID(), data.toString(), this);
                    logger.out();
                }
            });

        }).listen(Configuration.CLIENT_PORT);
        System.out.println("Lookup service running on port " + Configuration.CLIENT_PORT);
    }

    private void startHitCountLog() {
        vertx.setPeriodic(Configuration.LOG_INTERVAL, event -> {
            sendBus(Configuration.BUS_LOGGING, logger);
            sendBus(Configuration.BUS_LOGGING, new ServerTreeLog(servers));
            logger.reset();
        });
    }

    protected void sendBus(String address, Object data) {
        vertx.eventBus().send(address, Serializer.pack(data));
    }

    /**
     * Get a list of servers that is advertised as ready for more clients.
     *
     * @return List of all servers ready for clients.
     */
    protected ArrayList<Server> getReadyServers() {
        ArrayList<Server> list = new ArrayList<>();

        for (Server server : servers.values()) {
            if (!server.getFull())
                list.add(server);
        }
        return list;
    }


    /**
     * Returns a single server that is ready for more clients.
     *
     * @param roomName servers subscribed to roomName are preferred
     *                 as it is beneficial to group users to minimize
     *                 replication. Rooms that have more hits/connects are also
     *                 preferred.
     * @return A single server that has the highest match priority, if all servers
     * have equal priority the server will be randomized.
     * @throws NoServersFound if no servers are subscribed on the connector
     *                        or if all subscribed connectors are full.
     */
    protected Server getReadyServer(String roomName) throws NoServersFound {
        ArrayList<Server> ready = new ArrayList<>(getReadyServers());

        Server preferred = getPrioritized(ready, roomName);
        Integer hits = -1;

        for (Server server : ready) {
            Room room = server.getRooms().get(roomName);

            if (room != null && room.getHits() > hits) {
                preferred = server;
                hits = room.getHits();
            }
        }

        if (preferred.getRooms().containsKey(roomName))
            preferred.getRooms().get(roomName).hit();
        else
            lastPolled.put(roomName, preferred.getName());
        return preferred;
    }

    /**
     * Prioritizes servers that has been selected randomly for allocation and
     * that is ready. If no such servers are found then a ready server is selected
     * at random and chosen to be the next lastly polled.
     *
     * @param ready list of servers ready to host a new instance.
     * @param roomName the instance name.
     * @return a server that is ready and preferred.
     * @throws NoServersFound when no ready servers are available.
     */
    private Server getPrioritized(ArrayList<Server> ready, String roomName) throws NoServersFound {
        String serverName = lastPolled.get(roomName);
        Server polled = servers.get(serverName);

        if (ready.size() == 0)
            throw new NoServersFound();

        if (polled != null && !polled.getFull()) {
                return polled;
        } else {
            Server server = ready.get(new Random().nextInt(ready.size()));
            lastPolled.put(roomName, server.getName());
            return server;
        }
    }


    protected void removeRoom(String name, String room) {
        Server server = servers.get(name);

        if (server != null)
            server.getRooms().remove(room);
    }

    protected void addRoom(String name, Room room) {
        Server server = servers.get(name);

        if (server != null)
            server.getRooms().put(room.getName(), room);
    }


    protected void setFull(String name, boolean isFull) {
        Server server = servers.get(name);

        if (server != null)
            server.setFull(isFull);
    }

    protected void removeServer(String server) {
        servers.remove(server);
    }

    protected void addServer(Server server) {
        servers.put(server.getName(), server);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
    }
}
