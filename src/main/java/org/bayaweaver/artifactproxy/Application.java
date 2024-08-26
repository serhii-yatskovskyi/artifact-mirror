package org.bayaweaver.artifactproxy;

import com.sun.net.httpserver.HttpServer;
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
        ApplicationOptions opts = ApplicationOptions.parse(args);
        ArtifactRepositoryAccess httpRequestHandler = ArtifactRepositoryAccess.codeartifact(
                opts.value(ApplicationOptions.Option.CODEARTIFACT_DOMAIN),
                opts.value(ApplicationOptions.Option.CODEARTIFACT_DOMAIN_OWNER),
                opts.value(ApplicationOptions.Option.CODEARTIFACT_REGION),
                opts.value(ApplicationOptions.Option.AWS_ACCESS_KEY_ID),
                opts.value(ApplicationOptions.Option.AWS_SECRET_ACCESS_KEY));
        int httpPort = Integer.parseInt(opts.value(ApplicationOptions.Option.HTTP_SERVER_PORT));
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
        httpServer.createContext("/", httpRequestHandler);
        httpServer.start();
        SSLContext sslContext = createSslContext(
                opts.value(ApplicationOptions.Option.SSL_CERTIFICATE_PATH),
                opts.value(ApplicationOptions.Option.SSL_CA_BUNDLE_PATH),
                opts.value(ApplicationOptions.Option.SSL_PRIVATE_KEY_PATH));
        int httpsPort = Integer.parseInt(opts.value(ApplicationOptions.Option.HTTPS_SERVER_PORT));
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(httpsPort), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsServer.createContext("/", httpRequestHandler);
        httpsServer.start();
        System.out.println("Server has started on the ports " + httpPort + " (http), " + httpsPort + " (https)");
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
