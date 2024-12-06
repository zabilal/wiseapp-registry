package com.server.registry.ServerRegistry;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * Created by Zakariyya Raji on 2015-12-26.
 *
 * Uploads messages to the logging service.
 */
public class Logger implements Verticle {
    private Vertx vertx;

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        this.vertx = vertx;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.createHttpClient().websocket(Configuration.LOGGER_PORT, "localhost", "/", event -> {

            vertx.eventBus().consumer(Configuration.BUS_LOGGING, data -> {
               vertx.eventBus().send(event.textHandlerID(), data.body().toString());
            });

        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

    }
}
