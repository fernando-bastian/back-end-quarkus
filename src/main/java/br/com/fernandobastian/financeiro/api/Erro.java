package br.com.fernandobastian.financeiro.api;

import java.util.List;

public record Erro(String mensagem, List<String> detalhes) {
    public Erro(String mensagem) {
        this(mensagem, List.of());
    }
}
