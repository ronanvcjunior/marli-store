package marlistore.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import marlistore.exception.OperacaoNaoPermitidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private Produto fone;
    private Produto cabo;
    private Pedido pedido;

    @BeforeEach
    void preparar() throws Exception {
        Categoria c = new Categoria("Eletrônicos");
        fone = new Produto("Fone", new BigDecimal("100.00"), 10, c);
        cabo = new Produto("Cabo", new BigDecimal("20.00"), 5, c);
        Cliente cli = new Cliente("Ana", "52998224725", "ana@email.com", "62999990000");
        pedido = new Pedido(cli, "Ana");
    }

    @Test
    void nasceVazioEPrimeiroItemMudaParaEmAndamento() throws Exception {
        assertEquals(StatusPedido.VAZIO, pedido.getStatus());
        pedido.adicionarItem(fone, 2);
        assertEquals(StatusPedido.EM_ANDAMENTO, pedido.getStatus());
        assertEquals(new BigDecimal("200.00"), pedido.calcularTotal());
    }

    @Test
    void itensRepetidosSaoSomadosEEstoqueConsideraASoma() throws Exception {
        pedido.adicionarItem(fone, 6);
        pedido.adicionarItem(fone, 3);
        assertEquals(1, pedido.getItens().size());
        assertEquals(9, pedido.getItens().get(0).getQuantidade());
        EstoqueInsuficienteException e = assertThrows(EstoqueInsuficienteException.class,
                () -> pedido.adicionarItem(fone, 2));
        assertEquals(10, e.getDisponivel());
        assertEquals(11, e.getSolicitado());
        assertEquals(9, pedido.getItens().get(0).getQuantidade(), "item não é alterado após a falha");
    }

    @Test
    void primeiroItemInvalidoMantemPedidoVazio() {
        assertThrows(EstoqueInsuficienteException.class, () -> pedido.adicionarItem(fone, 11));
        assertEquals(StatusPedido.VAZIO, pedido.getStatus());
        assertThrows(DadosInvalidosException.class, () -> pedido.adicionarItem(fone, 0));
    }

    @Test
    void removerEAlterarQuantidade() throws Exception {
        pedido.adicionarItem(fone, 2);
        pedido.adicionarItem(cabo, 1);
        pedido.alterarQuantidade(1, 4);
        assertEquals(4, pedido.getItens().get(1).getQuantidade());
        assertThrows(EstoqueInsuficienteException.class, () -> pedido.alterarQuantidade(1, 6));
        pedido.removerItem(0);
        assertEquals(1, pedido.getItens().size());
        assertThrows(DadosInvalidosException.class, () -> pedido.removerItem(5));
    }

    @Test
    void finalizarBaixaEstoqueEConcluirNaoAltera() throws Exception {
        pedido.adicionarItem(fone, 4);
        pedido.adicionarItem(cabo, 5);
        pedido.iniciarFinalizacao();
        assertEquals(StatusPedido.EM_FINALIZACAO, pedido.getStatus());
        assertEquals(6, fone.getEstoque());
        assertEquals(0, cabo.getEstoque());
        pedido.concluir();
        assertEquals(StatusPedido.CONCLUIDO, pedido.getStatus());
        assertNotNull(pedido.getDataConclusao());
        assertEquals(6, fone.getEstoque());
        assertEquals(0, cabo.getEstoque());
    }

    @Test
    void finalizarEhTudoOuNada() throws Exception {
        pedido.adicionarItem(fone, 4);
        pedido.adicionarItem(cabo, 5);
        cabo.baixarEstoque(2); // outro pedido consumiu saldo
        EstoqueInsuficienteException e = assertThrows(EstoqueInsuficienteException.class, pedido::iniciarFinalizacao);
        assertTrue(e.getMessage().contains("Cabo"));
        assertEquals(10, fone.getEstoque(), "nada foi baixado");
        assertEquals(3, cabo.getEstoque());
        assertEquals(StatusPedido.EM_ANDAMENTO, pedido.getStatus());
    }

    @Test
    void voltarParaAndamentoDevolveEstoque() throws Exception {
        pedido.adicionarItem(fone, 4);
        pedido.iniciarFinalizacao();
        pedido.voltarParaAndamento();
        assertEquals(10, fone.getEstoque());
        assertEquals(StatusPedido.EM_ANDAMENTO, pedido.getStatus());
        pedido.alterarQuantidade(0, 5); // volta a poder editar
    }

    @Test
    void cancelarAposBaixaDevolveEstoqueEAntesNao() throws Exception {
        pedido.adicionarItem(fone, 4);
        pedido.iniciarFinalizacao();
        pedido.cancelar();
        assertEquals(10, fone.getEstoque());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertNotNull(pedido.getDataCancelamento());

        Pedido outro = new Pedido(pedido.getCliente(), "Ana");
        outro.adicionarItem(fone, 4);
        outro.cancelar();
        assertEquals(10, fone.getEstoque(), "sem baixa, nada a devolver");
    }

    @Test
    void estadosFinaisEFluxoInvalidoSaoBloqueados() throws Exception {
        assertThrows(OperacaoNaoPermitidaException.class, pedido::cancelar, "vazio não cancela");
        assertThrows(OperacaoNaoPermitidaException.class, pedido::iniciarFinalizacao);
        pedido.adicionarItem(fone, 1);
        assertThrows(OperacaoNaoPermitidaException.class, pedido::concluir, "precisa finalizar antes");
        assertThrows(OperacaoNaoPermitidaException.class, pedido::voltarParaAndamento);
        pedido.iniciarFinalizacao();
        assertThrows(OperacaoNaoPermitidaException.class, () -> pedido.adicionarItem(cabo, 1));
        assertThrows(OperacaoNaoPermitidaException.class, () -> pedido.removerItem(0));
        pedido.concluir();
        assertThrows(OperacaoNaoPermitidaException.class, pedido::cancelar);
        assertThrows(OperacaoNaoPermitidaException.class, pedido::iniciarFinalizacao);
    }

    @Test
    void naoFinalizaSemItens() throws Exception {
        pedido.adicionarItem(fone, 1);
        pedido.removerItem(0);
        assertThrows(OperacaoNaoPermitidaException.class, pedido::iniciarFinalizacao);
    }

    @Test
    void precoCongeladoNoItem() throws Exception {
        pedido.adicionarItem(fone, 1);
        fone.setPreco(new BigDecimal("999.00"));
        fone.setNome("Outro nome");
        assertEquals(new BigDecimal("100.00"), pedido.getItens().get(0).getPrecoUnitario());
        assertEquals("Fone", pedido.getItens().get(0).getNomeProduto());
    }

    @Test
    void idaEVoltaDoPedidoEDosItens() throws Exception {
        pedido.adicionarItem(fone, 3);
        pedido.getItens().get(0).aplicarDesconto(new ResultadoDesconto(new BigDecimal("30.00"),
                new Promocao("Natal", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL,
                        BigDecimal.TEN, null, null)));
        pedido.iniciarFinalizacao();
        pedido.concluir(LocalDateTime.of(2026, 11, 1, 10, 30));

        Pedido lido = Pedido.fromLinha(pedido.toLinha(), id -> id == pedido.getCliente().getId() ? pedido.getCliente() : null);
        lido.restaurarItem(ItemPedido.fromLinha(pedido.getItens().get(0).toLinha(pedido.getId()),
                id -> id == fone.getId() ? fone : null));
        assertEquals(pedido.getId(), lido.getId());
        assertEquals(StatusPedido.CONCLUIDO, lido.getStatus());
        assertEquals(LocalDateTime.of(2026, 11, 1, 10, 30), lido.getDataConclusao());
        assertEquals(new BigDecimal("270.00"), lido.calcularTotal());
        assertEquals("Natal", lido.getItens().get(0).getPromocaoAplicada());
        assertEquals(pedido.getId(), ItemPedido.pedidoIdDaLinha(pedido.getItens().get(0).toLinha(pedido.getId())));
    }
}
