package marlistore.exception;

import java.io.Serial;

/**
 * Lançada quando uma entrada não passa na validação (RF-25): preço, quantidade,
 * CPF, datas, campos obrigatórios e afins.
 */
public class DadosInvalidosException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação específica, em português, do dado rejeitado
     */
    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
