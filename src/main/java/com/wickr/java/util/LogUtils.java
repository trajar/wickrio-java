package com.wickr.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;

public class LogUtils {
    public static void configureJDK14() {
        final URL resource = LogUtils.class.getResource("/wickrio/logging/jdk14-console.properties");
        try (final InputStream in = resource.openStream()) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to configure jdk14 logging.", e);
        }
    }

    private LogUtils() {

    }
}
