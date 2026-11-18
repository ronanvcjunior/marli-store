package marlistore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Efeito de uma promoção. Cada constante implementa o cálculo do desconto por
 * unidade (padrão Strategy): um desconto novo é uma combinação nova de
 * dados, não uma classe nova.
 */
public enum TipoDesconto {

    /** Percentual sobre o preço: {@code preço × valor ÷ 100}. */
    PERCENTUAL("Percentual (%)") {
        @Override
        public BigDecimal descontoPorUnidade(BigDecimal preco, BigDecimal valor) {
            return preco.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    },

    /** Valor fixo em reais por unidade, no máximo o preço. */
    VALOR_FIXO("Valor fixo (R$ a menos)") {
        @Override
        public BigDecimal descontoPorUnidade(BigDecimal preco, BigDecimal valor) {
            return valor.min(preco).setScale(2, RoundingMode.HALF_UP);
        }
    },

    /** Preço promocional: o desconto é {@code preço − valor} (nunca negativo). */
    PRECO_FIXO("Preço promocional") {
        @Override
        public BigDecimal descontoPorUnidade(BigDecimal preco, BigDecimal valor) {
            return preco.subtract(valor).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
    };

    private final String rotulo;

    TipoDesconto(String rotulo) {
        this.rotulo = rotulo;
    }

    /** @return nome em português, para exibição */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Calcula o desconto concedido a cada unidade premiada.
     *
     * @param preco preço unitário do produto
     * @param valor valor configurado na promoção
     * @return desconto por unidade, com duas casas decimais
     */
    public abstract BigDecimal descontoPorUnidade(BigDecimal preco, BigDecimal valor);
}
