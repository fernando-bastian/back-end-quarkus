package br.com.fernandobastian.financeiro.api;

public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
