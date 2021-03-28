package com.wickr.java;

import com.wickr.java.http.WickrBotServer;
import com.wickr.java.impl.ArrayBlockingWickrEventQueue;
import com.wickr.java.impl.WickrEventPublishingWorker;
import com.wickr.java.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * a rest client for the wickr-io web application
 *
 * @date 2/27/21.
 */
public class WickrIO {

    public static class Builder {
        private final List<WickrBot> bots = new ArrayList<>();

        private final List<WickrListener> listeners = new ArrayList<>();

        private int serverPort = WickrBotServer.DEFAULT_LISTEN_PORT;

        private WickrSSL serverSSL = null;

        private boolean useServer = true;

        private int messageQueryFrequencyMsec = -1;

        private WickrEventQueue eventQueue = new ArrayBlockingWickrEventQueue();

        private WickrDocker docker = null;

        public Builder withDocker(final WickrDocker dockerApi) {
            this.docker = dockerApi;
            return this;
        }

        public Builder withBot(final WickrBot bot) {
            return this.withBots(Collections.singleton(bot));
        }

        public Builder withBots(final WickrBot... bots) {
            return withBots(Arrays.asList(bots));
        }

        public Builder withBots(final Iterable<WickrBot> bots) {
            for (final WickrBot bot : bots) {
                if (bot != null) {
                    this.bots.add(bot);
                }
            }
            return this;
        }

        public Builder withServer(final int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder withServer(final int serverPort, final WickrSSL ssl) {
            this.serverPort = serverPort;
            this.serverSSL = ssl;
            return this;
        }

        public Builder withoutServer() {
            this.useServer = false;
            return this;
        }

        public Builder withEventCallback() {
            this.messageQueryFrequencyMsec = -1;
            return this;
        }

        public Builder withEventListener(final int queryFrequencyMsec) {
            this.messageQueryFrequencyMsec = queryFrequencyMsec;
            return this;
        }

        public Builder withListener(final WickrListener l) {
            if (l != null) {
                this.listeners.add(l);
            }
            return this;
        }

        public Builder withEventQueue(final WickrEventQueue queue) {
            this.eventQueue = queue;
            return this;
        }

        public WickrIO start() throws Exception {
            return this.start(-1);
        }

        public WickrIO start(final int waitTimeoutMsec) throws Exception {
            if (this.serverSSL != null) {
                // setup http
                HttpUtils.setup(this.serverSSL);
            }

            if (this.docker != null && this.docker.isDockerRunning()) {
                // setup docker container management
                for (final WickrBot bot : this.bots) {
                    this.docker.putBot(bot);
                }
                this.docker.configureContainer();
                if (!this.docker.isContainerRunning()) {
                    this.docker.startContainer();
                }
            }

            // wait until the docker container is configured, running, and bots are available
            for (final WickrBot bot : this.bots) {
                bot.waitUntilAvailable(waitTimeoutMsec);
            }

            // setup wickio base
            final WickrIO wickrio = new WickrIO(this.bots, this.docker, this.eventQueue);
            if (this.docker != null) {
                wickrio.addComponent(this.docker);
            }
            wickrio.addComponent(this.eventQueue);
            for (final WickrListener listener : this.listeners) {
                wickrio.addListener(listener);
            }

            // create event process worker
            final WickrListener eventWorker;
            final boolean listenForCallback;
            if (this.messageQueryFrequencyMsec > 0) {
                eventWorker = WickrEventPublishingWorker.createAsThread(this.bots, this.eventQueue, this.messageQueryFrequencyMsec);
                ((Thread) eventWorker).start();
                listenForCallback = false;
            } else {
                eventWorker = WickrEventPublishingWorker.createAsApi(this.bots, this.eventQueue);
                listenForCallback = true;
            }
            wickrio.addComponent((WickrComponent) eventWorker);

            if (this.useServer) {
                // start management server
                final WickrBotServer server = new WickrBotServer(this.docker, this.serverPort, this.serverSSL, eventWorker, listenForCallback);
                server.start();
                wickrio.addComponent(server);
                for (final WickrBot bot : this.bots) {
                    server.getApplication().putBot(bot);
                }
            }

            registerShutdownHookFor(wickrio);
            return wickrio;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(WickrIO.class);

    private final WickrDocker docker;

    private final Thread eventThread;

    private final WickrEventQueue eventQueue;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Set<WickrComponent> components = new CopyOnWriteArraySet<>();

    private final Set<WickrListener> listeners = new CopyOnWriteArraySet<>();

    private final Map<String, WickrBot> bots;

    public WickrIO(final Collection<WickrBot> bots, final WickrDocker docker, final WickrEventQueue queue) {
        // save bots to lookup
        this.bots = new HashMap<>(bots.size());
        for (final WickrBot bot : bots) {
            this.bots.put(bot.getUser(), bot);
        }
        this.docker = docker;
        this.eventQueue = queue;
        // setup default components
        if (docker != null) {
            this.components.add(docker);
        }
        if (queue != null) {
            this.components.add(queue);
        }
        // spawn event processing thread
        this.eventThread = new Thread(new EventProcessingWorker());
        this.eventThread.setDaemon(true);
        this.eventThread.setName("WickIO-EventThread");
        this.eventThread.start();
    }

    public List<WickrBot> getBots() {
        return List.copyOf(this.bots.values());
    }

    public WickrBot getBot(final String user) {
        return this.ensureBot(user);
    }

    private boolean addComponent(final WickrComponent c) {
        if (null == c) {
            return false;
        } else {
            return this.components.add(c);
        }
    }

    private boolean addListener(final WickrListener l) {
        if (null == l) {
            return false;
        } else {
            return this.listeners.add(l);
        }
    }

    public void shutdown() throws Exception {
        this.closed.getAndSet(true);
        if (this.eventThread.isAlive()) {
            this.eventThread.interrupt();
            this.eventThread.join();
        }
        for (final WickrComponent component : this.components) {
            component.shutdown();
        }
        HttpUtils.shutdown();
    }

    public boolean isClosed() {
        return this.closed.get();
    }

    public boolean isAvailable() {
        if (null == this.docker) {
            throw new IllegalStateException("Docker not configured, unable to determine if container is running.");
        }
        try {
            return this.docker.isDockerRunning() && this.docker.isContainerRunning();
        } catch (final Exception e) {
            // ignore
            return false;
        }
    }

    private Collection<WickrListener> getListeners() {
        return List.copyOf(this.listeners);
    }

    private boolean processEvent(final WickrEvent event) {
        if (null == event) {
            return false;
        }
        int notified = 0;
        for (final WickrListener listener : this.getListeners()) {
            listener.messageReceived(event.getBot(), event.getMessage());
            notified++;
        }
        return notified > 0;
    }

    private WickrBot ensureBot(final String user) {
        if (null == user || user.isBlank()) {
            throw new IllegalArgumentException("Bot username is empty.");
        }
        final WickrBot bot = this.bots.get(user);
        if (null == bot) {
            throw new IllegalArgumentException("Unknown bot username [" + user + "].");
        }
        return bot;
    }

    private static void registerShutdownHookFor(final WickrIO wickrio) {
        final Thread hook = new Thread(() -> {
            try {
                wickrio.shutdown();
            } catch (final Exception e) {
                logger.warn("Unable to execute shutdown hook for wickr-io library.", e);
            }
        });
        hook.setName("WickIO-ShutdownHook");
        hook.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private class EventProcessingWorker implements Runnable {
        @Override
        public void run() {
            try {
                processEventsUntilClosed();
            } catch (final InterruptedException e) {
                logger.debug("Event processing thread interrupted, exiting.");
            } catch (final Exception e) {
                logger.warn("Unexpected error in event processing thread.", e);
            }
        }

        private void processEventsUntilClosed() throws InterruptedException {
            boolean firstRun = true;
            while (firstRun || !WickrIO.this.isClosed()) {
                final WickrEvent event = WickrIO.this.eventQueue.remove();
                if (event != null) {
                    processEvent(event);
                }
                firstRun = false;
            }
        }
    }
}
