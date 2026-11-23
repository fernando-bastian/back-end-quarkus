import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

class PrepararAmbiente {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of("config");
        Files.createDirectories(directory);
        Path privateKey = directory.resolve("private-key.pem");
        Path publicKey = directory.resolve("public-key.pem");
        if (Files.exists(privateKey) != Files.exists(publicKey)) {
            throw new IllegalStateException("Existe somente uma chave. Restaure o par antes de continuar.");
        }
        if (!Files.exists(privateKey)) {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            Files.writeString(privateKey, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
            Files.writeString(publicKey, pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        }
        Path env = Path.of(".env");
        if (!Files.exists(env)) {
            String template = Files.readString(Path.of(".env.example")).replace("\r\n", "\n");
            template = template.replace("DB_PASSWORD=\n", "DB_PASSWORD=" + secret() + "\n")
                    .replace("CSRF_SECRET=\n", "CSRF_SECRET=" + secret() + "\n");
            Files.writeString(env, template);
        }
        System.out.println("Ambiente preparado: config/ e .env. Arquivos existentes foram preservados.");
        System.out.println("Preencha APP_BOOTSTRAP_EMAIL e APP_BOOTSTRAP_SENHA no .env para criar o primeiro usuário.");
    }

    private static String secret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String pem(String label, byte[] bytes) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                + "\n-----END " + label + "-----\n";
    }
}
