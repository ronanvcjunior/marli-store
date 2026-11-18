package marlistore.exception;

import java.io.Serial;

/**
 * Lançada quando uma operação viola uma regra de negócio: transição de status
 * inválida (RN-03), cancelamento fora do fluxo (RN-05), acesso indevido por
 * perfil (RN-06) ou exclusão com vínculos (RN-09).
 */
public class OperacaoNaoPermitidaException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação, em português, do motivo do bloqueio
     */
    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
