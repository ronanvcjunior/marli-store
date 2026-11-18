package marlistore.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import marlistore.model.ItemPedido;
import marlistore.model.Pedido;
import marlistore.model.Promocao;
import marlistore.model.ResultadoDesconto;

/**
 * Motor de descontos (RN-07). Por item, vale a promoção de maior valor em
 * reais entre as ativas, vigentes e do escopo do produto. Promoções não se
 * acumulam, o desconto não excede o subtotal do item e, em empate, vale a de
 * menor ID.
 */
public class MotorDescontos {

    /**
     * Escolhe a melhor promoção para um item.
     *
     * @param item      item do pedido
     * @param promocoes promoções cadastradas (o motor filtra as aplicáveis)
     * @param hoje      data de referência para a vigência
     * @return o melhor desconto, ou {@link ResultadoDesconto#NENHUM}
     */
    public ResultadoDesconto avaliar(ItemPedido item, List<Promocao> promocoes, LocalDate hoje) {
        Promocao melhor = null;
        BigDecimal melhorValor = BigDecimal.ZERO;
        for (Promocao p : promocoes) {
            if (!p.aplicaA(item, hoje)) {
                continue;
            }
            BigDecimal valor = p.calcularDesconto(item.getPrecoUnitario(), item.getQuantidade());
            if (valor.signum() <= 0) {
                continue;
            }
            int cmp = melhor == null ? 1 : valor.compareTo(melhorValor);
            if (cmp > 0 || (cmp == 0 && p.getId() < melhor.getId())) {
                melhor = p;
                melhorValor = valor;
            }
        }
        return melhor == null ? ResultadoDesconto.NENHUM : new ResultadoDesconto(melhorValor, melhor);
    }

    /**
     * Aplica a melhor promoção a cada item do pedido.
     *
     * @param pedido    pedido a recalcular
     * @param promocoes promoções cadastradas
     * @param hoje      data de referência
     */
    public void aplicarAoPedido(Pedido pedido, List<Promocao> promocoes, LocalDate hoje) {
        for (ItemPedido item : pedido.getItens()) {
            item.aplicarDesconto(avaliar(item, promocoes, hoje));
        }
    }
}
