package com.wickr.java.http;

import org.apache.hc.client5.http.auth.*;
import org.apache.hc.client5.http.utils.ByteArrayBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

/**
 * basic authentication for wickr api
 *
 * @date 3/14/21.
 */
public class WickrAuthentication implements AuthScheme {
    private final Charset charset;

    private ByteArrayBuilder buffer;

    private boolean complete;

    private final Map<String, String> paramMap = new HashMap<>();

    private final String token;

    public WickrAuthentication(final String apiToken) {
        this(apiToken, StandardCharsets.US_ASCII);
    }

    public WickrAuthentication(final String apiToken, final Charset charset) {
        this.token = apiToken;
        this.charset = charset != null ? charset : StandardCharsets.US_ASCII;
        this.complete = false;
    }

    @Override
    public String getName() {
        return StandardAuthScheme.BASIC;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public String getRealm() {
        return this.paramMap.get("realm");
    }

    @Override
    public void processChallenge(
            final AuthChallenge authChallenge,
            final HttpContext context) {
        this.paramMap.clear();
        final List<NameValuePair> params = authChallenge.getParams();
        if (params != null) {
            for (final NameValuePair param : params) {
                this.paramMap.put(param.getName().toLowerCase(Locale.ROOT), param.getValue());
            }
        }
        this.complete = true;
    }

    @Override
    public boolean isChallengeComplete() {
        return this.complete;
    }

    @Override
    public boolean isResponseReady(
            final HttpHost host,
            final CredentialsProvider credentialsProvider,
            final HttpContext context) {
        return false;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public String generateAuthResponse(
            final HttpHost host,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {
        if (this.buffer == null) {
            this.buffer = new ByteArrayBuilder(256).charset(this.charset);
        } else {
            this.buffer.reset();
        }
        this.buffer.append(this.token);
        final byte[] encoded = Base64.getEncoder().encode(this.buffer.toByteArray());
        this.buffer.reset();
        return StandardAuthScheme.BASIC + " " + new String(encoded, 0, encoded.length, StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        return getName() + ":" + this.token;
    }
}
