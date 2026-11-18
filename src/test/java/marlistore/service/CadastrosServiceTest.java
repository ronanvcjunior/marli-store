package marlistore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.OperacaoNaoPermitidaException;
import marlistore.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CadastrosServiceTest {

    @TempDir
    Path pasta;
    Cenario cen;

    @BeforeEach
    void preparar() {
        cen = new Cenario(pasta);
    }

    @Test
    void novaCategoriaComCamposSemAlterarCodigoNeg06() throws Exception {
        int antes = Categoria.getProximoId();
        Categoria c = cen.catalogo.cadastrarCategoria("Alimentos", List.of("validade"));
        Produto p = cen.catalogo.cadastrarProduto("Café", new BigDecimal("12,50".replace(',', '.')), 5, c.getId(),
                Map.of("validade", "01/12/2027"));
        assertEquals("01/12/2027", p.getCaracteristicas().get("validade"));
        assertEquals(antes, c.getId());
    }

    @Test
    void categoriaRejeitaNomeRepetidoECampoDuplicadoSemGastarId() throws Exception {
        cen.catalogo.cadastrarCategoria("Beleza", List.of());
        int antes = Categoria.getProximoId();
        assertThrows(DadosInvalidosException.class, () -> cen.catalogo.cadastrarCategoria("  beleza ", List.of()));
        assertThrows(DadosInvalidosException.class, () -> cen.catalogo.cadastrarCategoria("Casa", List.of("a", "A")));
        assertEquals(antes, Categoria.getProximoId());
    }

    @Test
    void produtoRejeitaCampoQueACategoriaNaoDeclara() throws Exception {
        Categoria c = cen.catalogo.cadastrarCategoria("Casa", List.of("cor"));
        assertThrows(DadosInvalidosException.class, () -> cen.catalogo.cadastrarProduto("Vaso", BigDecimal.TEN, 1,
                c.getId(), Map.of("garantia", "1 ano")));
        assertThrows(DadosInvalidosException.class, () -> cen.catalogo.cadastrarProduto("Vaso", BigDecimal.TEN, 1, 999, Map.of()));
    }

    @Test
    void reporEstoqueEFiltros() throws Exception {
        Categoria c = cen.catalogo.cadastrarCategoria("Casa", List.of());
        Categoria d = cen.catalogo.cadastrarCategoria("Beleza", List.of());
        Produto vaso = cen.catalogo.cadastrarProduto("Vaso Azul", BigDecimal.TEN, 0, c.getId(), Map.of());
        cen.catalogo.cadastrarProduto("Batom", BigDecimal.TEN, 3, d.getId(), Map.of());
        assertEquals(1, cen.catalogo.listarProdutos(c.getId()).size());
        assertEquals(2, cen.catalogo.listarProdutos(null).size());
        assertEquals(1, cen.catalogo.listarProdutosDisponiveis(null).size(), "Cliente não vê produto sem estoque");
        assertEquals(1, cen.catalogo.buscarProdutosPorNome("vaso").size());
        assertEquals(1, cen.catalogo.buscarProdutosPorNome("AZUL").size());
        cen.catalogo.reporEstoque(vaso.getId(), 5);
        assertEquals(5, vaso.getEstoque());
        assertEquals(2, cen.catalogo.listarProdutosDisponiveis(null).size());
        assertThrows(DadosInvalidosException.class, () -> cen.catalogo.reporEstoque(vaso.getId(), 0));
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.catalogo.excluirCategoria(c.getId()));
        cen.catalogo.excluirProduto(vaso.getId());
        cen.catalogo.excluirCategoria(c.getId());
        assertEquals(1, cen.catalogo.listarCategorias().size());
    }

    @Test
    void clienteComCpfUnicoEValido() throws Exception {
        Cliente ana = cen.novoCliente("Ana Souza", Cenario.CPF_1);
        assertThrows(DadosInvalidosException.class, () -> cen.novoCliente("Outra", "529.982.247-25"));
        assertThrows(DadosInvalidosException.class, () -> cen.novoCliente("Outra", "123.456.789-00"));
        assertTrue(cen.clientes.buscarPorCpf("52998224725").isPresent());
        assertEquals(1, cen.clientes.buscarPorNome("souza").size());
        cen.clientes.alterar(ana.getId(), "Ana S.", Cenario.CPF_1, "novo@email.com", "6233334444");
        assertEquals("Ana S.", ana.getNome());
        Cliente bia = cen.novoCliente("Bia", Cenario.CPF_2);
        assertThrows(DadosInvalidosException.class,
                () -> cen.clientes.alterar(bia.getId(), "Bia", Cenario.CPF_1, "b@b.com", "6233334444"));
        cen.clientes.excluir(bia.getId());
        assertTrue(cen.clientes.buscarPorCpf(Cenario.CPF_2).isEmpty());
    }

    @Test
    void promocoesComoDadosKpi06() throws Exception {
        Categoria c = cen.catalogo.cadastrarCategoria("Beleza", List.of());
        Produto p = cen.catalogo.cadastrarProduto("Batom", new BigDecimal("50"), 10, c.getId(), Map.of());
        // desconto simples, por quantidade, escalonado, leve X pague Y e preço promocional
        cen.promocoes.cadastrar("10% Beleza", EscopoPromocao.CATEGORIA, c.getId(), 1, 1, 1, TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        cen.promocoes.cadastrar("15% a partir de 3", EscopoPromocao.TODOS, null, 3, 1, 1, TipoDesconto.PERCENTUAL, new BigDecimal("15"), null, null);
        cen.promocoes.cadastrar("Leve 3 pague 2", EscopoPromocao.PRODUTO, p.getId(), 1, 3, 1, TipoDesconto.PERCENTUAL, new BigDecimal("100"), null, null);
        cen.promocoes.cadastrar("R$ 5 a menos", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.VALOR_FIXO, new BigDecimal("5"), null, null);
        Promocao preco = cen.promocoes.cadastrar("R$ 40 a partir de 10", EscopoPromocao.TODOS, null, 10, 1, 1, TipoDesconto.PRECO_FIXO, new BigDecimal("40"), null, null);
        assertEquals(5, cen.promocoes.listar().size());

        cen.promocoes.ativar(preco.getId(), false);
        assertEquals(4, cen.promocoes.listarVigentes(LocalDate.of(2026, 11, 17)).size());
        cen.promocoes.alterar(preco.getId(), "Novo nome", EscopoPromocao.TODOS, null, 10, 1, 1,
                TipoDesconto.PRECO_FIXO, new BigDecimal("30"), null, null);
        assertEquals("Novo nome", cen.promocoes.buscar(preco.getId()).getNome());
        cen.promocoes.excluir(preco.getId());
        assertEquals(4, cen.promocoes.listar().size());
    }

    @Test
    void promocaoValidaAlvo() {
        assertThrows(DadosInvalidosException.class, () -> cen.promocoes.cadastrar("X", EscopoPromocao.PRODUTO, 999,
                1, 1, 1, TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null));
        assertThrows(DadosInvalidosException.class, () -> cen.promocoes.cadastrar("X", EscopoPromocao.CATEGORIA, 999,
                1, 1, 1, TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null));
    }
}
