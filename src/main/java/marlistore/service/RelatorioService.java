package marlistore.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.ItemPedido;
import marlistore.model.Pedido;
import marlistore.model.StatusPedido;
import marlistore.persistencia.Repositorio;

/**
 * Relatório de vendas por período (RF-17, RF-18). Considera os pedidos
 * CONCLUIDO, pela data de conclusão. Com filtro de produto, os totais somam
 * apenas os itens que correspondem ao filtro.
 */
public class RelatorioService {

    private final Repositorio<Pedido> pedidos;
    private final Clock clock;

    /**
     * @param pedidos repositório de pedidos
     * @param clock   relógio (injetável para testes)
     */
    public RelatorioService(Repositorio<Pedido> pedidos, Clock clock) {
        this.pedidos = pedidos;
        this.clock = clock;
    }

    /**
     * @param pedidos repositório de pedidos
     */
    public RelatorioService(Repositorio<Pedido> pedidos) {
        this(pedidos, Clock.systemDefaultZone());
    }

    /**
     * Gera o relatório.
     *
     * @param periodo      período predefinido (ou {@link PeriodoPredefinido#PERSONALIZADO})
     * @param inicio       início, usado só no período personalizado
     * @param fim          fim, usado só no período personalizado
     * @param nomeCliente  filtro por nome do cliente (nulo ou vazio = sem filtro)
     * @param nomeProduto  filtro por nome do produto (nulo ou vazio = sem filtro)
     * @return o relatório
     * @throws DadosInvalidosException se as datas livres faltarem ou o fim for anterior ao início
     */
    public RelatorioVendas gerar(PeriodoPredefinido periodo, LocalDate inicio, LocalDate fim,
                                 String nomeCliente, String nomeProduto) throws DadosInvalidosException {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate de;
        LocalDate ate = hoje;
        switch (periodo) {
            case ULTIMO_ANO: de = hoje.minusYears(1); break;
            case ULTIMOS_6_MESES: de = hoje.minusMonths(6); break;
            case ULTIMO_MES: de = hoje.minusMonths(1); break;
            case MES_ATUAL: de = hoje.withDayOfMonth(1); break;
            default:
                if (inicio == null || fim == null) {
                    throw new DadosInvalidosException("Informe a data inicial e a data final.");
                }
                if (fim.isBefore(inicio)) {
                    throw new DadosInvalidosException("A data final não pode ser anterior à inicial.");
                }
                de = inicio;
                ate = fim;
        }
        String fc = CatalogoService.normalizar(nomeCliente);
        String fp = CatalogoService.normalizar(nomeProduto);
        List<Pedido> considerados = new ArrayList<>();
        BigDecimal vendido = BigDecimal.ZERO.setScale(2);
        BigDecimal descontos = BigDecimal.ZERO.setScale(2);
        for (Pedido p : pedidos.listar()) {
            if (p.getStatus() != StatusPedido.CONCLUIDO || p.getDataConclusao() == null) {
                continue;
            }
            LocalDate d = p.getDataConclusao().toLocalDate();
            if (d.isBefore(de) || d.isAfter(ate)) {
                continue;
            }
            if (!fc.isEmpty() && !CatalogoService.normalizar(p.getCliente().getNome()).contains(fc)) {
                continue;
            }
            if (fp.isEmpty()) {
                considerados.add(p);
                vendido = vendido.add(p.calcularTotal());
                descontos = descontos.add(p.calcularTotalDescontos());
            } else {
                boolean achou = false;
                for (ItemPedido i : p.getItens()) {
                    if (CatalogoService.normalizar(i.getNomeProduto()).contains(fp)) {
                        achou = true;
                        vendido = vendido.add(i.calcularSubtotalLiquido());
                        descontos = descontos.add(i.getDescontoAplicado());
                    }
                }
                if (achou) {
                    considerados.add(p);
                }
            }
        }
        return new RelatorioVendas(de, ate, considerados.size(), vendido, descontos, considerados);
    }
}
