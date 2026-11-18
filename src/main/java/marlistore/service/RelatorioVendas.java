package marlistore.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import marlistore.model.Pedido;

/** Resultado de um relatório de vendas (value object). */
public final class RelatorioVendas {

    private final LocalDate inicio;
    private final LocalDate fim;
    private final int totalPedidos;
    private final BigDecimal totalVendido;
    private final BigDecimal totalDescontos;
    private final List<Pedido> pedidos;

    /**
     * @param inicio         primeiro dia do período
     * @param fim            último dia do período
     * @param totalPedidos   quantidade de pedidos
     * @param totalVendido   total vendido (líquido)
     * @param totalDescontos total de descontos concedidos
     * @param pedidos        pedidos considerados
     */
    public RelatorioVendas(LocalDate inicio, LocalDate fim, int totalPedidos, BigDecimal totalVendido,
                           BigDecimal totalDescontos, List<Pedido> pedidos) {
        this.inicio = inicio;
        this.fim = fim;
        this.totalPedidos = totalPedidos;
        this.totalVendido = totalVendido;
        this.totalDescontos = totalDescontos;
        this.pedidos = Collections.unmodifiableList(pedidos);
    }

    /** @return primeiro dia do período */
    public LocalDate getInicio() {
        return inicio;
    }

    /** @return último dia do período */
    public LocalDate getFim() {
        return fim;
    }

    /** @return quantidade de pedidos considerados */
    public int getTotalPedidos() {
        return totalPedidos;
    }

    /** @return total vendido (líquido) */
    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    /** @return total de descontos */
    public BigDecimal getTotalDescontos() {
        return totalDescontos;
    }

    /** @return pedidos considerados */
    public List<Pedido> getPedidos() {
        return pedidos;
    }
}
