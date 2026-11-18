package marlistore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import marlistore.exception.OperacaoNaoPermitidaException;
import marlistore.model.*;
import marlistore.session.SessaoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PedidoServiceTest {

    @TempDir
    Path pasta;
    Cenario cen;
    Produto fone;
    Cliente ana;
    SessaoUsuario sessaoAna;
    SessaoUsuario admin;

    @BeforeEach
    void preparar() throws Exception {
        cen = new Cenario(pasta);
        Categoria c = cen.catalogo.cadastrarCategoria("Eletrônicos", List.of("garantia"));
        fone = cen.catalogo.cadastrarProduto("Fone", new BigDecimal("100.00"), 10, c.getId(), Map.of("garantia", "1 ano"));
        ana = cen.novoCliente("Ana Souza", Cenario.CPF_1);
        sessaoAna = cen.sessaoDe(ana);
        admin = cen.admin();
    }

    @Test
    void fluxoCompletoComPromocaoEPersistencia() throws Exception {
        cen.promocoes.cadastrar("Leve 3 pague 2", EscopoPromocao.PRODUTO, fone.getId(), 1, 3, 1,
                TipoDesconto.PERCENTUAL, new BigDecimal("100"), null, null);
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        assertEquals(0, cen.banco.getPedidos().tamanho(), "pedido VAZIO ainda não é gravado");

        cen.pedidos.adicionarItem(p, fone.getId(), 3, sessaoAna);
        assertEquals(1, cen.banco.getPedidos().tamanho());
        assertEquals(new BigDecimal("200.00"), p.calcularTotal());
        assertEquals("Ana Souza", p.getUsuarioSessao());

        cen.pedidos.iniciarFinalizacao(p, sessaoAna);
        assertEquals(7, fone.getEstoque());
        cen.pedidos.concluir(p, sessaoAna);
        assertEquals(StatusPedido.CONCLUIDO, p.getStatus());
        assertEquals(LocalDate.of(2026, 11, 17), p.getDataConclusao().toLocalDate());

        // KPI-03: dados idênticos após fechar e reabrir
        Cenario reaberto = new Cenario(pasta);
        Pedido lido = reaberto.banco.getPedidos().obter(p.getId());
        assertEquals(StatusPedido.CONCLUIDO, lido.getStatus());
        assertEquals(new BigDecimal("200.00"), lido.calcularTotal());
        assertEquals("Leve 3 pague 2", lido.getItens().get(0).getPromocaoAplicada());
        assertEquals(7, reaberto.banco.getProdutos().obter(fone.getId()).getEstoque());
        assertEquals("1 ano", reaberto.banco.getProdutos().obter(fone.getId()).getCaracteristicas().get("garantia"));
    }

    @Test
    void descontoRecalculadoAAlteracaoECongeladoDepoisDaFinalizacao() throws Exception {
        Promocao promo = cen.promocoes.cadastrar("10% a partir de 3", EscopoPromocao.TODOS, null, 3, 1, 1,
                TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null);
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        cen.pedidos.adicionarItem(p, fone.getId(), 2, sessaoAna);
        assertEquals(new BigDecimal("0.00"), p.calcularTotalDescontos());
        cen.pedidos.alterarQuantidade(p, 0, 3, sessaoAna);
        assertEquals(new BigDecimal("30.00"), p.calcularTotalDescontos());
        cen.pedidos.iniciarFinalizacao(p, sessaoAna);
        cen.promocoes.excluir(promo.getId());
        cen.catalogo.alterarProduto(fone.getId(), "Fone Novo", new BigDecimal("1.00"), fone.getCategoria().getId(), Map.of());
        assertEquals(new BigDecimal("270.00"), p.calcularTotal(), "RN-08: preço e desconto congelados");
    }

    @Test
    void estoqueInsuficienteNaInclusao() throws Exception {
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        EstoqueInsuficienteException e = assertThrows(EstoqueInsuficienteException.class,
                () -> cen.pedidos.adicionarItem(p, fone.getId(), 11, sessaoAna));
        assertTrue(e.getMessage().contains("apenas 10 unidade(s)"));
        assertTrue(p.getItens().isEmpty());
        assertEquals(0, cen.banco.getPedidos().tamanho());
    }

    @Test
    void cancelarDevolveEstoqueKpi05() throws Exception {
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        cen.pedidos.adicionarItem(p, fone.getId(), 4, sessaoAna);
        cen.pedidos.iniciarFinalizacao(p, sessaoAna);
        assertEquals(6, fone.getEstoque());
        cen.pedidos.cancelar(p, sessaoAna);
        assertEquals(10, fone.getEstoque());
        assertEquals(10, new Cenario(pasta).banco.getProdutos().obter(fone.getId()).getEstoque());

        Pedido q = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        cen.pedidos.adicionarItem(q, fone.getId(), 4, sessaoAna);
        cen.pedidos.iniciarFinalizacao(q, sessaoAna);
        cen.pedidos.voltarParaAndamento(q, sessaoAna);
        assertEquals(10, fone.getEstoque());
    }

    @Test
    void doisCarrinhosNaoDisputamOMesmoSaldoNaFinalizacao() throws Exception {
        Cliente bia = cen.novoCliente("Bia Lima", Cenario.CPF_2);
        SessaoUsuario sBia = cen.sessaoDe(bia);
        Pedido a = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        Pedido b = cen.pedidos.criarPedido(bia.getId(), sBia);
        cen.pedidos.adicionarItem(a, fone.getId(), 8, sessaoAna);
        cen.pedidos.adicionarItem(b, fone.getId(), 8, sBia);
        cen.pedidos.iniciarFinalizacao(a, sessaoAna);
        assertThrows(EstoqueInsuficienteException.class, () -> cen.pedidos.iniciarFinalizacao(b, sBia));
        assertEquals(StatusPedido.EM_ANDAMENTO, b.getStatus());
        assertEquals(2, fone.getEstoque());
    }

    @Test
    void clienteSoAcessaOsProprios() throws Exception {
        Cliente bia = cen.novoCliente("Bia Lima", Cenario.CPF_2);
        SessaoUsuario sBia = cen.sessaoDe(bia);
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        cen.pedidos.adicionarItem(p, fone.getId(), 1, sessaoAna);

        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.pedidos.criarPedido(ana.getId(), sBia));
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.pedidos.cancelar(p, sBia));
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.pedidos.buscarPedido(p.getId(), sBia));
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.pedidos.historicoDoCliente(ana.getId(), sBia));
        assertTrue(cen.pedidos.listarPedidos(sBia).isEmpty());
        assertEquals(1, cen.pedidos.listarPedidos(sessaoAna).size());
        assertEquals(1, cen.pedidos.listarPedidos(admin).size());
        assertEquals(1, cen.pedidos.historicoDoCliente(ana.getId(), admin).size());
        assertSame(p, cen.pedidos.buscarPedido(p.getId(), admin));
        assertThrows(DadosInvalidosException.class, () -> cen.pedidos.buscarPedido(999, admin));
    }

    @Test
    void exclusoesComVinculoSaoBloqueadasRn09() throws Exception {
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        cen.pedidos.adicionarItem(p, fone.getId(), 1, sessaoAna);
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.catalogo.excluirProduto(fone.getId()));
        assertThrows(OperacaoNaoPermitidaException.class, () -> cen.clientes.excluir(ana.getId()));
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> cen.catalogo.excluirCategoria(fone.getCategoria().getId()));
    }

    @Test
    void produtoInexistenteEQuantidadeInvalida() throws Exception {
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessaoAna);
        assertThrows(DadosInvalidosException.class, () -> cen.pedidos.adicionarItem(p, 999, 1, sessaoAna));
        assertThrows(DadosInvalidosException.class, () -> cen.pedidos.adicionarItem(p, fone.getId(), -1, sessaoAna));
        assertThrows(DadosInvalidosException.class, () -> cen.pedidos.criarPedido(999, admin));
    }
}
