package com.server.registry.ServerRegistry;

/**
 * Created by Zakariyya Raji on Zakariyya Raji 04/01/2018.
 * <p>
 * Contains configuration params.
 */
public class Configuration {
    public final static Integer CLIENT_PORT = 6090;
    public final static Integer CONNECTOR_PORT = 7040;
    public static final Integer LOGGER_PORT = 5454;
    public static final String BUS_LOGGING = "logger.upstream";
    public static final long LOG_INTERVAL = 1000;
    public static final String REGISTER_NAME = "registry";
}
