package br.com.fernandobastian.financeiro;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class TestDatabaseProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        String url = System.getenv("TEST_DB_URL");
        if (url == null || url.isBlank()) return Map.of();
        return Map.of("quarkus.datasource.jdbc.url", url);
    }
}
