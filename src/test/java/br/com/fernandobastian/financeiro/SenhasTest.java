package br.com.fernandobastian.financeiro;

import br.com.fernandobastian.financeiro.auth.Senhas;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SenhasTest {
    @Test
    void geraSaltAleatorioEConfereSemArmazenarSenhaPura() {
        var senhas = new Senhas();
        String first = senhas.hash("Uma-senha-comprida-123");
        String second = senhas.hash("Uma-senha-comprida-123");
        assertNotEquals(first, second);
        assertFalse(first.contains("Uma-senha-comprida-123"));
        assertTrue(senhas.conferir("Uma-senha-comprida-123", first));
        assertTrue(senhas.conferir("Uma-senha-comprida-123", second));
        assertFalse(senhas.conferir("senha-incorreta", first));
        assertFalse(senhas.conferir("Uma-senha-comprida-123", "hash-invalido"));
    }
}
