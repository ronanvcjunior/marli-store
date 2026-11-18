package marlistore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import marlistore.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** O Cliente precisa enxergar as promoções vigentes antes de montar o pedido. */
class PromocoesVigentesTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);

    @TempDir
    Path pasta;
    Cenario cen;
    Categoria beleza;
    Produto shampoo;
    Produto fone;

    @BeforeEach
    void preparar() throws Exception {
        cen = new Cenario(pasta);
        beleza = cen.catalogo.cadastrarCategoria("Beleza", List.of());
        Categoria eletro = cen.catalogo.cadastrarCategoria("Eletrônicos", List.of());
        shampoo = cen.catalogo.cadastrarProduto("Shampoo", new BigDecimal("30"), 20, beleza.getId(), Map.of());
        fone = cen.catalogo.cadastrarProduto("Fone", new BigDecimal("100"), 20, eletro.getId(), Map.of());
    }

    private Promocao leve3Pague2() throws Exception {
        return cen.promocoes.cadastrar("Leve 3 pague 2 - Shampoo", EscopoPromocao.PRODUTO, shampoo.getId(),
                1, 3, 1, TipoDesconto.PERCENTUAL, new BigDecimal("100"), null, null);
    }

    @Test
    void listaApenasAtivasEVigentes() throws Exception {
        Promocao ok = leve3Pague2();
        Promocao inativa = cen.promocoes.cadastrar("Inativa", EscopoPromocao.TODOS, null, 1, 1, 1,
                TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        cen.promocoes.ativar(inativa.getId(), false);
        cen.promocoes.cadastrar("Vencida", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL,
                BigDecimal.TEN, null, HOJE.minusDays(1));
        cen.promocoes.cadastrar("Futura", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL,
                BigDecimal.TEN, HOJE.plusDays(1), null);
        List<Promocao> vigentes = cen.promocoes.listarVigentes(HOJE);
        assertEquals(List.of(ok), vigentes);
    }

    @Test
    void descricaoMostraNomeDoProdutoEDaCategoriaSemStatusParaOCliente() throws Exception {
        Promocao porProduto = leve3Pague2();
        Promocao porCategoria = cen.promocoes.cadastrar("10% Beleza", EscopoPromocao.CATEGORIA, beleza.getId(),
                2, 1, 1, TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        String texto = cen.promocoes.descrever(porProduto, false);
        assertTrue(texto.contains("no produto Shampoo"), texto);
        assertTrue(texto.contains("a cada grupo de 3"), texto);
        assertFalse(texto.contains("[ativa]"), texto);
        assertFalse(texto.contains("código"), texto);
        String cat = cen.promocoes.descrever(porCategoria, false);
        assertTrue(cat.contains("na categoria Beleza") && cat.contains("a partir de 2 unidades"), cat);
        assertTrue(cen.promocoes.descrever(porProduto, true).endsWith("[ativa]"));
    }

    @Test
    void descricaoComAlvoRemovidoVoltaParaOCodigo() throws Exception {
        Promocao p = leve3Pague2();
        assertTrue(p.descricaoPublica(null).contains("código " + shampoo.getId()));
        assertTrue(p.descricao().contains("código"));
    }

    @Test
    void promocoesDoProdutoConsideramEscopo() throws Exception {
        Promocao shamp = leve3Pague2();
        Promocao loja = cen.promocoes.cadastrar("Loja 5%", EscopoPromocao.TODOS, null, 1, 1, 1,
                TipoDesconto.PERCENTUAL, new BigDecimal("5"), null, null);
        Promocao catBeleza = cen.promocoes.cadastrar("Beleza 10%", EscopoPromocao.CATEGORIA, beleza.getId(), 1, 1, 1,
                TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        assertEquals(List.of(shamp, loja, catBeleza), cen.promocoes.promocoesDoProduto(shampoo, HOJE));
        assertEquals(List.of(loja), cen.promocoes.promocoesDoProduto(fone, HOJE));
        assertTrue(shamp.cobreProduto(shampoo));
        assertFalse(shamp.cobreProduto(fone));
    }

    @Test
    void semPromocoesAListaVemVazia() {
        assertTrue(cen.promocoes.listarVigentes(HOJE).isEmpty());
        assertTrue(cen.promocoes.promocoesDoProduto(shampoo, HOJE).isEmpty());
    }
}
