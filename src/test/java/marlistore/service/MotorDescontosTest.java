package marlistore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import marlistore.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** KPI-02: o total confere com o cálculo manual em cada tipo de efeito, de grupo e de disputa. */
class MotorDescontosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private final MotorDescontos motor = new MotorDescontos();
    private Categoria beleza;
    private Produto batom;

    @BeforeEach
    void preparar() throws Exception {
        beleza = new Categoria("Beleza");
        batom = new Produto("Batom", new BigDecimal("50.00"), 100, beleza);
    }

    private Promocao pct(int min, String valor) throws Exception {
        return new Promocao("P" + min, EscopoPromocao.TODOS, null, min, 1, 1, TipoDesconto.PERCENTUAL,
                new BigDecimal(valor), null, null);
    }

    @Test
    void escalonadoComDozeUnidadesDa90() throws Exception {
        List<Promocao> promos = List.of(pct(2, "5"), pct(5, "10"), pct(10, "15"));
        ItemPedido item = new ItemPedido(batom, 12);
        ResultadoDesconto r = motor.avaliar(item, promos, HOJE);
        assertEquals(new BigDecimal("90.00"), r.getValor());
        assertEquals("P10", r.getPromocao().getNome());
    }

    @Test
    void escalonadoEscolheFaixaPelaQuantidade() throws Exception {
        List<Promocao> promos = List.of(pct(2, "5"), pct(5, "10"), pct(10, "15"));
        assertEquals(new BigDecimal("5.00"), motor.avaliar(new ItemPedido(batom, 2), promos, HOJE).getValor());
        assertEquals(new BigDecimal("25.00"), motor.avaliar(new ItemPedido(batom, 5), promos, HOJE).getValor());
        assertSame(ResultadoDesconto.NENHUM, motor.avaliar(new ItemPedido(batom, 1), promos, HOJE));
    }

    @Test
    void naoAcumulaEEmpateVenceMenorId() throws Exception {
        Promocao a = pct(1, "10");
        Promocao b = new Promocao("B", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.VALOR_FIXO,
                new BigDecimal("5"), null, null); // R$ 5 por unidade = 10% de R$ 50
        ItemPedido item = new ItemPedido(batom, 2);
        assertEquals(a, motor.avaliar(item, List.of(b, a), HOJE).getPromocao());
        assertEquals(new BigDecimal("10.00"), motor.avaliar(item, List.of(a, b), HOJE).getValor());
    }

    @Test
    void venceMaiorValorEmReais() throws Exception {
        Promocao dezPorCento = pct(1, "10");
        Promocao levePague = new Promocao("Leve 3 pague 2", EscopoPromocao.CATEGORIA, beleza.getId(), 1, 3, 1,
                TipoDesconto.PERCENTUAL, new BigDecimal("100"), null, null);
        ItemPedido tres = new ItemPedido(batom, 3);
        ResultadoDesconto r = motor.avaliar(tres, List.of(dezPorCento, levePague), HOJE);
        assertEquals(levePague, r.getPromocao());
        assertEquals(new BigDecimal("50.00"), r.getValor());
        ItemPedido dois = new ItemPedido(batom, 2);
        assertEquals(dezPorCento, motor.avaliar(dois, List.of(dezPorCento, levePague), HOJE).getPromocao());
    }

    @Test
    void ignoraInativasForaDaVigenciaEForaDoEscopo() throws Exception {
        Promocao inativa = pct(1, "50");
        inativa.ativar(false);
        Promocao vencida = new Promocao("V", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL,
                new BigDecimal("50"), null, HOJE.minusDays(1));
        Categoria casa = new Categoria("Casa");
        Promocao outraCategoria = new Promocao("C", EscopoPromocao.CATEGORIA, casa.getId(), 1, 1, 1,
                TipoDesconto.PERCENTUAL, new BigDecimal("50"), null, null);
        assertSame(ResultadoDesconto.NENHUM,
                motor.avaliar(new ItemPedido(batom, 1), List.of(inativa, vencida, outraCategoria), HOJE));
    }

    @Test
    void aplicaAoPedidoItemAItem() throws Exception {
        Produto outro = new Produto("Cabo", new BigDecimal("10.00"), 10, new Categoria("Eletro"));
        Pedido p = new Pedido(new Cliente("Ana", "52998224725", "a@b.com", "62999990000"), "Ana");
        p.adicionarItem(batom, 3);
        p.adicionarItem(outro, 1);
        Promocao promo = new Promocao("Batom 10%", EscopoPromocao.PRODUTO, batom.getId(), 3, 1, 1,
                TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        motor.aplicarAoPedido(p, List.of(promo), HOJE);
        assertEquals(new BigDecimal("15.00"), p.getItens().get(0).getDescontoAplicado());
        assertEquals(new BigDecimal("0.00"), p.getItens().get(1).getDescontoAplicado());
        assertEquals(new BigDecimal("145.00"), p.calcularTotal());
    }
}
