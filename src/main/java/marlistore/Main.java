package marlistore;

import java.nio.file.Path;
import java.nio.file.Paths;
import marlistore.persistencia.BancoDeDados;
import marlistore.service.CatalogoService;
import marlistore.service.ClienteService;
import marlistore.service.MotorDescontos;
import marlistore.service.PedidoService;
import marlistore.service.PromocaoService;
import marlistore.service.RelatorioService;
import marlistore.ui.Entrada;
import marlistore.ui.TelaPrincipal;

/**
 * Ponto de entrada da Marli Store. Cria os repositórios (pasta {@code dados/}) e
 * os injeta nos serviços pelo construtor (injeção de dependência); a interface só
 * conversa com os serviços.
 */
public final class Main {

    private Main() {
    }

    /**
     * Inicia o sistema.
     *
     * @param args argumento opcional: caminho da pasta de dados (padrão: {@code dados})
     */
    public static void main(String[] args) {
        Path pasta = Paths.get(args.length > 0 ? args[0] : "dados");
        try {
            BancoDeDados banco = new BancoDeDados(pasta);
            banco.carregar();
            CatalogoService catalogo = new CatalogoService(banco.getCategorias(), banco.getProdutos(),
                    banco.getPedidos());
            ClienteService clientes = new ClienteService(banco.getClientes(), banco.getPedidos());
            PromocaoService promocoes = new PromocaoService(banco.getPromocoes(), banco.getCategorias(),
                    banco.getProdutos());
            PedidoService pedidos = new PedidoService(banco.getPedidos(), banco.getProdutos(),
                    banco.getClientes(), promocoes, new MotorDescontos());
            RelatorioService relatorios = new RelatorioService(banco.getPedidos());
            new TelaPrincipal(catalogo, clientes, promocoes, pedidos, relatorios).iniciar();
        } catch (RuntimeException e) {
            Entrada.erro("Não foi possível iniciar o sistema. Verifique a pasta " + pasta + ".");
        }
    }
}
