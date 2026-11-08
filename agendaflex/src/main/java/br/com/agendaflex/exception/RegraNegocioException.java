package br.com.agendaflex.exception;

/** Lançada quando uma regra de negócio impede a operação. A mensagem é exibida ao usuário. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
