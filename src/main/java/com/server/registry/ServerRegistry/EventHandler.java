package com.server.registry.ServerRegistry;

import com.server.registry.ServerRegistry.Model.Room;
import com.server.registry.ServerRegistry.Model.Server;
import com.server.registry.ServerRegistry.Protocol.RoomEvent;
import com.server.registry.ServerRegistry.Protocol.Serializer;
import com.server.registry.ServerRegistry.Protocol.ServerEvent;

/**
 * Created by Zakariyya Raji on 2015-12-18.
 * <p>
 * Handles events from the backend regarding changes
 * in the list of servers that are subscribed to a room.
 * <p>
 * Handles events that indicate server state as well.
 */
enum EventHandler {

    HandleRoom {
        @Override
        public void handle(String data, RegistryService registry) {
            RoomEvent room = (RoomEvent) Serializer.unpack(data, RoomEvent.class);

            if (room.getStatus() != null)
                switch (room.getStatus()) {
                    case POPULATED:
                        registry.addRoom(room.getServer(), new Room(room.getRoom()));
                        break;
                    case DEPLETED:
                        registry.removeRoom(room.getServer(), room.getRoom());
                        break;
                }
            registry.sendBus(Configuration.BUS_LOGGING, data);
        }
    },

    HandleServer {
        @Override
        public void handle(String data, RegistryService registry) {
            ServerEvent server = (ServerEvent) Serializer.unpack(data, ServerEvent.class);

            if (server.getStatus() != null)
                switch (server.getStatus()) {
                    case UP:
                        registry.addServer(new Server(server.getName(), server.getIp(), server.getPort()));
                        break;
                    case DOWN:
                        registry.removeServer(server.getName());
                        break;
                    case FULL:
                        registry.setFull(server.getName(), true);
                        break;
                    case READY:
                        registry.setFull(server.getName(), false);
                        break;
                }
        }
    };

    public abstract void handle(String data, RegistryService registry);
}
