package br.com.fernandobastian.financeiro.auth;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@ApplicationScoped
public class Senhas {
    private static final int ITERACOES = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derivar(senha, salt, ITERACOES));
    }

    public boolean conferir(String senha, String hash) {
        try {
            String[] parts = hash.split("\\$");
            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERACOES || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derivar(senha, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String senha, byte[] salt, int iterations) {
        char[] chars = senha.toCharArray();
        var spec = new PBEKeySpec(chars, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível processar a senha.", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
