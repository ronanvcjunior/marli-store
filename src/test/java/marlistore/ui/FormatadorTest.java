package marlistore.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import marlistore.model.*;
import org.junit.jupiter.api.Test;

class FormatadorTest {

    @Test
    void valoresEDatasNoPadraoBrasileiro() {
        assertEquals("R$ 0,00", Formatador.moeda(BigDecimal.ZERO));
        assertEquals("R$ 1.299,90", Formatador.moeda(new BigDecimal("1299.9")));
        assertEquals("06/11/2026", Formatador.data(LocalDate.of(2026, 11, 6)));
        assertEquals("06/11/2026 14:05", Formatador.dataHora(LocalDateTime.of(2026, 11, 6, 14, 5)));
        assertEquals("-", Formatador.data(null));
    }

    @Test
    void detalheDoPedidoMostraItensDescontosETotal() throws Exception {
        Produto p = new Produto("Fone", new BigDecimal("100"), 10, new Categoria("E"));
        Cliente c = new Cliente("Ana", "52998224725", "a@b.com", "62999990000");
        Pedido ped = new Pedido(c, "Ana");
        ped.adicionarItem(p, 3);
        ped.getItens().get(0).aplicarDesconto(new ResultadoDesconto(new BigDecimal("30"),
                new Promocao("Natal", EscopoPromocao.TODOS, null, 1, 1, 1, TipoDesconto.PERCENTUAL, BigDecimal.TEN, null, null)));
        String t = Formatador.pedidoDetalhe(ped);
        assertTrue(t.contains("Fone - 3 x R$ 100,00 = R$ 300,00"));
        assertTrue(t.contains("desconto R$ 30,00 (Natal)"));
        assertTrue(t.contains("Total: R$ 270,00"));
        assertTrue(Formatador.pedidoLinha(ped).contains("Em andamento"));
    }
}
