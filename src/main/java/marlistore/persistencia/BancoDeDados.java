package marlistore.persistencia;

import java.nio.file.Path;
import marlistore.model.Categoria;
import marlistore.model.Cliente;
import marlistore.model.EntidadeBase;
import marlistore.model.Pedido;
import marlistore.model.Produto;
import marlistore.model.Promocao;

/**
 * Reúne os repositórios de uma pasta de dados ({@code dados/}) e cuida da ordem
 * de carregamento (categorias, produtos, clientes, promoções, pedidos e itens)
 * e dos contadores de ID. O backup do sistema é a cópia dessa pasta (RNF-06).
 */
public class BancoDeDados {

    private final Path pasta;
    private final Repositorio<Categoria> categorias;
    private final Repositorio<Produto> produtos;
    private final Repositorio<Cliente> clientes;
    private final Repositorio<Promocao> promocoes;
    private final RepositorioPedidos pedidos;

    /**
     * Cria os repositórios (sem carregar). Cada gravação também grava {@code contadores.dat}.
     *
     * @param pasta pasta onde ficam os arquivos {@code .dat}
     */
    public BancoDeDados(Path pasta) {
        this.pasta = pasta;
        categorias = new Repositorio<>(pasta.resolve("categorias.dat"), Categoria::fromLinha);
        produtos = new Repositorio<>(pasta.resolve("produtos.dat"),
                linha -> Produto.fromLinha(linha, categorias::obter));
        clientes = new Repositorio<>(pasta.resolve("clientes.dat"), Cliente::fromLinha);
        promocoes = new Repositorio<>(pasta.resolve("promocoes.dat"), Promocao::fromLinha);
        pedidos = new RepositorioPedidos(pasta.resolve("pedidos.dat"), pasta.resolve("itens.dat"),
                clientes::obter, produtos::obter);
        Runnable gravarContadores = () -> Contadores.salvar(pasta.resolve("contadores.dat"));
        categorias.setAposSalvar(gravarContadores);
        produtos.setAposSalvar(gravarContadores);
        clientes.setAposSalvar(gravarContadores);
        promocoes.setAposSalvar(gravarContadores);
        pedidos.setAposSalvar(gravarContadores);
    }

    /**
     * Carrega todos os arquivos na ordem de dependência e ajusta os contadores
     * (o valor gravado em {@code contadores.dat}, nunca abaixo do maior ID existente + 1).
     */
    public void carregar() {
        categorias.carregar();
        produtos.carregar();
        clientes.carregar();
        promocoes.carregar();
        pedidos.carregar();
        Contadores.carregar(pasta.resolve("contadores.dat"));
        Categoria.setProximoId(maiorId(categorias) + 1);
        Produto.setProximoId(maiorId(produtos) + 1);
        Cliente.setProximoId(maiorId(clientes) + 1);
        Promocao.setProximoId(maiorId(promocoes) + 1);
        Pedido.setProximoId(maiorId(pedidos) + 1);
    }

    private static int maiorId(Repositorio<? extends EntidadeBase> r) {
        return r.listar().stream().mapToInt(EntidadeBase::getId).max().orElse(0);
    }

    /** @return pasta de dados */
    public Path getPasta() {
        return pasta;
    }

    /** @return repositório de categorias */
    public Repositorio<Categoria> getCategorias() {
        return categorias;
    }

    /** @return repositório de produtos */
    public Repositorio<Produto> getProdutos() {
        return produtos;
    }

    /** @return repositório de clientes */
    public Repositorio<Cliente> getClientes() {
        return clientes;
    }

    /** @return repositório de promoções */
    public Repositorio<Promocao> getPromocoes() {
        return promocoes;
    }

    /** @return repositório de pedidos (e itens) */
    public RepositorioPedidos getPedidos() {
        return pedidos;
    }
}
