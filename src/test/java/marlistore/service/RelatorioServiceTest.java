package marlistore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Hoje, no cenário, é 17/11/2026. */
class RelatorioServiceTest {

    @TempDir
    Path pasta;
    Cenario cen;
    Produto fone;
    Produto cabo;
    Cliente ana;
    Cliente bia;

    private Pedido vender(Cliente c, Produto p, int qtd, LocalDateTime quando) throws Exception {
        Pedido ped = new Pedido(c, "Marli", quando);
        ped.adicionarItem(p, qtd);
        if (qtd >= 2) {
            ped.getItens().get(0).aplicarDesconto(new ResultadoDesconto(new BigDecimal("10.00"), null));
        }
        ped.iniciarFinalizacao();
        ped.concluir(quando);
        cen.banco.getPedidos().incluir(ped);
        return ped;
    }

    @BeforeEach
    void preparar() throws Exception {
        cen = new Cenario(pasta);
        Categoria cat = cen.catalogo.cadastrarCategoria("Eletrônicos", List.of());
        fone = cen.catalogo.cadastrarProduto("Fone", new BigDecimal("100"), 100, cat.getId(), Map.of());
        cabo = cen.catalogo.cadastrarProduto("Cabo", new BigDecimal("20"), 100, cat.getId(), Map.of());
        ana = cen.novoCliente("Ana Souza", Cenario.CPF_1);
        bia = cen.novoCliente("Bia Lima", Cenario.CPF_2);
        vender(ana, fone, 2, LocalDateTime.of(2026, 11, 10, 9, 0));   // 190
        vender(bia, cabo, 1, LocalDateTime.of(2026, 11, 2, 9, 0));    // 20
        vender(ana, cabo, 3, LocalDateTime.of(2026, 9, 1, 9, 0));     // 50
        vender(bia, fone, 1, LocalDateTime.of(2025, 12, 1, 9, 0));    // 100
        vender(bia, fone, 1, LocalDateTime.of(2025, 10, 1, 9, 0));    // fora do último ano
        Pedido aberto = new Pedido(ana, "Marli", LocalDateTime.of(2026, 11, 12, 9, 0));
        aberto.adicionarItem(fone, 1);
        cen.banco.getPedidos().incluir(aberto);                      // não concluído: nunca entra
    }

    @Test
    void periodosPredefinidos() throws Exception {
        RelatorioVendas mes = cen.relatorios.gerar(PeriodoPredefinido.MES_ATUAL, null, null, "", "");
        assertEquals(2, mes.getTotalPedidos());
        assertEquals(new BigDecimal("210.00"), mes.getTotalVendido());
        assertEquals(new BigDecimal("10.00"), mes.getTotalDescontos());
        assertEquals(LocalDate.of(2026, 11, 1), mes.getInicio());

        assertEquals(2, cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_MES, null, null, null, null).getTotalPedidos());
        assertEquals(3, cen.relatorios.gerar(PeriodoPredefinido.ULTIMOS_6_MESES, null, null, null, null).getTotalPedidos());
        RelatorioVendas ano = cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_ANO, null, null, null, null);
        assertEquals(4, ano.getTotalPedidos());
        assertEquals(new BigDecimal("360.00"), ano.getTotalVendido());
    }

    @Test
    void datasLivresComValidacao() throws Exception {
        RelatorioVendas r = cen.relatorios.gerar(PeriodoPredefinido.PERSONALIZADO,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2), null, null);
        assertEquals(1, r.getTotalPedidos());
        assertThrows(DadosInvalidosException.class, () -> cen.relatorios.gerar(PeriodoPredefinido.PERSONALIZADO,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null, null));
        assertThrows(DadosInvalidosException.class,
                () -> cen.relatorios.gerar(PeriodoPredefinido.PERSONALIZADO, null, null, null, null));
    }

    @Test
    void filtrosPorClienteEProduto() throws Exception {
        RelatorioVendas porCliente = cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_ANO, null, null, "ana", "");
        assertEquals(2, porCliente.getTotalPedidos());
        assertEquals(new BigDecimal("240.00"), porCliente.getTotalVendido());

        RelatorioVendas porProduto = cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_ANO, null, null, "", "cabo");
        assertEquals(2, porProduto.getTotalPedidos());
        assertEquals(new BigDecimal("70.00"), porProduto.getTotalVendido());

        RelatorioVendas ambos = cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_ANO, null, null, "bia", "fone");
        assertEquals(1, ambos.getTotalPedidos());
        assertEquals(new BigDecimal("100.00"), ambos.getTotalVendido());
    }

    @Test
    void desempenhoKpi08() throws Exception {
        for (int i = 0; i < 1000; i++) {
            Pedido p = new Pedido(ana, "Marli", LocalDateTime.of(2026, 10, 1, 8, 0));
            p.adicionarItem(cabo, 1);
            p.iniciarFinalizacao();
            p.concluir(LocalDateTime.of(2026, 10, 1, 8, 0));
            cen.banco.getPedidos().incluir(p);
            cabo.reporEstoque(1);
        }
        long t0 = System.nanoTime();
        RelatorioVendas r = cen.relatorios.gerar(PeriodoPredefinido.ULTIMO_ANO, null, null, "ana", "cabo");
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(r.getTotalPedidos() >= 1000);
        assertTrue(ms < 2000, "relatório levou " + ms + " ms");
    }
}
