package org.bayaweaver.artifactproxy;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class Application {

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        ApplicationProperties props = ApplicationProperties.parse(args);
        SSLContext sslContext = createSslContext(
                props.value(ApplicationProperties.Property.SSL_CERTIFICATE_PATH),
                props.value(ApplicationProperties.Property.SSL_CA_BUNDLE_PATH),
                props.value(ApplicationProperties.Property.SSL_PRIVATE_KEY_PATH));
        int port = Integer.parseInt(props.value(ApplicationProperties.Property.SERVER_PORT));
        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/", ArtifactRepositoryAccess.codeartifact(
                props.value(ApplicationProperties.Property.CODEARTIFACT_DOMAIN),
                props.value(ApplicationProperties.Property.CODEARTIFACT_DOMAIN_OWNNER),
                props.value(ApplicationProperties.Property.CODEARTIFACT_REGION),
                props.value(ApplicationProperties.Property.AWS_ACCESS_KEY_ID),
                props.value(ApplicationProperties.Property.AWS_SECRET_ACCESS_KEY)));
        server.start();
        System.out.println("Server has started on the port " + port);
    }

    private static SSLContext createSslContext(
            String certificateFilePath,
            String caBundleFilePath,
            String privateKeyFilePath)
            throws IOException, GeneralSecurityException {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
        X509Certificate[] certificates;
        SslCertificateProvider certificateProvider = new SslCertificateProvider();
        X509Certificate mainCertificate = certificateProvider.readCertificate(certificateFilePath);
        if (caBundleFilePath != null) {
            certificates = new X509Certificate[]{mainCertificate, certificateProvider.readCertificate(caBundleFilePath)};
        } else {
            certificates = new X509Certificate[]{mainCertificate};
        }
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(
                "alias",
                certificateProvider.readPrivateKey(privateKeyFilePath),
                keyStorePassword,
                certificates);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}
