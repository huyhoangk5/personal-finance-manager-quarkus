package com.finance.pfm.util;

import java.io.FileWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Utility để tạo RSA key pair cho JWT.
 * Chạy một lần: java GenerateKeys
 * Output: privateKey.pem và publicKey.pem trong src/main/resources/
 */
public class GenerateKeys {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        String resourceDir = "src/main/resources/";

        try (FileWriter fw = new FileWriter(resourceDir + "privateKey.pem")) {
            fw.write(privateKeyPem);
        }
        try (FileWriter fw = new FileWriter(resourceDir + "publicKey.pem")) {
            fw.write(publicKeyPem);
        }

        System.out.println("✅ privateKey.pem và publicKey.pem đã được tạo trong " + resourceDir);
        System.out.println("⚠️  KHÔNG commit privateKey.pem lên git!");
    }
}
