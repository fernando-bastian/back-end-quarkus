package br.com.fernandobastian.financeiro;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

public class TestEnvironment implements QuarkusTestResourceLifecycleManager {
    private Path directory;

    @Override
    public Map<String, String> start() {
        try {
            directory = Files.createTempDirectory("financeiro-test-keys-");
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            Path privateKey = directory.resolve("private.pem");
            Path publicKey = directory.resolve("public.pem");
            Files.writeString(privateKey, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
            Files.writeString(publicKey, pem("PUBLIC KEY", pair.getPublic().getEncoded()));
            return Map.of(
                    "smallrye.jwt.sign.key.location", privateKey.toUri().toString(),
                    "mp.jwt.verify.publickey.location", publicKey.toUri().toString(),
                    "app.cookie.secure", "false");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao preparar chaves de teste", e);
        }
    }

    private String pem(String label, byte[] bytes) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                + "\n-----END " + label + "-----\n";
    }

    @Override
    public void stop() {
        if (directory == null) return;
        try {
            Files.deleteIfExists(directory.resolve("private.pem"));
            Files.deleteIfExists(directory.resolve("public.pem"));
            Files.deleteIfExists(directory);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao remover chaves de teste", e);
        }
    }
}
