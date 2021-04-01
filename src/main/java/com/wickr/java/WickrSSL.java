package com.wickr.java;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.wickr.java.util.CertUtils;
import com.wickr.java.util.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * ssl configuration for wickr-io and clients
 *
 * @date 3/20/21.
 */
public class WickrSSL {

    public static WickrSSL fromSystemProperties() {
        String certPath = findProperty("wickr.ssl.certPath");
        String keyPath = findProperty("wickr.ssl.keyPath");
        String keystorePath = findProperty("wickr.ssl.keyStore", "wickr.ssl.keyStorePath", "javax.net.ssl.keyStore");
        String keystorePassword = findProperty("wickr.ssl.keyStorePassword", "javax.net.ssl.keyStorePassword");
        String keystoreType = findProperty("wickr.ssl.keyStoreType", "javax.net.ssl.keyStoreType");
        return new WickrSSL(certPath, keyPath, keystorePath, keystorePassword, keystoreType);
    }

    public static WickrSSL fromCert(final URL cert, final URL key) {
        return fromCert(cert, key, false);
    }

    public static WickrSSL fromCert(final File cert, final File key) {
        return fromCert(cert, key, false);
    }

    public static WickrSSL fromCert(final File cert, final File key, final boolean convertKeystore) {
        try {
            return fromCert(cert.toURI().toURL(), key.toURI().toURL(), false);
        } catch (final MalformedURLException e) {
            throw new IllegalStateException("Unable to convert file to URL.", e);
        }
    }

    public static WickrSSL fromCert(final URL cert, final URL key, final boolean convertKeystore) {
        final String certPath = cert.toString();
        final String keyPath = key != null ? key.toString() : null;
        if (convertKeystore) {
            try {
                if ("file".equalsIgnoreCase(cert.getProtocol())) {
                    final File keystoreFile = createTemporaryKeystoreFrom(FileUtils.toFile(cert), FileUtils.toFile(key));
                    final String keystorePath = keystoreFile.getCanonicalFile().toURI().toString();
                    return new WickrSSL(certPath, keyPath, keystorePath, "", "PKCS12");
                } else {
                    final File keystoreFile = createTemporaryKeystoreFrom(cert, key);
                    final String keystorePath = keystoreFile.getCanonicalFile().toURI().toString();
                    return new WickrSSL(certPath, keyPath, keystorePath, "", "PKCS12");
                }
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to convert cert [" + cert + "] to pkcs12 format.", e);
            }
        } else {
            return new WickrSSL(certPath, keyPath, null, null, null);
        }
    }

    public static WickrSSL fromJson(final File file) throws IOException {
        return JsonUtils.toEntity(file, WickrSSL.class);
    }

    public static WickrSSL fromJson(final URL resource) throws IOException {
        try (final InputStream input = resource.openStream()) {
            return JsonUtils.toEntity(input, WickrSSL.class);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(WickrSSL.class);

    @JsonProperty("cert_path")
    private String certPath;

    @JsonProperty("key_path")
    private String keyPath;

    @JsonProperty("keystore_path")
    private String keystorePath;

    @JsonIgnore
    private File tmpKeystore;

    @JsonProperty("keystore_password")
    private String keystorePassword;

    @JsonProperty("keystore_type")
    private String keystoreType;

    public WickrSSL(final String cert, final String key, final String keystore, final String keystorePwd,
                    final String keystoreType) {
        this.certPath = cert;
        this.keyPath = key;
        this.keystorePath = keystore;
        this.keystorePassword = keystorePwd;
        this.keystoreType = keystoreType;
    }

    @Deprecated
    public WickrSSL() {

    }

    public boolean isEmpty() {
        return !this.hasCert() && !this.hasKeystore();
    }

    public boolean hasCert() {
        return this.certPath != null && !this.certPath.isBlank();
    }

    public URL getCertURL() {
        return safeUrl(this.certPath);
    }

    public URL getKeyURL() {
        return safeUrl(this.keyPath);
    }

    public boolean hasKeystore() {
        return this.keystorePath != null && !this.keystorePath.isBlank();
    }

    public String getKeystorePath() {
        return this.keystorePath;
    }

    public URL getKeystoreURL() {
        return safeUrl(this.keystorePath);
    }

    public File getKeystoreFile() {
        if (!this.hasKeystore()) {
            return null;
        }
        final URL resourceURL = this.getKeystoreURL();
        if (null == resourceURL) {
            return null;
        }
        final File resourceFile = FileUtils.toFile(resourceURL);
        if (resourceFile != null) {
            return resourceFile;
        }
        if (tmpKeystore != null) {
            return tmpKeystore;
        }
        try {
            tmpKeystore = File.createTempFile("wickr-keystore-", "." + this.keystoreType);
            tmpKeystore.deleteOnExit();
            FileUtils.copyURLToFile(resourceURL, tmpKeystore);
            return tmpKeystore;
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to copy keystore to temporary file.", e);
        }
    }

    public char[] getKeystorePassword() {
        if (null == this.keystorePassword || this.keystorePassword.isBlank()) {
            return new char[0];
        } else {
            return this.keystorePassword.toCharArray();
        }
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public String toJson() throws JsonProcessingException {
        return JsonUtils.fromEntity(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WickrSSL wickrSSL = (WickrSSL) o;
        return Objects.equals(certPath, wickrSSL.certPath) && Objects.equals(keyPath, wickrSSL.keyPath) && Objects.equals(keystorePath, wickrSSL.keystorePath) && Objects.equals(keystorePassword, wickrSSL.keystorePassword) && Objects.equals(keystoreType, wickrSSL.keystoreType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certPath, keyPath, keystorePath, keystorePassword, keystoreType);
    }

    private static File createTemporaryKeystoreFrom(final File certFile, final File keyFile) throws
            IOException, InterruptedException {
        final String basename = FilenameUtils.getBaseName(certFile.getName());
        final File destFile = new File(FileUtils.getTempDirectory(), basename + ".p12");
        try {
            CertUtils.convertPEMToPKCS12(certFile, keyFile, "", destFile, "");
        } finally {
            if (destFile.exists()) {
                destFile.deleteOnExit();
            }
        }
        return destFile;
    }

    private static File createTemporaryKeystoreFrom(final URL certURL, final URL keyURL) throws
            IOException, InterruptedException {
        final File tmpCert = File.createTempFile("wickr-cert-", ".cert");
        tmpCert.deleteOnExit();
        final File tmpKey = File.createTempFile("wickr-key-", ".key");
        tmpKey.deleteOnExit();
        try {
            FileUtils.copyURLToFile(certURL, tmpCert);
            FileUtils.copyURLToFile(keyURL, tmpKey);
            return createTemporaryKeystoreFrom(tmpCert, tmpKey);
        } finally {
            FileUtils.deleteQuietly(tmpCert);
            FileUtils.deleteQuietly(tmpKey);
        }
    }

    private static URL safeUrl(final String path) {
        if (null == path || path.isBlank()) {
            return null;
        }
        try {
            return new URL(path);
        } catch (final MalformedURLException e) {
            logger.trace("Unable to convert path [" + path + "] to URL.", e);
        }
        File file = new File(path);
        try {
            file = new File(path).getCanonicalFile();
        } catch (IOException e) {
            logger.trace("Unable to obtain canonical path for [" + path + "].", e);
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            logger.trace("Unable to convert path [" + path + "] to URL.", e);
        }
        throw new IllegalStateException("Unable to convert path [" + path + "] to URL.");
    }

    private static String findProperty(final String... possibleNames) {
        if (null == possibleNames || possibleNames.length <= 0) {
            return null;
        }
        for (final String name : possibleNames) {
            final String value = System.getProperty(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
