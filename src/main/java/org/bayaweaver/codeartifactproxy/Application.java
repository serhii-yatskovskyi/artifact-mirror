package org.bayaweaver.codeartifactproxy;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class Application {

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        ApplicationProperties props = ApplicationProperties.parse(args);
        SSLContext sslContext = createSslContext(
                props.value(ApplicationProperties.Property.KEYSTORE_PATH),
                props.value(ApplicationProperties.Property.KEYSTORE_PASSWORD).toCharArray());
        int port = Integer.parseInt(props.value(ApplicationProperties.Property.SERVER_PORT));
        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/", new GeneralHttpHandler(
                props.value(ApplicationProperties.Property.CODEARTIFACT_DOMAIN),
                props.value(ApplicationProperties.Property.CODEARTIFACT_DOMAIN_OWNNER),
                props.value(ApplicationProperties.Property.CODEARTIFACT_REGION),
                props.value(ApplicationProperties.Property.AWS_ACCESS_KEY_ID),
                props.value(ApplicationProperties.Property.AWS_SECRET_ACCESS_KEY)));
        server.start();
        System.out.println("Server has started on the port " + port);
    }

    private static SSLContext createSslContext(String keyStoreFilePath, char[] keyStorePassword)
            throws IOException, GeneralSecurityException {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = (keyStoreFilePath.endsWith("p12"))
                ? KeyStore.getInstance("PKCS12")
                : KeyStore.getInstance("JKS");
        try (InputStream fileContent = new FileInputStream(keyStoreFilePath);) {
            keyStore.load(fileContent, keyStorePassword);
        } catch (Throwable e) {
            System.err.println("Error loading keystore from " + keyStoreFilePath + ".");
            throw e;
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}
