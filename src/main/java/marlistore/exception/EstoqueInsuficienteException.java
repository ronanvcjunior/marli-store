package marlistore.exception;

import java.io.Serial;

/**
 * Lançada quando uma operação tentaria vender mais unidades do que o saldo em
 * estoque (regra de negócio RN-01 e requisito RF-24).
 */
public class EstoqueInsuficienteException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nomeProduto;
    private final int disponivel;
    private final int solicitado;

    /**
     * Cria a exceção.
     *
     * @param nomeProduto nome do produto que não tem saldo suficiente
     * @param disponivel  unidades disponíveis em estoque
     * @param solicitado  unidades que se tentou vender
     */
    public EstoqueInsuficienteException(String nomeProduto, int disponivel, int solicitado) {
        super("O produto " + nomeProduto + " tem apenas " + disponivel
                + " unidade(s) em estoque; não é possível vender " + solicitado + ".");
        this.nomeProduto = nomeProduto;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    /** @return nome do produto sem saldo suficiente */
    public String getNomeProduto() {
        return nomeProduto;
    }

    /** @return unidades disponíveis em estoque */
    public int getDisponivel() {
        return disponivel;
    }

    /** @return unidades solicitadas */
    public int getSolicitado() {
        return solicitado;
    }
}
