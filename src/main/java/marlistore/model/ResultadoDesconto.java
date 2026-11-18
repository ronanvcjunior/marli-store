package marlistore.model;

import java.math.BigDecimal;

/**
 * Resultado da avaliação de promoções para um item (value object): o valor do
 * desconto e a promoção que o gerou.
 */
public final class ResultadoDesconto {

    /** Resultado sem desconto algum. */
    public static final ResultadoDesconto NENHUM = new ResultadoDesconto(BigDecimal.ZERO.setScale(2), null);

    private final BigDecimal valor;
    private final Promocao promocao;

    /**
     * @param valor    desconto total em reais para o item
     * @param promocao promoção aplicada (nula quando não há desconto)
     */
    public ResultadoDesconto(BigDecimal valor, Promocao promocao) {
        this.valor = valor;
        this.promocao = promocao;
    }

    /** @return valor do desconto em reais */
    public BigDecimal getValor() {
        return valor;
    }

    /** @return promoção aplicada, ou {@code null} */
    public Promocao getPromocao() {
        return promocao;
    }
}
