/*
 * MIT License
 *
 * Copyright (c) 2021
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.wickr.java.util;

import com.wickr.java.WickrBot;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigUtils {

    public static List<WickrBot> createFromApplicationDir(final File appDir) throws IOException {
        final File clientsDir = new File(appDir, "clients");
        if (!clientsDir.exists() || !clientsDir.isDirectory()) {
            return Collections.emptyList();
        }

        final File[] clientDirs = clientsDir.listFiles(File::isDirectory);
        if (null == clientDirs || clientDirs.length <= 0) {
            return Collections.emptyList();
        }

        final List<WickrBot> bots = new ArrayList<>(clientDirs.length);
        for (final File clientDir : clientDirs) {
            final File integrationDir = new File(clientDir, "integration" + File.separator + "wickrio_web_interface");
            final File processConfig = new File(integrationDir, "processes.json");
            if (processConfig.exists() && processConfig.isFile()) {
                final WickrBot bot = createFromConfig(processConfig);
                if (bot != null) {
                    bots.add(bot);
                }
            }
        }
        return bots;
    }

    public static WickrBot createFromConfig(final File file) throws IOException {
        final Map<String, String> config = parseProcessConfig(file);
        if (config.isEmpty()) {
            return null;
        }
        final String botname = config.get("WICKRIO_BOT_NAME");
        if (null == botname || botname.isBlank()) {
            return null;
        }
        final WickrBot bot = WickrBot.createForExisting(botname);
        provisionFromConfig(bot, config);
        return bot;
    }

    public static boolean provisionFromConfig(final WickrBot bot, final File file) throws IOException {
        return provisionFromConfig(bot, parseProcessConfig(file));
    }

    public static boolean provisionFromConfig(final WickrBot bot, final Map<String, String> config) throws IOException {
        if (null == config || config.isEmpty()) {
            return false;
        }
        final int port = Integer.parseInt(config.get("BOT_PORT"));
        final boolean ssl = "yes".equalsIgnoreCase(config.get("HTTPS_CHOICE")) || "y".equalsIgnoreCase(config.get("HTTPS_CHOICE"));
        bot.withContainer(bot.getContainerHost(), port, ssl);
        final String apiKey = config.get("BOT_API_KEY");
        final String apiToken = config.get("BOT_API_AUTH_TOKEN");
        if (apiKey != null && apiToken != null) {
            bot.withApi(apiKey, apiToken);
        }
        return true;
    }

    private static Map<String, String> parseProcessConfig(final File file) throws IOException {
        if (null == file || !file.exists()) {
            return Collections.emptyMap();
        }
        final Map<String, Object> config = JsonUtils.toEntity(file, Map.class);
        final Object appsObj = config.get("apps");
        if (!(appsObj instanceof List)) {
            return Collections.emptyMap();
        }
        final List<Map> appList = (List) appsObj;
        for (final Map app : appList) {
            final Map<String, Object> env = safeMapFor(app, "env");
            final Map<String, Object> tokens = safeMapFor(env, "tokens");
            final Map<String, String> result = new LinkedHashMap<>();
            for (final String key : Arrays.asList(
                    "WICKRIO_BOT_NAME", "BOT_PORT",
                    "BOT_API_KEY", "BOT_API_AUTH_TOKEN", "HTTPS_CHOICE")) {
                result.put(key, safeTokenValue(tokens, key));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return Collections.emptyMap();
    }

    public static Map<String, Object> safeMapFor(final Map map, final String key) {
        if (null == map || map.isEmpty()) {
            return Collections.emptyMap();
        }
        final Object obj = map.get(key);
        if (!(obj instanceof Map)) {
            return Collections.emptyMap();
        }
        return (Map) obj;
    }

    public static String safeTokenValue(final Map<String, Object> tokens, final String key) {
        final Map<String, Object> keyvalue = safeMapFor(tokens, key);
        final Object value = keyvalue.get("value");
        if (null == value) {
            return null;
        }
        return value.toString();
    }

    private ConfigUtils() {

    }
}
