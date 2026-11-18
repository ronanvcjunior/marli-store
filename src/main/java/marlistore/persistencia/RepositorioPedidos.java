package marlistore.persistencia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import marlistore.model.Cliente;
import marlistore.model.ItemPedido;
import marlistore.model.Pedido;
import marlistore.model.Produto;

/**
 * Repositório de pedidos: grava os pedidos em {@code pedidos.dat} e os itens
 * em {@code itens.dat}. Clientes e produtos devem ser carregados antes, pois as
 * referências são resolvidas por ID.
 */
public class RepositorioPedidos extends Repositorio<Pedido> {

    private static final Logger LOG = Logger.getLogger(RepositorioPedidos.class.getName());

    private final Path arquivoItens;
    private final Function<Integer, Produto> resolverProduto;

    /**
     * @param arquivoPedidos  arquivo dos pedidos
     * @param arquivoItens    arquivo dos itens
     * @param resolverCliente devolve o cliente pelo ID
     * @param resolverProduto devolve o produto pelo ID
     */
    public RepositorioPedidos(Path arquivoPedidos, Path arquivoItens,
                              Function<Integer, Cliente> resolverCliente,
                              Function<Integer, Produto> resolverProduto) {
        super(arquivoPedidos, linha -> Pedido.fromLinha(linha, resolverCliente));
        this.arquivoItens = arquivoItens;
        this.resolverProduto = resolverProduto;
    }

    @Override
    public void carregar() {
        super.carregar();
        if (!Files.exists(arquivoItens)) {
            return;
        }
        int n = 0;
        for (String linha : lerLinhas(arquivoItens)) {
            n++;
            if (linha.isBlank()) {
                continue;
            }
            try {
                int pedidoId = ItemPedido.pedidoIdDaLinha(linha);
                Pedido dono = obter(pedidoId);
                if (dono == null) {
                    throw new IllegalArgumentException("Pedido inexistente: " + pedidoId);
                }
                dono.restaurarItem(ItemPedido.fromLinha(linha, resolverProduto));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Linha ilegível ignorada em {0} (linha {1}): {2}",
                        new Object[]{arquivoItens.getFileName(), n, e.toString()});
            }
        }
    }

    @Override
    public void salvar() {
        List<String> linhasItens = new ArrayList<>();
        for (Pedido p : itens) {
            for (ItemPedido i : p.getItens()) {
                linhasItens.add(i.toLinha(p.getId()));
            }
        }
        gravarSeguro(arquivoItens, linhasItens);
        super.salvar();
    }
}
