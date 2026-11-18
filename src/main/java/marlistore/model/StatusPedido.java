package marlistore.model;

/**
 * Estados de um pedido e suas transições válidas (RN-03, padrão State).
 * {@link #CONCLUIDO} e {@link #CANCELADO} são estados finais (RN-05).
 */
public enum StatusPedido {
    /** Carrinho recém-criado, ainda sem itens e não gravado. */
    VAZIO("Vazio"),
    /** Pedido com itens, em edição. */
    EM_ANDAMENTO("Em andamento"),
    /** Estoque já baixado, aguardando confirmação da compra. */
    EM_FINALIZACAO("Em finalização"),
    /** Compra confirmada (estado final). */
    CONCLUIDO("Concluído"),
    /** Pedido cancelado (estado final). */
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusPedido(String rotulo) {
        this.rotulo = rotulo;
    }

    /** @return nome do status em português, para exibição */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Informa se a transição para o status de destino é permitida.
     *
     * @param destino status desejado
     * @return {@code true} se o fluxo permite a transição
     */
    public boolean podeIrPara(StatusPedido destino) {
        return switch (this) {
            case VAZIO -> destino == EM_ANDAMENTO;
            case EM_ANDAMENTO -> destino == EM_FINALIZACAO || destino == CANCELADO;
            case EM_FINALIZACAO -> destino == EM_ANDAMENTO || destino == CONCLUIDO || destino == CANCELADO;
            default -> false;
        };
    }

    /** @return {@code true} se o status é final */
    public boolean isFinal() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
