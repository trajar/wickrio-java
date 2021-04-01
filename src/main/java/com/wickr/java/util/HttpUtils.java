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

import com.wickr.java.WickrSSL;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.concurrent.CancellableDependency;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * common set of http utilities
 *
 * @date 3/14/21.
 */
public class HttpUtils {

    public static <T> String postJson(final URI target, final T entity, final AuthScheme authentication) throws IOException, HttpException {
        final String json = JsonUtils.fromEntity(entity);
        return postJson(target, json, authentication);
    }

    public static String postJson(final URI target, final String json, final AuthScheme authentication) throws IOException, HttpException {
        final HttpPost post = new HttpPost(target);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON, false));
        return readResponseAndCheckStatus(client().execute(post, contextFor(target, authentication)));
    }

    public static String post(final URI target, final AuthScheme authentication) throws IOException, HttpException {
        final HttpPost post = new HttpPost(target);
        return readResponseAndCheckStatus(client().execute(post, contextFor(target, authentication)));
    }

    public static String postForm(final URI target, final Map<String, Object> formData, final AuthScheme authentication) throws IOException, HttpException {
        final HttpPost post = new HttpPost(target);
        MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
        for (final Map.Entry<String, Object> entry : formData.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof File) {
                multipartBuilder.addBinaryBody(name, (File) value);
            } else if (value instanceof URL) {
                multipartBuilder.addBinaryBody(name, IOUtils.toByteArray((URL) value));
            } else if (value instanceof URI) {
                multipartBuilder.addBinaryBody(name, IOUtils.toByteArray((URI) value));
            } else if (value instanceof CharSequence) {
                multipartBuilder.addTextBody(name, value.toString(), ContentType.TEXT_PLAIN);
            } else {
                throw new IllegalStateException("Unexpected form type [" + value.getClass() + "], value [" + value + "].");
            }
        }
        post.setEntity(multipartBuilder.build());
        return readResponseAndCheckStatus(client().execute(post, contextFor(target, authentication)));
    }

    public static <T> T getJson(final URI target, final Class<T> clazz, final AuthScheme authentication) throws IOException, HttpException {
        final String json = get(target, authentication);
        if (null == json || json.isBlank()) {
            return null;
        }
        return JsonUtils.toEntity(json, clazz);
    }

    public static String get(final URI target, final AuthScheme authentication) throws IOException, HttpException {
        final HttpGet get = new HttpGet(target);
        return readResponseAndCheckStatus(client().execute(get, contextFor(target, authentication)));
    }

    public static String delete(final URI target, final AuthScheme authentication) throws IOException, HttpException {
        final HttpDelete get = new HttpDelete(target);
        return readResponseAndCheckStatus(client().execute(get, contextFor(target, authentication)));
    }

    private static HttpClientContext contextFor(final URI target, final AuthScheme authentication) {
        // setup basic authentication for host
        final HttpHost host = new HttpHost(target.getScheme(), target.getHost(), target.getPort());
        final HttpClientContext localContext = HttpClientContext.create();
        if (authentication != null) {
            localContext.resetAuthExchange(host, authentication);
        }
        return localContext;
    }

    private static String readResponseAndCheckStatus(final CloseableHttpResponse response) throws IOException, ParseException {
        // ensure we read the entire response and close the io resource
        try (response) {
            final String responseEntity = EntityUtils.toString(response.getEntity());
            checkStatus(response);
            return responseEntity;
        }
    }

    private static void checkStatus(final HttpResponse response) throws IOException {
        if (response.getCode() == HttpStatus.SC_OK ||
                response.getCode() == HttpStatus.SC_CREATED ||
                response.getCode() == HttpStatus.SC_ACCEPTED) {
            // successful operation
            return;
        }

        // unexpected status code response
        throw new IOException("Unable to process request - status [" + response.getCode() + "] reason [" + response.getReasonPhrase() + "].");
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static HttpClientConnectionManager connectionManager = null;

    private static CloseableHttpClient httpClient = null;

    public static void shutdown() throws IOException {
        if (httpClient != null) {
            httpClient.close(CloseMode.GRACEFUL);
            httpClient = null;
        }
        if (connectionManager != null) {
            connectionManager.close(CloseMode.GRACEFUL);
            connectionManager = null;
        }
    }

    public static void setupWithDefaults() throws IOException {
        setup(WickrSSL.fromSystemProperties());
    }

    public static void setup(final WickrSSL ssl) throws IOException {
        shutdown();
        if (null == connectionManager) {
            connectionManager = createManager(ssl);
        }
        if (null == httpClient) {
            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setUserAgent("wickrio-java")
                    .setDefaultHeaders(Arrays.asList(
                            new BasicHeader("Cache-Control", "no-cache, must-revalidate"),
                            new BasicHeader("Pragma", "no-cache")))
                    .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                    .setRetryStrategy(new RetryingHttpRequestRetryStrategy(5, TimeValue.ofSeconds(2L)))
                    .build();
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        }
    }

    private static ConnectionSocketFactory createSocketFactoryForSSL(final WickrSSL ssl) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        final SSLContext sslContext;
        if (ssl != null && ssl.hasKeystore()) {
            sslContext = SSLContexts.custom()
                    .setKeyStoreType(ssl.getKeystoreType())
                    .loadTrustMaterial(ssl.getKeystoreURL(), ssl.getKeystorePassword(), acceptingTrustStrategy).build();
        } else {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(acceptingTrustStrategy).build();
        }
        return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    }

    private static CloseableHttpClient client() {
        if (null == httpClient) {
            try {
                setupWithDefaults();
            } catch (final IOException e) {
                throw new IllegalStateException("Unable to create default http client.", e);
            }
        }
        return httpClient;
    }

    private static HttpClientConnectionManager createManager(final WickrSSL ssl) throws IOException {
        ConnectionSocketFactory sslFactory = null;
        try {
            sslFactory = createSocketFactoryForSSL(ssl);
        } catch (CertificateException e) {
            logger.warn("Unable to create SSL socket factory.", e);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Unable to create SSL socket factory.", e);
        } catch (KeyStoreException e) {
            logger.warn("Unable to create SSL socket factory.", e);
        } catch (KeyManagementException e) {
            logger.warn("Unable to create SSL socket factory.", e);
        }
        final RegistryBuilder builder = RegistryBuilder.create()
                .register("http", new PlainConnectionSocketFactory());
        if (sslFactory != null) {
            builder.register("https", sslFactory);
        }
        final PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager(builder.build());
        mgr.setMaxTotal(Integer.parseInt(System.getProperty("wickr.http.totalPoolSize", "12")));
        mgr.setDefaultMaxPerRoute(Integer.parseInt(System.getProperty("wickr.http.maxPerRoute", "4")));
        return mgr;
    }

    private static class RetryingHttpRequestRetryStrategy implements HttpRequestRetryStrategy {
        private final int maxRetries;

        private final TimeValue defaultRetryInterval;

        private final Set<Class<? extends IOException>> nonRetriableIOExceptionClasses;

        private final Set<Class<? extends IOException>> retriableIOExceptionClasses;

        private final Set<Integer> retriableCodes;

        public RetryingHttpRequestRetryStrategy(
                final int maxRetries,
                final TimeValue defaultRetryInterval) {
            this.maxRetries = maxRetries;
            this.defaultRetryInterval = defaultRetryInterval;
            this.nonRetriableIOExceptionClasses = new HashSet<>(Arrays.asList(
                    InterruptedIOException.class,
                    UnknownHostException.class,
                    ConnectException.class,
                    ConnectionClosedException.class,
                    SSLException.class));
            this.retriableIOExceptionClasses = new HashSet<>(Arrays.asList(
                    NoHttpResponseException.class));
            this.retriableCodes = new HashSet<>(
                    Arrays.asList(
                            HttpStatus.SC_TOO_MANY_REQUESTS,
                            HttpStatus.SC_SERVICE_UNAVAILABLE));
        }

        @Override
        public boolean retryRequest(
                final HttpRequest request,
                final IOException exception,
                final int execCount,
                final HttpContext context) {
            if (execCount > this.maxRetries) {
                return false;
            }
            if (request instanceof CancellableDependency && ((CancellableDependency) request).isCancelled()) {
                return false;
            }
            if (ExceptionUtils.isException(exception, this.nonRetriableIOExceptionClasses)) {
                return false;
            }
            if (ExceptionUtils.isException(exception, this.retriableIOExceptionClasses)) {
                return true;
            }
            return Method.isIdempotent(request.getMethod());
        }

        @Override
        public boolean retryRequest(
                final HttpResponse response,
                final int execCount,
                final HttpContext context) {
            return execCount <= this.maxRetries && retriableCodes.contains(response.getCode());
        }

        @Override
        public TimeValue getRetryInterval(
                final HttpResponse response,
                final int execCount,
                final HttpContext context) {
            final Header header = response.getFirstHeader(HttpHeaders.RETRY_AFTER);
            TimeValue retryAfter = null;
            if (header != null) {
                final String value = header.getValue();
                try {
                    retryAfter = TimeValue.ofSeconds(Long.parseLong(value));
                } catch (final NumberFormatException ignore) {
                    final Date retryAfterDate = DateUtils.parseDate(value);
                    if (retryAfterDate != null) {
                        retryAfter = TimeValue.ofMilliseconds(retryAfterDate.getTime() - System.currentTimeMillis());
                    }
                }
                if (TimeValue.isPositive(retryAfter)) {
                    return retryAfter;
                }
            }
            return this.defaultRetryInterval;
        }
    }

    private HttpUtils() {

    }
}
