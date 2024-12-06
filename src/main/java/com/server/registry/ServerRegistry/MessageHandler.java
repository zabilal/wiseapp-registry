package com.server.registry.ServerRegistry;

import com.server.registry.ServerRegistry.Exception.NoServersFound;
import com.server.registry.ServerRegistry.Model.Server;
import com.server.registry.ServerRegistry.Protocol.Index;
import com.server.registry.ServerRegistry.Protocol.Lookup;
import com.server.registry.ServerRegistry.Protocol.Serializer;

/**
 * Created by Zakariyya Raji on 04/01/2018.
 * <p>
 * Handles messages from a client.
 */
enum MessageHandler {
    HandleLookup {
        @Override
        public void handle(String socket, String data, RegistryService registry) {
            Lookup lookup = (Lookup) Serializer.unpack(data, Lookup.class);
            try {
                Server server = registry.getReadyServer(lookup.getRoom());
                registry.sendBus(socket, new Index(server));
            } catch (NoServersFound e) {
                registry.sendBus(socket, new Index().setFull(true));
            }
        }
    };

    public abstract void handle(String socket, String data, RegistryService registry);
}
