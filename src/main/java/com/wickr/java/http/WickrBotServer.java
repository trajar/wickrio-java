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

package com.wickr.java.http;

import com.wickr.java.WickrComponent;
import com.wickr.java.WickrDocker;
import com.wickr.java.WickrListener;
import com.wickr.java.WickrSSL;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * a simple callback and proxy server for wickrio container api
 *
 * @date 2/28/21.
 */
public class WickrBotServer implements WickrComponent {

    public static final int DEFAULT_LISTEN_PORT = 8814;

    private final Component component;

    private final WickrDocker docker;

    private final WickrSSL ssl;

    private final int listenPort;

    private final WickrBotApplication application;

    public WickrBotServer(final WickrListener events) {
        this(new WickrDocker.Builder().create(), DEFAULT_LISTEN_PORT, null, events, true);
    }

    public WickrBotServer(final WickrDocker docker, final int port, final WickrSSL ssl, final WickrListener events, final boolean registerCallbackEndpoint) {
        this.docker = docker;
        this.ssl = ssl;
        this.listenPort = port;
        final String hostname;
        if (docker != null && docker.isDockerRunning() && docker.isLocalHost()) {
            hostname = "host.docker.internal";
        } else {
            try {
                hostname = Inet4Address.getLocalHost().getHostAddress();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("Unable to determine localhost address for message callback URL.", e);
            }
        }
        final boolean useSSL = this.ssl != null && this.ssl.hasKeystore();
        final String protocol = useSSL ? "https" : "http";
        this.application = new WickrBotApplication(protocol + "://" + hostname + ":" + this.listenPort, events, registerCallbackEndpoint);
        this.component = new Component();
        this.component.setName("WickIO-Restlet");
        final Server server;
        if (useSSL) {
            server = this.component.getServers().add(Protocol.HTTPS, this.listenPort);
            final File keystoreFile = this.ssl.getKeystoreFile();
            final Series<Parameter> parameters = server.getContext().getParameters();
            final char[] pwdChars = this.ssl.getKeystorePassword();
            final String pwd = pwdChars != null && pwdChars.length > 0 ? new String(pwdChars) : "";
            parameters.add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
            parameters.add("keyStorePath", keystoreFile != null ? keystoreFile.getPath() : null);
            parameters.add("keyStorePassword", pwd);
            parameters.add("keyStoreType", this.ssl.getKeystoreType());
        } else {
            server = this.component.getServers().add(Protocol.HTTP, this.listenPort);
        }
        server.setName("WickIO-Server");
        this.component.getDefaultHost().attach(this.application);
    }

    public WickrBotApplication getApplication() {
        return this.application;
    }

    public synchronized void start() throws Exception {
        this.component.start();
    }

    public synchronized boolean isRunning() {
        return this.component.isStarted() && !this.component.isStopped();
    }

    @Override
    public void shutdown() throws Exception {
        this.component.stop();
    }
}
