package marlistore.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import marlistore.model.Cliente;
import marlistore.model.ItemPedido;
import marlistore.model.Moeda;
import marlistore.model.Pedido;
import marlistore.model.Produto;

/** Formatação de textos para exibição: valores como {@code R$ 0,00} e datas como {@code dd/mm/aaaa} (RNF-09). */
public final class Formatador {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatador() {
    }

    /**
     * @param v valor
     * @return texto como {@code R$ 1.234,50}
     */
    public static String moeda(BigDecimal v) {
        return Moeda.formatar(v);
    }

    /**
     * @param d data (pode ser nula)
     * @return texto dd/mm/aaaa, ou "-" se nula
     */
    public static String data(LocalDate d) {
        return d == null ? "-" : d.format(DATA);
    }

    /**
     * @param d data e hora (pode ser nula)
     * @return texto dd/mm/aaaa hh:mm, ou "-" se nula
     */
    public static String dataHora(LocalDateTime d) {
        return d == null ? "-" : d.format(DATA_HORA);
    }

    /**
     * @param p produto
     * @return linha de listagem com código, nome, preço, estoque e categoria
     */
    public static String produtoLinha(Produto p) {
        return p.getId() + " - " + p.getNome() + " | " + moeda(p.getPreco()) + " | estoque: " + p.getEstoque()
                + " | " + p.getCategoria().getNome();
    }

    /**
     * @param p produto
     * @return texto detalhado, incluindo os campos específicos
     */
    public static String produtoDetalhe(Produto p) {
        StringBuilder sb = new StringBuilder(produtoLinha(p));
        for (Map.Entry<String, String> e : p.getCaracteristicas().entrySet()) {
            sb.append("\n   ").append(e.getKey()).append(": ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * @param c cliente
     * @return linha de listagem do cliente
     */
    public static String clienteLinha(Cliente c) {
        return c.getId() + " - " + c.getNome() + " | CPF " + c.getCpfFormatado() + " | " + c.getEmail()
                + " | " + c.getTelefone();
    }

    /**
     * @param p pedido
     * @return linha de listagem com código, data, cliente, status e total
     */
    public static String pedidoLinha(Pedido p) {
        return "Pedido " + p.getId() + " | " + dataHora(p.getDataCriacao()) + " | " + p.getCliente().getNome()
                + " | " + p.getStatus().getRotulo() + " | " + moeda(p.calcularTotal());
    }

    /**
     * @param p pedido
     * @return texto com itens, descontos, totais e datas
     */
    public static String pedidoDetalhe(Pedido p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pedido ").append(p.getId()).append(" - ").append(p.getStatus().getRotulo()).append('\n');
        sb.append("Cliente: ").append(p.getCliente().getNome()).append('\n');
        sb.append("Criado em ").append(dataHora(p.getDataCriacao())).append(" por ")
                .append(p.getUsuarioSessao()).append('\n');
        if (p.getDataConclusao() != null) {
            sb.append("Concluído em ").append(dataHora(p.getDataConclusao())).append('\n');
        }
        if (p.getDataCancelamento() != null) {
            sb.append("Cancelado em ").append(dataHora(p.getDataCancelamento())).append('\n');
        }
        sb.append("\nItens:\n");
        int n = 1;
        for (ItemPedido i : p.getItens()) {
            sb.append(n++).append(". ").append(i.getNomeProduto()).append(" - ").append(i.getQuantidade())
                    .append(" x ").append(moeda(i.getPrecoUnitario()))
                    .append(" = ").append(moeda(i.calcularSubtotalBruto()));
            if (i.getDescontoAplicado().signum() > 0) {
                sb.append("\n     desconto ").append(moeda(i.getDescontoAplicado()))
                        .append(" (").append(i.getPromocaoAplicada()).append(") -> ")
                        .append(moeda(i.calcularSubtotalLiquido()));
            }
            sb.append('\n');
        }
        if (p.getItens().isEmpty()) {
            sb.append("(sem itens)\n");
        }
        sb.append("\nTotal bruto: ").append(moeda(p.calcularTotalBruto()));
        sb.append("\nDescontos: ").append(moeda(p.calcularTotalDescontos()));
        sb.append("\nTotal: ").append(moeda(p.calcularTotal()));
        return sb.toString();
    }
}
