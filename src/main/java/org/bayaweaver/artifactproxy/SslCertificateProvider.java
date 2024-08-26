package org.bayaweaver.artifactproxy;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

class SslCertificateProvider {

    X509Certificate readCertificate(String filePath) throws IOException, CertificateException {
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        try (InputStream fileContent = readFile(filePath)) {
            return (X509Certificate) f.generateCertificate(fileContent);
        }
    }

    PrivateKey readPrivateKey(String filePath) throws IOException {
        try (InputStream fileStream = readFile(filePath);
                 Reader reader = new InputStreamReader(fileStream)) {

            PEMParser pemParser = new PEMParser(reader);
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof PrivateKeyInfo pkcs8Key) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Key.getEncoded());
                return keyFactory.generatePrivate(keySpec);
            } else if (pemObject instanceof PEMKeyPair pkcs1Key) {
                PrivateKeyInfo privateKeyInfo = pkcs1Key.getPrivateKeyInfo();
                byte[] pkcs8EncodedKey = privateKeyInfo.getEncoded();
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedKey);
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePrivate(keySpec);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IOException(e);
                }
            } else {
                throw new IOException("Unknown private key format.");
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private InputStream readFile(String filePath) throws IOException {
        InputStream fileContent;
        fileContent = getClass().getClassLoader().getResourceAsStream(filePath);
        if (fileContent != null) {
            return fileContent;
        }
        return new FileInputStream(filePath);
    }
}
