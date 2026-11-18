package marlistore.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import marlistore.model.Pedido;
import marlistore.service.PeriodoPredefinido;
import marlistore.service.RelatorioService;
import marlistore.service.RelatorioVendas;

/** UC13 - Painel de relatórios de vendas do Administrador. */
public class PainelRelatorios {

    private final RelatorioService relatorios;

    /**
     * @param relatorios serviço de relatórios
     */
    public PainelRelatorios(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    /** Pergunta período e filtros opcionais e exibe o relatório. */
    public void exibir() {
        Acao.executar(() -> {
            PeriodoPredefinido[] ps = PeriodoPredefinido.values();
            List<String> nomes = new ArrayList<>();
            for (PeriodoPredefinido p : ps) {
                nomes.add(p.getRotulo());
            }
            int i = Entrada.escolher("Período do relatório:", nomes);
            if (i < 0) {
                return;
            }
            LocalDate ini = null;
            LocalDate fim = null;
            if (ps[i] == PeriodoPredefinido.PERSONALIZADO) {
                ini = Entrada.data("Data inicial", false);
                fim = Entrada.data("Data final", false);
            }
            String cliente = Entrada.texto("Filtrar por nome do cliente (vazio = todos):", "");
            if (cliente == null) {
                return;
            }
            String produto = Entrada.texto("Filtrar por nome do produto (vazio = todos):", "");
            if (produto == null) {
                return;
            }
            RelatorioVendas r = relatorios.gerar(ps[i], ini, fim, cliente, produto);
            StringBuilder sb = new StringBuilder();
            sb.append("Período: ").append(Formatador.data(r.getInicio())).append(" a ")
                    .append(Formatador.data(r.getFim())).append('\n');
            sb.append("Pedidos concluídos: ").append(r.getTotalPedidos()).append('\n');
            sb.append("Total vendido: ").append(Formatador.moeda(r.getTotalVendido())).append('\n');
            sb.append("Total de descontos: ").append(Formatador.moeda(r.getTotalDescontos())).append("\n\n");
            for (Pedido p : r.getPedidos()) {
                sb.append(Formatador.pedidoLinha(p)).append('\n');
            }
            Entrada.mostrarTexto("Relatório de vendas", sb.toString());
        });
    }
}
