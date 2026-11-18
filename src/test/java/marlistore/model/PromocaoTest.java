package marlistore.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import marlistore.exception.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica os exemplos do documento (5.3), com preço unitário de R$ 50,00. */
class PromocaoTest {

    private static final BigDecimal PRECO = new BigDecimal("50.00");
    private Produto produto;
    private Categoria categoria;

    @BeforeEach
    void preparar() throws Exception {
        categoria = new Categoria("Beleza");
        produto = new Produto("Batom", PRECO, 100, categoria);
    }

    private Promocao promo(EscopoPromocao esc, Integer alvo, int min, int grupo, int prem, TipoDesconto t,
                           String valor) throws DadosInvalidosException {
        return new Promocao("P", esc, alvo, min, grupo, prem, t, new BigDecimal(valor), null, null);
    }

    private static BigDecimal dec(String s) {
        return new BigDecimal(s);
    }

    @Test
    void levePague2() throws Exception {
        Promocao p = promo(EscopoPromocao.TODOS, null, 1, 3, 1, TipoDesconto.PERCENTUAL, "100");
        assertEquals(dec("50.00"), p.calcularDesconto(PRECO, 3));
        assertEquals(dec("0.00"), p.calcularDesconto(PRECO, 2));
        assertEquals(dec("100.00"), p.calcularDesconto(PRECO, 7));
    }

    @Test
    void segundaUnidadeCom50() throws Exception {
        Promocao p = promo(EscopoPromocao.TODOS, null, 1, 2, 1, TipoDesconto.PERCENTUAL, "50");
        assertEquals(dec("25.00"), p.calcularDesconto(PRECO, 2));
        assertEquals(dec("50.00"), p.calcularDesconto(PRECO, 5));
    }

    @Test
    void dezPorCentoAPartirDeTres() throws Exception {
        Promocao p = promo(EscopoPromocao.TODOS, null, 3, 1, 1, TipoDesconto.PERCENTUAL, "10");
        assertEquals(dec("15.00"), p.calcularDesconto(PRECO, 3));
        assertFalse(p.aplicaA(new ItemPedido(produto, 2), LocalDate.now()));
        assertTrue(p.aplicaA(new ItemPedido(produto, 3), LocalDate.now()));
    }

    @Test
    void valorFixoEPrecoFixo() throws Exception {
        Promocao fixo = promo(EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.VALOR_FIXO, "5");
        assertEquals(dec("15.00"), fixo.calcularDesconto(PRECO, 3));
        Promocao preco = promo(EscopoPromocao.TODOS, null, 10, 1, 1, TipoDesconto.PRECO_FIXO, "40");
        assertEquals(dec("100.00"), preco.calcularDesconto(PRECO, 10));
    }

    @Test
    void descontoNuncaExcedeSubtotal() throws Exception {
        Promocao p = promo(EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.VALOR_FIXO, "80");
        assertEquals(dec("100.00"), p.calcularDesconto(PRECO, 2));
        Promocao maior = promo(EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PRECO_FIXO, "90");
        assertEquals(dec("0.00"), maior.calcularDesconto(PRECO, 2));
    }

    @Test
    void escopoCategoriaEProduto() throws Exception {
        Categoria outra = new Categoria("Casa");
        Promocao cat = promo(EscopoPromocao.CATEGORIA, categoria.getId(), 1, 1, 1, TipoDesconto.PERCENTUAL, "10");
        Promocao catOutra = promo(EscopoPromocao.CATEGORIA, outra.getId(), 1, 1, 1, TipoDesconto.PERCENTUAL, "10");
        Promocao prod = promo(EscopoPromocao.PRODUTO, produto.getId(), 1, 1, 1, TipoDesconto.PERCENTUAL, "10");
        ItemPedido item = new ItemPedido(produto, 1);
        LocalDate hoje = LocalDate.of(2026, 11, 17);
        assertTrue(cat.aplicaA(item, hoje));
        assertFalse(catOutra.aplicaA(item, hoje));
        assertTrue(prod.aplicaA(item, hoje));
    }

    @Test
    void vigenciaEAtivacao() throws Exception {
        Promocao p = new Promocao("Novembro", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL,
                dec("10"), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        ItemPedido item = new ItemPedido(produto, 1);
        assertTrue(p.aplicaA(item, LocalDate.of(2026, 11, 1)));
        assertTrue(p.aplicaA(item, LocalDate.of(2026, 11, 30)));
        assertFalse(p.aplicaA(item, LocalDate.of(2026, 10, 31)));
        assertFalse(p.aplicaA(item, LocalDate.of(2026, 12, 1)));
        p.ativar(false);
        assertFalse(p.aplicaA(item, LocalDate.of(2026, 11, 15)));
    }

    @Test
    void validacoesDoAssistente() {
        assertThrows(DadosInvalidosException.class,
                () -> promo(EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL, "100.01"));
        assertThrows(DadosInvalidosException.class,
                () -> promo(EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL, "0"));
        assertThrows(DadosInvalidosException.class,
                () -> promo(EscopoPromocao.TODOS, null, 1, 3, 4, TipoDesconto.PERCENTUAL, "10"));
        assertThrows(DadosInvalidosException.class,
                () -> promo(EscopoPromocao.TODOS, null, 1, 3, 0, TipoDesconto.PERCENTUAL, "10"));
        assertThrows(DadosInvalidosException.class,
                () -> promo(EscopoPromocao.CATEGORIA, null, 1, 1, 1, TipoDesconto.PERCENTUAL, "10"));
        assertThrows(DadosInvalidosException.class, () -> new Promocao("P", EscopoPromocao.TODOS, null, 1, 1, 1,
                TipoDesconto.PERCENTUAL, dec("10"), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void rascunhoNaoConsomeIdEDescricaoEmPortugues() throws Exception {
        int antes = Promocao.getProximoId();
        Promocao r = Promocao.rascunho("Leve 3 pague 2", EscopoPromocao.TODOS, null, 1, 3, 1,
                TipoDesconto.PERCENTUAL, dec("100"), null, null);
        assertEquals(antes, Promocao.getProximoId());
        assertTrue(r.descricao().contains("100% de desconto"));
        assertTrue(r.descricao().contains("grupo de 3"));
    }

    @Test
    void idaEVolta() throws Exception {
        Promocao p = new Promocao("Natal; 2026", EscopoPromocao.PRODUTO, produto.getId(), 2, 3, 2,
                TipoDesconto.VALOR_FIXO, dec("5"), LocalDate.of(2026, 12, 1), null);
        p.ativar(false);
        Promocao l = Promocao.fromLinha(p.toLinha());
        assertEquals(p.getId(), l.getId());
        assertEquals("Natal; 2026", l.getNome());
        assertFalse(l.isAtiva());
        assertEquals(EscopoPromocao.PRODUTO, l.getEscopo());
        assertEquals(produto.getId(), l.getAlvoId());
        assertEquals(3, l.getTamanhoGrupo());
        assertEquals(LocalDate.of(2026, 12, 1), l.getInicioVigencia());
        assertNull(l.getFimVigencia());
        assertEquals(dec("5.00"), l.getValor());
    }
}
