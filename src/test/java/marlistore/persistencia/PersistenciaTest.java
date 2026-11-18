package marlistore.persistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import marlistore.model.*;
import marlistore.service.Cenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenciaTest {

    @TempDir
    Path pasta;

    @Test
    void salvarECarregarPreservaEntidades() throws Exception {
        Repositorio<Cliente> repo = new Repositorio<>(pasta.resolve("clientes.dat"), Cliente::fromLinha);
        repo.incluir(new Cliente("Ana; \"A\"", "52998224725", "ana@email.com", "62999990000"));
        repo.salvar();
        Repositorio<Cliente> outro = new Repositorio<>(pasta.resolve("clientes.dat"), Cliente::fromLinha);
        outro.carregar();
        assertEquals(1, outro.tamanho());
        assertEquals("Ana; \"A\"", outro.listar().get(0).getNome());
    }

    @Test
    void gravacaoSeguraNaoDeixaArquivoTemporario() throws Exception {
        Repositorio<Categoria> repo = new Repositorio<>(pasta.resolve("categorias.dat"), Categoria::fromLinha);
        repo.incluir(new Categoria("A"));
        repo.salvar();
        repo.salvar();
        try (var arquivos = Files.list(pasta)) {
            assertTrue(arquivos.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
        assertTrue(Files.exists(pasta.resolve("categorias.dat")));
    }

    @Test
    void linhaIlegivelEhIgnoradaSemImpedirAAbertura() throws Exception {
        Categoria boa = new Categoria("Boa");
        Files.write(pasta.resolve("categorias.dat"),
                List.of(boa.toLinha(), "lixo sem formato", "", "x;y"), StandardCharsets.UTF_8);
        Repositorio<Categoria> repo = new Repositorio<>(pasta.resolve("categorias.dat"), Categoria::fromLinha);
        assertDoesNotThrow(repo::carregar);
        assertEquals(1, repo.tamanho());
        assertEquals("Boa", repo.listar().get(0).getNome());
    }

    @Test
    void arquivoAusenteGeraListaVazia() {
        Repositorio<Categoria> repo = new Repositorio<>(pasta.resolve("nao-existe.dat"), Categoria::fromLinha);
        repo.carregar();
        assertEquals(0, repo.tamanho());
    }

    @Test
    void buscarRemoverEListar() throws Exception {
        Repositorio<Categoria> repo = new Repositorio<>(pasta.resolve("c.dat"), Categoria::fromLinha);
        Categoria a = new Categoria("A");
        repo.incluir(a);
        assertTrue(repo.buscarPorId(a.getId()).isPresent());
        assertNull(repo.obter(-1));
        repo.listar().clear();
        assertEquals(1, repo.tamanho(), "listar devolve cópia");
        assertTrue(repo.remover(a.getId()));
        assertFalse(repo.remover(a.getId()));
    }

    @Test
    void contadoresGravadosImpedemReusoDeIdAposExclusaoKpi04() throws Exception {
        Cenario cen = new Cenario(pasta);
        Set<Integer> usados = new HashSet<>();
        int ultimoExcluido = -1;
        for (int i = 0; i < 50; i++) {
            Categoria c = cen.catalogo.cadastrarCategoria("Cat " + i, List.of());
            assertTrue(usados.add(c.getId()), "ID reaproveitado: " + c.getId());
            if (i % 3 == 0) {
                cen.catalogo.excluirCategoria(c.getId());
                ultimoExcluido = c.getId();
            }
        }
        List<String> linhas = Files.readAllLines(pasta.resolve("contadores.dat"));
        int gravado = linhas.stream().filter(l -> l.startsWith("Categoria="))
                .mapToInt(l -> Integer.parseInt(l.substring(l.indexOf('=') + 1))).findFirst().orElseThrow();
        assertTrue(gravado > ultimoExcluido, "contador gravado deve estar além do último ID excluído");
    }

    @Test
    void cargaRestauraContadorGravadoMesmoComOMaiorIdExcluido() throws Exception {
        Files.writeString(pasta.resolve("contadores.dat"),
                "Categoria=5000\nProduto=lixo\nCliente=7\nPedido=3\nPromocao=2\nOutra=1\nsem-igual\n");
        new BancoDeDados(pasta).carregar();
        assertTrue(Categoria.getProximoId() >= 5000);
        Categoria nova = new Categoria("Nova");
        assertTrue(nova.getId() >= 5000);
    }

    @Test
    void cargaProtegeContadorPeloMaiorIdExistente() throws Exception {
        Categoria c = new Categoria("X");
        Files.write(pasta.resolve("categorias.dat"), List.of(Categoria.fromLinha(c.toLinha()).toLinha()));
        BancoDeDados banco = new BancoDeDados(pasta);
        banco.carregar();
        assertEquals(1, banco.getCategorias().tamanho());
        assertTrue(Categoria.getProximoId() > c.getId());
    }

    @Test
    void sistemaCompletoSobreviveAFecharEReabrirKpi03() throws Exception {
        Cenario cen = new Cenario(pasta);
        Categoria cat = cen.catalogo.cadastrarCategoria("Eletrônicos", List.of("garantia"));
        Produto fone = cen.catalogo.cadastrarProduto("Fone", new BigDecimal("99.90"), 10, cat.getId(), Map.of("garantia", "1 ano"));
        Cliente ana = cen.novoCliente("Ana", Cenario.CPF_1);
        var sessao = cen.sessaoDe(ana);
        Pedido p = cen.pedidos.criarPedido(ana.getId(), sessao);
        cen.pedidos.adicionarItem(p, fone.getId(), 3, sessao);
        cen.pedidos.iniciarFinalizacao(p, sessao);

        Cenario reaberto = new Cenario(pasta);
        Pedido lido = reaberto.banco.getPedidos().obter(p.getId());
        assertEquals(StatusPedido.EM_FINALIZACAO, lido.getStatus());
        assertEquals(1, lido.getItens().size());
        assertEquals(3, lido.getItens().get(0).getQuantidade());
        assertEquals(7, reaberto.banco.getProdutos().obter(fone.getId()).getEstoque());
        assertSame(reaberto.banco.getProdutos().obter(fone.getId()), lido.getItens().get(0).getProduto());
        assertSame(reaberto.banco.getClientes().obter(ana.getId()), lido.getCliente());
        for (String arq : List.of("categorias", "produtos", "clientes", "pedidos", "itens", "contadores")) {
            assertTrue(Files.exists(pasta.resolve(arq + ".dat")), arq + ".dat deveria existir");
        }
        // continua o fluxo no sistema reaberto
        reaberto.pedidos.cancelar(lido, reaberto.sessaoDe(reaberto.banco.getClientes().obter(ana.getId())));
        assertEquals(10, reaberto.banco.getProdutos().obter(fone.getId()).getEstoque());
    }

    @Test
    void itemOrfaoNoArquivoEhIgnoradoComLog() throws IOException {
        Files.writeString(pasta.resolve("itens.dat"), "999;1;Nome;10.00;1;0.00;\n");
        BancoDeDados banco = new BancoDeDados(pasta);
        assertDoesNotThrow(banco::carregar);
        assertEquals(0, banco.getPedidos().tamanho());
    }

    @Test
    void cargaInicialRapidaComMilPedidosKpi08() throws Exception {
        Cenario cen = new Cenario(pasta);
        Categoria cat = cen.catalogo.cadastrarCategoria("C", List.of());
        Produto p = cen.catalogo.cadastrarProduto("P", BigDecimal.TEN, 5, cat.getId(), Map.of());
        Cliente c = cen.novoCliente("Ana", Cenario.CPF_1);
        for (int i = 0; i < 1000; i++) {
            Pedido ped = new Pedido(c, "x");
            ped.adicionarItem(p, 1);
            cen.banco.getPedidos().incluir(ped);
        }
        cen.banco.getPedidos().salvar();
        long t0 = System.nanoTime();
        BancoDeDados banco = new BancoDeDados(pasta);
        banco.carregar();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(1000, banco.getPedidos().tamanho());
        assertTrue(ms < 3000, "carga levou " + ms + " ms");
    }
}
