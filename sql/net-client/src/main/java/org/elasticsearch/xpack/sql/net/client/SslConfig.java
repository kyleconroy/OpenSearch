/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.net.client;

import org.elasticsearch.xpack.sql.net.client.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

class SslConfig {

    private static final String SSL = "ssl";
    private static final String SSL_DEFAULT = "false";

    private static final String SSL_PROTOCOL = "ssl.protocol";
    private static final String SSL_PROTOCOL_DEFAULT = "TLS"; // SSL alternative

    private static final String SSL_KEYSTORE_LOCATION = "ssl.keystore.location";
    private static final String SSL_KEYSTORE_LOCATION_DEFAULT = "";

    private static final String SSL_KEYSTORE_PASS = "ssl.keystore.pass";
    private static final String SSL_KEYSTORE_PASS_DEFAULT = "";

    private static final String SSL_KEYSTORE_TYPE = "ssl.keystore.type";
    private static final String SSL_KEYSTORE_TYPE_DEFAULT = "JKS"; // PCKS12

    private static final String SSL_TRUSTSTORE_LOCATION = "ssl.truststore.location";
    private static final String SSL_TRUSTSTORE_LOCATION_DEFAULT = "";

    private static final String SSL_TRUSTSTORE_PASS = "ssl.truststore.pass";
    private static final String SSL_TRUSTSTORE_PASS_DEFAULT = "";

    private static final String SSL_TRUSTSTORE_TYPE = "ssl.truststore.type";
    private static final String SSL_TRUSTSTORE_TYPE_DEFAULT = "JKS";
    
    private final boolean enabled;
    private final String protocol, keystoreLocation, keystorePass, keystoreType;
    private final String truststoreLocation, truststorePass, truststoreType;

    private final SSLContext sslContext;

    SslConfig(Properties settings) {
        // ssl
        enabled = StringUtils.parseBoolean(settings.getProperty(SSL, SSL_DEFAULT));
        protocol = settings.getProperty(SSL_PROTOCOL, SSL_PROTOCOL_DEFAULT);
        keystoreLocation = settings.getProperty(SSL_KEYSTORE_LOCATION, SSL_KEYSTORE_LOCATION_DEFAULT);
        keystorePass = settings.getProperty(SSL_KEYSTORE_PASS, SSL_KEYSTORE_PASS_DEFAULT);
        keystoreType = settings.getProperty(SSL_KEYSTORE_TYPE, SSL_KEYSTORE_TYPE_DEFAULT);
        truststoreLocation = settings.getProperty(SSL_TRUSTSTORE_LOCATION, SSL_TRUSTSTORE_LOCATION_DEFAULT);
        truststorePass = settings.getProperty(SSL_TRUSTSTORE_PASS, SSL_TRUSTSTORE_PASS_DEFAULT);
        truststoreType = settings.getProperty(SSL_TRUSTSTORE_TYPE, SSL_TRUSTSTORE_TYPE_DEFAULT);

        sslContext = enabled ? createSSLContext() : null;
    }

    // ssl
    boolean isEnabled() {
        return enabled;
    }

    SSLSocketFactory sslSocketFactory() {
        return sslContext.getSocketFactory();
    }

    private SSLContext createSSLContext() {
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance(protocol);
            ctx.init(loadKeyManagers(), loadTrustManagers(), null);
        } catch (Exception ex) {
            throw new ClientException(ex, "Failed to initialize SSL - %s", ex.getMessage());
        }

        return ctx;
    }

    private KeyManager[] loadKeyManagers() throws GeneralSecurityException, IOException {
        if (!StringUtils.hasText(keystoreLocation)) {
            return null;
        }

        char[] pass = (StringUtils.hasText(keystorePass) ? keystorePass.trim().toCharArray() : null);
        KeyStore keyStore = loadKeyStore(keystoreLocation, pass, keystoreType);
        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmFactory.init(keyStore, pass);
        return kmFactory.getKeyManagers();
    }


    private KeyStore loadKeyStore(String location, char[] pass, String keyStoreType) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        Path path = Paths.get(location);

        if (!Files.exists(path)) {
           throw new ClientException(
                   "Expected to find keystore file at [%s] but was unable to. Make sure you have specified a valid URI.", location); 
        }
        
        try (InputStream in = Files.newInputStream(Paths.get(location), StandardOpenOption.READ)) {
            keyStore.load(in, pass);
        } catch (Exception ex) {
            throw new ClientException(ex, "Cannot open keystore [%s] - %s", location, ex.getMessage());
        } finally {
            
        }
        return keyStore;
    }

    private TrustManager[] loadTrustManagers() throws GeneralSecurityException, IOException {
        KeyStore keyStore = null;

        if (StringUtils.hasText(truststoreLocation)) {
            char[] pass = (StringUtils.hasText(truststorePass) ? truststorePass.trim().toCharArray() : null);
            keyStore = loadKeyStore(truststoreLocation, pass, truststoreType);
        }

        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(keyStore);
        return tmFactory.getTrustManagers();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        SslConfig other = (SslConfig) obj;
        return Objects.equals(enabled, other.enabled)
                && Objects.equals(protocol, other.protocol)
                && Objects.equals(keystoreLocation, other.keystoreLocation)
                && Objects.equals(keystorePass, other.keystorePass)
                && Objects.equals(keystoreType, other.keystoreType)
                && Objects.equals(truststoreLocation, other.truststoreLocation)
                && Objects.equals(truststorePass, other.truststorePass)
                && Objects.equals(truststoreType, other.truststoreType);
    }

    public int hashCode() {
        return getClass().hashCode();
    }
}