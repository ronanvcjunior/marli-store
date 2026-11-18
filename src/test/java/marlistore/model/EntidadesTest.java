package marlistore.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.Test;

class EntidadesTest {

    @Test
    void idsSaoSequenciaisEInvalidosNaoConsomemContador() throws Exception {
        Categoria a = new Categoria("A");
        assertThrows(DadosInvalidosException.class, () -> new Categoria(" "));
        Categoria b = new Categoria("B");
        assertEquals(a.getId() + 1, b.getId());
    }

    @Test
    void setProximoIdNuncaRecua() throws Exception {
        Categoria a = new Categoria("A");
        Categoria.setProximoId(1);
        assertEquals(a.getId() + 1, new Categoria("B").getId());
        Categoria.setProximoId(a.getId() + 100);
        assertTrue(Categoria.getProximoId() >= a.getId() + 100);
    }

    @Test
    void categoriaCamposEspecificos() throws Exception {
        Categoria c = new Categoria("Eletrônicos");
        c.adicionarCampo("garantia");
        assertThrows(DadosInvalidosException.class, () -> c.adicionarCampo("GARANTIA"));
        c.adicionarCampo("voltagem");
        c.removerCampo("garantia");
        assertEquals(java.util.List.of("voltagem"), c.getCamposEspecificos());
    }

    @Test
    void categoriaIdaEVolta() throws Exception {
        Categoria c = new Categoria("Casa; e jardim");
        c.adicionarCampo("cor, tom");
        c.adicionarCampo("material");
        int proximo = Categoria.getProximoId();
        Categoria lida = Categoria.fromLinha(c.toLinha());
        assertEquals(c.getId(), lida.getId());
        assertEquals("Casa; e jardim", lida.getNome());
        assertEquals(c.getCamposEspecificos(), lida.getCamposEspecificos());
        assertEquals(proximo, Categoria.getProximoId(), "fromLinha não consome o contador");
    }

    @Test
    void produtoValidaEntradas() throws Exception {
        Categoria c = new Categoria("X");
        assertThrows(DadosInvalidosException.class, () -> new Produto("P", BigDecimal.ZERO, 1, c));
        assertThrows(DadosInvalidosException.class, () -> new Produto("P", BigDecimal.TEN, -1, c));
        assertThrows(DadosInvalidosException.class, () -> new Produto("P", BigDecimal.TEN, 1, null));
        assertThrows(DadosInvalidosException.class, () -> new Produto("", BigDecimal.TEN, 1, c));
    }

    @Test
    void estoqueNuncaFicaNegativoEMensagemEClara() throws Exception {
        Produto p = new Produto("Fone", new BigDecimal("10"), 3, new Categoria("X"));
        EstoqueInsuficienteException e = assertThrows(EstoqueInsuficienteException.class, () -> p.baixarEstoque(5));
        assertEquals("O produto Fone tem apenas 3 unidade(s) em estoque; não é possível vender 5.", e.getMessage());
        assertEquals(3, p.getEstoque());
        p.baixarEstoque(3);
        assertEquals(0, p.getEstoque());
        assertThrows(DadosInvalidosException.class, () -> p.reporEstoque(0));
        p.reporEstoque(4);
        assertEquals(4, p.getEstoque());
    }

    @Test
    void produtoIdaEVoltaComCaracteristicas() throws Exception {
        Categoria c = new Categoria("Eletro");
        Produto p = new Produto("TV; 4K", new BigDecimal("1999.9"), 7, c);
        p.setCaracteristicas(Map.of("garantia", "12 meses, total"));
        Produto lido = Produto.fromLinha(p.toLinha(), id -> id == c.getId() ? c : null);
        assertEquals("TV; 4K", lido.getNome());
        assertEquals(new BigDecimal("1999.90"), lido.getPreco());
        assertEquals(7, lido.getEstoque());
        assertEquals("12 meses, total", lido.getCaracteristicas().get("garantia"));
        assertThrows(IllegalArgumentException.class, () -> Produto.fromLinha(p.toLinha(), id -> null));
    }

    @Test
    void clienteValidaCpfEmailETelefone() throws Exception {
        Cliente c = new Cliente("Ana", "529.982.247-25", "ana@email.com", "(62) 99999-0000");
        assertEquals("52998224725", c.getCpf());
        assertEquals("529.982.247-25", c.getCpfFormatado());
        assertTrue(Cliente.cpfValido("52998224725"));
        assertThrows(DadosInvalidosException.class, () -> new Cliente("Ana", "123", "ana@email.com", "62999990000"));
        assertThrows(DadosInvalidosException.class, () -> new Cliente("Ana", "52998224725", "sem-arroba", "62999990000"));
        assertThrows(DadosInvalidosException.class, () -> new Cliente("Ana", "52998224725", "a@b.com", "12"));
    }

    @Test
    void clienteAtualizarNaoAlteraNadaSeHouverErro() throws Exception {
        Cliente c = new Cliente("Ana", "52998224725", "ana@email.com", "62999990000");
        assertThrows(DadosInvalidosException.class, () -> c.atualizar("Beatriz", "52998224725", "ruim", "62999990000"));
        assertEquals("Ana", c.getNome());
        Cliente lido = Cliente.fromLinha(c.toLinha());
        assertEquals(c.getId(), lido.getId());
        assertEquals("ana@email.com", lido.getEmail());
    }

    @Test
    void statusSoPermiteTransicoesDoFluxo() {
        assertTrue(StatusPedido.VAZIO.podeIrPara(StatusPedido.EM_ANDAMENTO));
        assertFalse(StatusPedido.VAZIO.podeIrPara(StatusPedido.CANCELADO));
        assertTrue(StatusPedido.EM_ANDAMENTO.podeIrPara(StatusPedido.EM_FINALIZACAO));
        assertTrue(StatusPedido.EM_ANDAMENTO.podeIrPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.EM_ANDAMENTO.podeIrPara(StatusPedido.CONCLUIDO));
        assertTrue(StatusPedido.EM_FINALIZACAO.podeIrPara(StatusPedido.EM_ANDAMENTO));
        assertTrue(StatusPedido.EM_FINALIZACAO.podeIrPara(StatusPedido.CONCLUIDO));
        assertTrue(StatusPedido.EM_FINALIZACAO.podeIrPara(StatusPedido.CANCELADO));
        for (StatusPedido destino : StatusPedido.values()) {
            assertFalse(StatusPedido.CONCLUIDO.podeIrPara(destino));
            assertFalse(StatusPedido.CANCELADO.podeIrPara(destino));
        }
    }
}
