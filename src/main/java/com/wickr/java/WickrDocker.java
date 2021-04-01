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

package com.wickr.java;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.wickr.java.model.Token;
import com.wickr.java.util.ConfigUtils;
import com.wickr.java.util.JsonUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * docker container management for wickio
 *
 * @date 3/6/21.
 */
public class WickrDocker implements WickrComponent {
    public static final String DEFAULT_IMAGE = "wickr/bot-cloud:5.68.14.03";

    public static final String ENTERPRISE_IMAGE = "wickr/bot-enterprise:5.68.14.03";

    public static final String ANSIBLE_IMAGE = "wickr/ansible:1.2.0";

    public static final int DEFAULT_PORT = 4001;

    public static final String DOCKER_APP_DIR = "/opt/WickrIO";

    public static final String DOCKER_CONFIG_DIR = "/usr/local/wickr/WickrIO";

    public static class Builder {
        private String name = "wickr_io";

        private String image = DEFAULT_IMAGE;

        private File provisionDir = null;

        private File appDir = null;

        private WickrSSL ssl = null;

        private int provisionPort = DEFAULT_PORT;

        private boolean autoRemove = false;

        private boolean encryptDatabases = false;

        private DockerClientConfig config = createDefaultConfig();

        public Builder withConfig(final DockerClientConfig config) {
            this.config = config;
            return this;
        }

        public Builder withImage(final String imageName) {
            this.image = imageName;
            return this;
        }

        public Builder withName(final String containerName) {
            this.name = containerName;
            return this;
        }

        public Builder withAutoRemove(final boolean autoremove) {
            this.autoRemove = autoremove;
            return this;
        }

        public Builder withProvisionPort(final int initialPort) {
            this.provisionPort = initialPort;
            return this;
        }

        public Builder withProvisioningAt(final File baseDir) {
            if (baseDir != null) {
                this.appDir = new File(baseDir, "app");
                this.provisionDir = new File(baseDir, "config");
            } else {
                this.appDir = null;
                this.provisionDir = null;
            }
            return this;
        }

        public Builder withProvisioningAt(final File appDir, final File configDir) {
            this.appDir = appDir;
            this.provisionDir = configDir;
            return this;
        }

        public Builder withSLL(final WickrSSL ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder withDatabaseEncryption(final boolean encrypt) {
            this.encryptDatabases = encrypt;
            return this;
        }

        public WickrDocker create() {
            return new WickrDocker(
                    this.config, this.image, this.name,
                    this.provisionDir, this.appDir,
                    this.ssl, this.provisionPort,
                    this.encryptDatabases, this.autoRemove);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(WickrDocker.class);

    private final String dockerImage;

    private final DockerClientConfig config;

    private final DockerHttpClient client;

    private final String containerName;

    private final File provisionDir;

    private final Set<WickrBot> configuredBots = new CopyOnWriteArraySet<>();

    private final Set<WickrBot> unprovisionedBots = new CopyOnWriteArraySet<>();

    private final File applicationDir;

    private final boolean autoRemove;

    private final boolean encryptDatabases;

    private final WickrSSL ssl;

    private String sslCertFile;

    private String sslKeyFile;

    private String containerId;

    private final AtomicInteger currentPort;

    public WickrDocker(
            final DockerClientConfig config, final String image, final String containerName,
            final File provisionDir, final File applicationDir,
            final WickrSSL ssl, final int startPort,
            final boolean encryptdb, final boolean autoremove) {
        this.config = config;
        this.client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerImage = image;
        this.containerName = containerName;
        this.provisionDir = provisionDir;
        this.applicationDir = applicationDir;
        this.encryptDatabases = encryptdb;
        this.autoRemove = autoremove;
        this.ssl = ssl;
        this.currentPort = new AtomicInteger(startPort);
    }

    public void putBots(final Iterable<WickrBot> bots) {
        for (final WickrBot bot : bots) {
            if (bot != null) {
                this.putBot(bot);
            }
        }
    }

    public void putBot(final WickrBot bot) {
        if (null == bot) {
            throw new IllegalArgumentException("Bot cannot be null.");
        }
        if (!bot.isProvisioned() && this.isContainerRunning()) {
            try {
                // attempt to provision existing bot by copying configuration from container
                if (this.retrieveConfigurationTokensForBot(bot)) {
                    logger.debug("Found configuration tokens for bot [" + bot + "] from running container [" + this.containerId + "].");
                }
            } catch (final Exception e) {
                logger.warn("Unable to retrieve configuration for [" + bot + "] from running container.", e);
            }
        }
        if (bot.isProvisioned()) {
            this.configuredBots.add(bot);
        } else {
            this.unprovisionedBots.add(bot);
        }
    }

    public boolean isLocalHost() {
        if ("unix".equalsIgnoreCase(this.config.getDockerHost().getScheme())) {
            return true;
        } else return "localhost".equalsIgnoreCase(this.config.getDockerHost().getHost());
    }

    public void pullImage() throws Exception {
        try (final PullImageCmd cmd = this.getApi().pullImageCmd(this.dockerImage)) {
            cmd.start().awaitCompletion();
        }
    }

    public String findContainerId() {
        if (this.containerId != null && !this.containerId.isBlank()) {
            return this.containerId;
        }

        for (final Container container : this.listContainers()) {
            if (container.getId().equalsIgnoreCase(this.containerId)) {
                this.containerId = container.getId();
                return this.containerId;
            }
            for (final String name : container.getNames()) {
                if (name.equalsIgnoreCase(this.containerName) || name.equalsIgnoreCase("/" + this.containerName)) {
                    this.containerId = container.getId();
                    return this.containerId;
                }
            }
        }

        return null;
    }

    public String configureContainer() throws Exception {
        final String id = this.findContainerId();
        if (id != null && !id.isBlank()) {
            return id;
        }
        return this.createContainer();
    }

    public List<Container> listContainers() {
        try (final ListContainersCmd cmd = this.getApi().listContainersCmd()) {
            final List<Container> list = cmd.withShowSize(false).exec();
            return list != null ? List.copyOf(list) : Collections.emptyList();
        }
    }

    public InspectContainerResponse inspectContainer() {
        try (final InspectContainerCmd cmd = this.getApi().inspectContainerCmd(this.findContainerId())) {
            return cmd.withSize(false).exec();
        }
    }

    public void startContainer() throws Exception {
        this.configureContainer();
        try (final StartContainerCmd cmd = this.getApi().startContainerCmd(this.findContainerId())) {
            cmd.exec();
        }
    }

    public void stopContainer() throws Exception {
        this.stopContainer(10);
    }

    public void stopContainer(final int timeoutSeconds) throws Exception {
        try (final StopContainerCmd cmd = this.getApi().stopContainerCmd(this.findContainerId())) {
            cmd.withTimeout(timeoutSeconds).exec();
        }
    }

    public void killContainer() throws Exception {
        try (final KillContainerCmd cmd = this.getApi().killContainerCmd(this.findContainerId())) {
            cmd.exec();
        }
    }

    public void stopOrKillContainer(final int timeoutSeconds) throws Exception {
        try (final StopContainerCmd cmd = this.getApi().stopContainerCmd(this.findContainerId())) {
            cmd.withTimeout(timeoutSeconds).exec();
        } catch (final Exception e) {
            logger.warn("Unable to stop container [" + this.containerId + "].");
        }

        if (!this.isContainerRunning()) {
            return;
        }

        this.killContainer();
    }

    public boolean isDockerRunning() {
        try (final PingCmd cmd = this.getApi().pingCmd()) {
            cmd.exec();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean isContainerRunning() {
        final String existingId = this.findContainerId();
        if (null == existingId || existingId.isBlank()) {
            return false;
        }
        try (final InspectContainerCmd cmd = this.getApi().inspectContainerCmd(existingId)) {
            final InspectContainerResponse response = cmd.withSize(false).exec();
            if (null == response) {
                return false;
            } else if (response.getState() == null || response.getState().getRunning() == null) {
                return false;
            } else {
                return response.getState().getRunning();
            }
        }
    }

    public void copyFileFromContainer(final String containerPath, final File destFile) throws IOException {
        try (final CopyArchiveFromContainerCmd cmd = this.getApi().copyArchiveFromContainerCmd(this.findContainerId(), containerPath)) {
            final InputStream io = cmd.exec();
            if (null == io) {
                throw new FileNotFoundException("Unable to copy file, empty input stream returned for resource [" + containerPath + "].");
            }
            try (io) {
                try (final TarArchiveInputStream tarInput = new TarArchiveInputStream(io)) {
                    final TarArchiveEntry tarEntry = tarInput.getNextTarEntry();
                    if (Integer.MAX_VALUE < tarEntry.getSize()) {
                        throw new IllegalStateException("File size exceeds limit.");
                    }
                    final byte[] buffer = new byte[(int) tarEntry.getSize()];
                    tarInput.read(buffer);
                    FileUtils.writeByteArrayToFile(destFile, buffer);
                }
            }
        }
    }

    public void copyTarFromContainer(final String containerPath, final File destFile) throws IOException {
        try (final CopyArchiveFromContainerCmd cmd = this.getApi().copyArchiveFromContainerCmd(this.findContainerId(), containerPath)) {
            final InputStream io = cmd.exec();
            if (null == io) {
                throw new IOException("Unable to copy file, empty input stream returned for resource [" + containerPath + "].");
            }
            try (io) {
                FileUtils.copyInputStreamToFile(io, destFile);
            }
        }
    }

    public void copyResourcesToContainer(final Iterable<URL> resources, final String containerPath) throws IOException {
        final File tmpFile = bundleResourcesIntoTar(resources);
        try {
            this.copyTarToContainer(tmpFile, containerPath);
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    public void copyFilesToContainer(final Iterable<File> files, final String containerPath) throws IOException {
        final File tmpFile = bundleFilesIntoTar(files);
        try {
            this.copyTarToContainer(tmpFile, containerPath);
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    public void copyTarToContainer(final File tarFile, final String containerPath) throws IOException {
        final String id = this.findContainerId();
        if (null == id || id.isBlank()) {
            throw new IllegalStateException("Unable to find container.");
        }
        try (final InputStream in = new FileInputStream(tarFile)) {
            try (final CopyArchiveToContainerCmd cmd = this.getApi().copyArchiveToContainerCmd(id)) {
                cmd.withTarInputStream(in).withRemotePath(containerPath).exec();
            }
        }
    }

    public void copyTarToContainer(final URL tarResource, final String containerPath) throws IOException {
        final String id = this.findContainerId();
        if (null == id || id.isBlank()) {
            throw new IllegalStateException("Unable to find container.");
        }

        try (final InputStream in = tarResource.openStream()) {
            try (final CopyArchiveToContainerCmd cmd = this.getApi().copyArchiveToContainerCmd(id)) {
                cmd.withTarInputStream(in).withRemotePath(containerPath).exec();
            }
        }
    }

    @Override
    public void shutdown() throws Exception {
        this.getApi().close();
    }

    private List<Token> buildTokensForBot(final WickrBot bot) {
        final List<Token> tokens = new ArrayList<>();
        tokens.add(new Token("CLIENT_NAME", bot.getUser()));
        tokens.add(new Token("WICRIO_BOT_NAME", bot.getUser()));
        tokens.add(new Token("DATABASE_ENCRYPTION_CHOICE", this.encryptDatabases ? "yes" : "no"));
        tokens.add(new Token("BOT_PORT", Integer.toString(bot.getContainerPort())));
        tokens.add(new Token("BOT_API_KEY", bot.getApiKey()));
        tokens.add(new Token("BOT_API_AUTH_TOKEN", bot.getApiToken()));
        if (this.sslCertFile != null && !this.sslCertFile.isBlank()) {
            tokens.add(new Token("HTTPS_CHOICE", "yes"));
            tokens.add(new Token("SSL_CERT_LOCATION", this.sslCertFile));
            tokens.add(new Token("SSL_CRT_LOCATION", this.sslCertFile));
            if (this.sslKeyFile != null && !this.sslKeyFile.isBlank()) {
                tokens.add(new Token("SSL_KEY_LOCATION", this.sslKeyFile));
            }
        } else {
            tokens.add(new Token("HTTPS_CHOICE", "no"));
        }
        return tokens;
    }

    private boolean retrieveConfigurationTokensForBot(final WickrBot bot) throws IOException {
        // attempt to determine port/host for each client
        final String resource = WickrDocker.DOCKER_APP_DIR + "/clients/" + bot.getUser() + "/integration/wickrio_web_interface/processes.json";
        final File tmpFile = new File(FileUtils.getTempDirectory(), "processes.json");
        tmpFile.deleteOnExit();
        try {
            if (ConfigUtils.provisionFromConfig(bot, tmpFile)) {
                bot.withContainer(this.getHostName(), bot.getContainerPort(), bot.useSSL());
            }
            return true;
        } catch (final FileNotFoundException e) {
            logger.debug("Unable to copy configuration tokens from container for bot [" + bot + "].", e);
            return false;
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    private boolean hasCerts() {
        return this.ssl != null && this.ssl.hasCert();
    }

    private List<WickrBot> getAllBots() {
        final List<WickrBot> allBots = new ArrayList<>(this.configuredBots.size() + this.unprovisionedBots.size());
        allBots.addAll(this.configuredBots);
        allBots.addAll(this.unprovisionedBots);
        return Collections.unmodifiableList(allBots);
    }

    private void provisionBots() {
        final Set<Integer> usedPorts = new HashSet<>();
        // configure ports first
        for (final WickrBot bot : this.getAllBots()) {
            if (bot.getContainerPort() > 0) {
                usedPorts.add(bot.getContainerPort());
            }
        }
        for (final WickrBot bot : this.unprovisionedBots) {
            if (bot.getContainerPort() <= 0) {
                int port = this.currentPort.getAcquire();
                while (usedPorts.contains(port)) {
                    port = this.currentPort.incrementAndGet();
                }
                bot.withContainer(this.getHostName(), port, this.hasCerts());
                usedPorts.add(port);
            }
            if (bot.getApiKey() == null || bot.getApiToken() == null) {
                final String apiSuffix = bot.getUser().indexOf("_") > 0 ? "_api" : "-api";
                bot.withApi(bot.getUser() + apiSuffix, UUID.randomUUID().toString());
            }
        }
        // configure rest of parameters
        for (final WickrBot bot : new ArrayList<>(this.unprovisionedBots)) {
            if (!bot.isProvisioned()) {
                bot.withContainer(this.getHostName(), bot.getContainerPort(), this.hasCerts());
            }
        }
    }

    private Map<String, Object> createConfiguration() {
        final List<Map> clients = new ArrayList<>();
        for (final WickrBot bot : this.unprovisionedBots) {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("integration", "wickrio_web_interface");
            map.put("name", bot.getUser());
            if (!bot.hasPassword()) {
                throw new IllegalStateException("Wickr bot [" + bot.getUser() + "] does not have a password, which is required for provisioning.");
            }
            if (!bot.isProvisioned()) {
                throw new IllegalStateException("Wickr bot [" + bot.getUser() + "] is not fully configured.");
            }
            map.put("password", bot.getPassword());
            map.put("tokens", buildTokensForBot(bot));
            clients.add(map);
        }
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("clients", clients);
        return config;
    }

    private void setupRemoteContainer() throws IOException {
        // copy application files
        final List<URL> appResources = new ArrayList<>();
        appResources.add(WickrDocker.class.getResource("/wickrio/app/WickrIO.ini"));
        this.copyResourcesToContainer(appResources, DOCKER_APP_DIR);
        final List<URL> sslResources = new ArrayList<>();
        final URL sslCert = this.ssl != null ? this.ssl.getCertURL() : null;
        final URL sslKey = this.ssl != null ? this.ssl.getKeyURL() : null;
        if (sslCert != null) {
            this.sslCertFile = DOCKER_APP_DIR + "/" + filenameFor(sslCert);
            sslResources.add(sslCert);
        }
        if (sslKey != null) {
            this.sslKeyFile = DOCKER_APP_DIR + "/" + filenameFor(sslKey);
            sslResources.add(sslKey);
        }
        if (!sslResources.isEmpty()) {
            this.copyResourcesToContainer(sslResources, DOCKER_APP_DIR);
        }

        // until we we can get the config dir created and exposed in order to pre-copy
        // the configuration we can't provision remote containers
        if (!this.unprovisionedBots.isEmpty()) {
            throw new IOException("Unable to provision bots on remote container. Please manually configure on remote container.");
        }
    }

    private void setupLocalContainer() throws IOException {
        // setup application
        if (this.autoRemove) {
            if (this.applicationDir != null) {
                FileUtils.deleteDirectory(this.applicationDir);
            }
            if (this.provisionDir != null) {
                FileUtils.deleteDirectory(this.provisionDir);
            }
        }
        if (this.applicationDir != null) {
            if (!this.applicationDir.exists()) {
                this.applicationDir.mkdirs();
            }
            final File iniFile = new File(this.applicationDir, "WickrIO.ini");
            try (final InputStream in = WickrDocker.class.getResourceAsStream("/wickrio/app/WickrIO.ini")) {
                FileUtils.copyInputStreamToFile(in, iniFile);
            }
        }
        // setup ssl context
        final URL sslCert = this.ssl != null ? this.ssl.getCertURL() : null;
        final URL sslKey = this.ssl != null ? this.ssl.getKeyURL() : null;
        if (sslCert != null) {
            final String relativePath = "ssl/wickr_bot.cert";
            this.sslCertFile = DOCKER_APP_DIR + "/" + relativePath;
            final File certFile = new File(this.applicationDir, relativePath);
            certFile.getParentFile().mkdirs();
            try (final InputStream in = sslCert.openStream()) {
                FileUtils.copyInputStreamToFile(in, certFile);
            }
        }
        if (sslKey != null) {
            final String relativePath = "ssl/wickr_bot.key";
            this.sslKeyFile = DOCKER_APP_DIR + "/" + relativePath;
            final File keyFile = new File(this.applicationDir, relativePath);
            keyFile.getParentFile().mkdirs();
            try (final InputStream in = sslKey.openStream()) {
                FileUtils.copyInputStreamToFile(in, keyFile);
            }
        }

        if (this.unprovisionedBots.size() > 0) {
            // provision bots
            this.provisionBots();
            if (null == this.provisionDir) {
                throw new IllegalStateException("Provision configuration path not specified.");
            }
            if (!this.provisionDir.exists()) {
                this.provisionDir.mkdirs();
            }
            final File configFile = new File(this.provisionDir, "clientConfig.json");
            JsonUtils.toFile(createConfiguration(), configFile);
        }
    }

    private String createContainer() throws Exception {
        // ensure we configure all bots before setting up port mapping
        this.provisionBots();

        // if local we need to setup directories first before configuring mount point
        final boolean provisionLocally = this.unprovisionedBots.size() > 0 ||
                this.provisionDir != null ||
                this.applicationDir != null;
        if (provisionLocally) {
            this.setupLocalContainer();
        }

        try (final CreateContainerCmd cmd = this.getApi().createContainerCmd(this.dockerImage)) {
            // setup ports
            final Set<Integer> ports = new TreeSet<>();
            for (final WickrBot bot : this.getAllBots()) {
                if (bot.getContainerPort() > 0) {
                    ports.add(bot.getContainerPort());
                }
            }
            final List<PortBinding> portBindings = ports.stream().map(x -> PortBinding.parse(x + ":" + x)).collect(Collectors.toList());
            final List<ExposedPort> exposedPorts = ports.stream().map(ExposedPort::tcp).collect(Collectors.toList());
            // setup mount points
            final List<Bind> mountBindings = new ArrayList<>();
            if (this.provisionDir != null) {
                mountBindings.add(Bind.parse(this.provisionDir.getCanonicalPath() + ":" + DOCKER_CONFIG_DIR));
            }
            if (this.applicationDir != null) {
                mountBindings.add(Bind.parse(this.applicationDir.getCanonicalPath() + ":" + DOCKER_APP_DIR));
            }
            HostConfig host = HostConfig.newHostConfig()
                    .withAutoRemove(this.autoRemove)
                    .withPortBindings(portBindings)
                    .withBinds(mountBindings);
            cmd.withHostConfig(host)
                    .withExposedPorts(exposedPorts)
                    .withStdinOpen(true)
                    .withTty(true)
                    .withName(this.containerName);
            final CreateContainerResponse response = cmd.exec();
            this.containerId = response.getId();
        }

        if (!provisionLocally) {
            // configure contain after creation
            this.setupRemoteContainer();
        }

        return this.containerId;
    }

    private DockerClient getApi() {
        return DockerClientImpl.getInstance(this.config, this.client);
    }

    public String getHostName() {
        if (this.isLocalHost()) {
            return "localhost";
        } else if (!this.config.getDockerHost().getHost().isBlank()) {
            return this.config.getDockerHost().getHost();
        }
        throw new IllegalStateException("Unable to determine docker host.");
    }

    private static DockerClientConfig createDefaultConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    private static String filenameFor(final URL url) {
        if (null == url) {
            return null;
        }
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            try {
                return new File(url.toURI()).getName();
            } catch (URISyntaxException e) {
                // ignore
            }
        }
        final String path = url.getPath();
        if (path != null && !path.isBlank()) {
            return FilenameUtils.getName(path);
        }
        throw new IllegalStateException("Unable to generate file path from [" + url + "].");
    }

    private static File bundleResourcesIntoTar(final Iterable<URL> resources) throws IOException {
        final File tmpFile = File.createTempFile("tmptar-", ".tar");
        tmpFile.deleteOnExit();
        try (final OutputStream os = new FileOutputStream(tmpFile)) {
            try (final TarArchiveOutputStream tar = new TarArchiveOutputStream(os)) {
                for (final URL resource : resources) {
                    final byte[] contents = IOUtils.toByteArray(resource);
                    final String name = filenameFor(resource);
                    final TarArchiveEntry entry = new TarArchiveEntry(name);
                    entry.setSize(contents.length);
                    tar.putArchiveEntry(entry);
                    tar.write(contents);
                    tar.closeArchiveEntry();
                }
            }
        }
        return tmpFile;
    }

    private static File bundleFilesIntoTar(final Iterable<File> files) throws IOException {
        final File tmpFile = File.createTempFile("tmptar-", ".tar");
        tmpFile.deleteOnExit();
        try (final OutputStream os = new FileOutputStream(tmpFile)) {
            try (final TarArchiveOutputStream tar = new TarArchiveOutputStream(os)) {
                for (final File file : files) {
                    final byte[] contents = FileUtils.readFileToByteArray(file);
                    final String name = file.getName();
                    final TarArchiveEntry entry = new TarArchiveEntry(name);
                    entry.setSize(contents.length);
                    tar.putArchiveEntry(entry);
                    tar.write(contents);
                    tar.closeArchiveEntry();
                }
            }
        }
        return tmpFile;
    }
}
