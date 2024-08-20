package org.bayaweaver.codeartifactproxy;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
        server.createContext("/", new GeneralHttpHandler(
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
        X509Certificate mainCertificate = readCertificate(certificateFilePath);
        if (caBundleFilePath != null) {
            certificates = new X509Certificate[]{mainCertificate, readCertificate(caBundleFilePath)};
        } else {
            certificates = new X509Certificate[]{mainCertificate};
        }
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(
                "alias",
                readPrivateKey(privateKeyFilePath),
                keyStorePassword,
                certificates);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    private static X509Certificate readCertificate(String filePath) throws IOException, CertificateException {
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        try (InputStream fileStream = new FileInputStream(filePath)) {
            return (X509Certificate) f.generateCertificate(fileStream);
        }
    }

    private static PrivateKey readPrivateKey(String filePath) throws IOException {
        try (PEMParser pemParser = new PEMParser(new FileReader(filePath))) {
            if (pemParser.readObject() instanceof PEMKeyPair keyPair) {
                PrivateKeyInfo privateKeyInfo = keyPair.getPrivateKeyInfo();
                byte[] pkcs8EncodedKey = privateKeyInfo.getEncoded();
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedKey);
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePrivate(keySpec);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("The file '" + filePath
                        + "' does not contain a valid PKCS#1 RSA private key.");
            }
        }
    }
}
