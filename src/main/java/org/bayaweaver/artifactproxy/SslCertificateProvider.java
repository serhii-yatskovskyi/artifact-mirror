package org.bayaweaver.artifactproxy;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
        try (InputStream fileStream = new FileInputStream(filePath)) {
            return (X509Certificate) f.generateCertificate(fileStream);
        }
    }

    PrivateKey readPrivateKey(String filePath) throws IOException {
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
