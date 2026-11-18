package marlistore.ui;

import java.util.ArrayList;
import java.util.List;
import marlistore.model.Cliente;
import marlistore.model.Pedido;
import marlistore.model.Produto;
import marlistore.model.StatusPedido;
import marlistore.service.CatalogoService;
import marlistore.service.ClienteService;
import marlistore.service.PedidoService;
import marlistore.session.SessaoUsuario;

/**
 * Menu do perfil Cliente (UC03 a UC07). O Administrador reutiliza estas
 * funções (ele herda os casos de uso do Cliente); a diferença está nos dados
 * acessíveis (RN-06). A classe só chama serviços: nenhuma regra de negócio mora aqui.
 */
public class MenuCliente {

    private static final String[] OPCOES = {
        "Consultar catálogo", "Novo pedido", "Meus pedidos (continuar ou revisar)",
        "Cancelar pedido", "Histórico de compras", "Sair"};

    private final SessaoUsuario sessao;
    private final CatalogoService catalogo;
    private final ClienteService clientes;
    private final PedidoService pedidos;

    /**
     * @param sessao   sessão atual
     * @param catalogo serviço de catálogo
     * @param clientes serviço de clientes (o Administrador escolhe o cliente do pedido)
     * @param pedidos  serviço de pedidos
     */
    public MenuCliente(SessaoUsuario sessao, CatalogoService catalogo, ClienteService clientes,
                       PedidoService pedidos) {
        this.sessao = sessao;
        this.catalogo = catalogo;
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    /** Exibe o menu do Cliente até o usuário sair. */
    public void exibir() {
        while (true) {
            int op = Entrada.escolher("Olá, " + sessao.getNome() + "! O que deseja fazer?",
                    List.of(OPCOES));
            if (op < 0 || op == 5) {
                return;
            }
            switch (op) {
                case 0: Acao.executar(this::consultarCatalogo); break;
                case 1: Acao.executar(this::novoPedido); break;
                case 2: Acao.executar(this::abrirPedido); break;
                case 3: Acao.executar(this::cancelarPedido); break;
                default: Acao.executar(this::historico);
            }
        }
    }

    /** UC03 - lista produtos (o Cliente só vê os com estoque) e consulta um por código ou nome. */
    public void consultarCatalogo() throws Exception {
        List<String> modos = List.of("Listar todos", "Filtrar por categoria", "Buscar por código ou nome");
        int m = Entrada.escolher("Consultar catálogo", modos);
        if (m < 0) {
            return;
        }
        List<Produto> lista;
        if (m == 0) {
            lista = produtosVisiveis(null);
        } else if (m == 1) {
            Integer cat = escolherCategoriaId();
            if (cat == null) {
                return;
            }
            lista = produtosVisiveis(cat);
        } else {
            String t = Entrada.textoObrigatorio("Código ou parte do nome:", "");
            if (t == null) {
                return;
            }
            lista = new ArrayList<>();
            if (t.matches("\\d+")) {
                try {
                    Produto p = catalogo.buscarProduto(Integer.parseInt(t));
                    if (sessao.isAdministrador() || p.getEstoque() > 0) {
                        lista.add(p);
                    }
                } catch (marlistore.exception.DadosInvalidosException e) {
                    // cai na busca por nome
                }
            }
            for (Produto p : catalogo.buscarProdutosPorNome(t)) {
                if ((sessao.isAdministrador() || p.getEstoque() > 0) && !lista.contains(p)) {
                    lista.add(p);
                }
            }
        }
        if (lista.isEmpty()) {
            Entrada.mensagem("Nenhum produto encontrado.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Produto p : lista) {
            sb.append(Formatador.produtoDetalhe(p)).append("\n");
        }
        Entrada.mostrarTexto("Catálogo", sb.toString());
    }

    private List<Produto> produtosVisiveis(Integer categoriaId) {
        return sessao.isAdministrador() ? catalogo.listarProdutos(categoriaId)
                : catalogo.listarProdutosDisponiveis(categoriaId);
    }

    private Integer escolherCategoriaId() {
        var cats = catalogo.listarCategorias();
        List<String> nomes = new ArrayList<>();
        cats.forEach(c -> nomes.add(c.getId() + " - " + c.getNome()));
        int i = Entrada.escolher("Categoria:", nomes);
        return i < 0 ? null : cats.get(i).getId();
    }

    /** UC04 - cria um pedido (o Cliente usa o próprio cadastro; o Administrador escolhe o cliente). */
    public void novoPedido() throws Exception {
        int clienteId;
        if (sessao.isAdministrador()) {
            Cliente c = escolherCliente();
            if (c == null) {
                return;
            }
            clienteId = c.getId();
        } else {
            clienteId = sessao.getClienteLogado().getId();
        }
        trabalharPedido(pedidos.criarPedido(clienteId, sessao));
    }

    /** Seleciona um pedido visível ao perfil e abre o fluxo de edição ou revisão. */
    public void abrirPedido() throws Exception {
        Pedido p = escolherPedido(pedidos.listarPedidos(sessao), "Escolha o pedido:");
        if (p != null) {
            trabalharPedido(p);
        }
    }

    private Cliente escolherCliente() {
        List<Cliente> lista = clientes.listar();
        if (lista.isEmpty()) {
            Entrada.erro("Não há clientes cadastrados.");
            return null;
        }
        List<String> textos = new ArrayList<>();
        lista.forEach(c -> textos.add(c.getId() + " - " + c.getNome() + " (CPF " + c.getCpfFormatado() + ")"));
        int i = Entrada.escolher("Cliente:", textos);
        return i < 0 ? null : lista.get(i);
    }

    private Pedido escolherPedido(List<Pedido> lista, String msg) {
        if (lista.isEmpty()) {
            Entrada.mensagem("Nenhum pedido encontrado.");
            return null;
        }
        List<String> textos = new ArrayList<>();
        lista.forEach(p -> textos.add(Formatador.pedidoLinha(p)));
        int i = Entrada.escolher(msg, textos);
        return i < 0 ? null : lista.get(i);
    }

    private void trabalharPedido(Pedido p) throws Exception {
        while (true) {
            StatusPedido st = p.getStatus();
            if (st.isFinal()) {
                Entrada.mostrarTexto("Pedido " + p.getId(), Formatador.pedidoDetalhe(p));
                return;
            }
            String cabecalho = Formatador.pedidoDetalhe(p);
            if (st == StatusPedido.EM_FINALIZACAO) {
                int op = Entrada.escolher("REVISÃO DO PEDIDO\n\n" + cabecalho,
                        List.of("Concluir compra", "Voltar e editar", "Cancelar pedido", "Sair"));
                if (op == 0) {
                    if (Entrada.confirmar("Confirmar a compra?")) {
                        Acao.executar(() -> pedidos.concluir(p, sessao));
                        if (p.getStatus() == StatusPedido.CONCLUIDO) {
                            Entrada.mensagem("Compra concluída! Total: " + Formatador.moeda(p.calcularTotal()));
                        }
                    }
                } else if (op == 1) {
                    Acao.executar(() -> pedidos.voltarParaAndamento(p, sessao));
                } else if (op == 2) {
                    cancelar(p);
                } else {
                    return;
                }
            } else {
                int op = Entrada.escolher(cabecalho + "\n\nO que deseja fazer?",
                        List.of("Adicionar item", "Remover item", "Alterar quantidade", "Finalizar pedido",
                                "Cancelar pedido", "Sair (manter como está)"));
                switch (op) {
                    case 0: Acao.executar(() -> adicionarItem(p)); break;
                    case 1: Acao.executar(() -> removerItem(p)); break;
                    case 2: Acao.executar(() -> alterarQuantidade(p)); break;
                    case 3: Acao.executar(() -> pedidos.iniciarFinalizacao(p, sessao)); break;
                    case 4:
                        if (st == StatusPedido.VAZIO) {
                            return;
                        }
                        cancelar(p);
                        break;
                    default: return;
                }
            }
        }
    }

    private void adicionarItem(Pedido p) throws Exception {
        List<Produto> lista = produtosVisiveis(null);
        if (lista.isEmpty()) {
            Entrada.erro("Não há produtos disponíveis.");
            return;
        }
        List<String> textos = new ArrayList<>();
        lista.forEach(x -> textos.add(Formatador.produtoLinha(x)));
        int i = Entrada.escolher("Produto:", textos);
        if (i < 0) {
            return;
        }
        Integer qtd = Entrada.inteiro("Quantidade:", 1);
        if (qtd == null) {
            return;
        }
        pedidos.adicionarItem(p, lista.get(i).getId(), qtd, sessao);
    }

    private int escolherItem(Pedido p, String msg) {
        List<String> textos = new ArrayList<>();
        int n = 1;
        for (var item : p.getItens()) {
            textos.add(n++ + ". " + item.getNomeProduto() + " x " + item.getQuantidade());
        }
        return Entrada.escolher(msg, textos);
    }

    private void removerItem(Pedido p) throws Exception {
        int i = escolherItem(p, "Item a remover:");
        if (i >= 0) {
            pedidos.removerItem(p, i, sessao);
        }
    }

    private void alterarQuantidade(Pedido p) throws Exception {
        int i = escolherItem(p, "Item a alterar:");
        if (i < 0) {
            return;
        }
        Integer qtd = Entrada.inteiro("Nova quantidade:", p.getItens().get(i).getQuantidade());
        if (qtd != null) {
            pedidos.alterarQuantidade(p, i, qtd, sessao);
        }
    }

    private void cancelar(Pedido p) {
        if (Entrada.confirmar("Cancelar o pedido " + p.getId() + "? Esta ação não pode ser desfeita.")) {
            Acao.executar(() -> {
                pedidos.cancelar(p, sessao);
                Entrada.mensagem("Pedido cancelado.");
            });
        }
    }

    /** UC06 - cancela um pedido em andamento ou em finalização, com confirmação. */
    public void cancelarPedido() throws Exception {
        List<Pedido> cancelaveis = new ArrayList<>();
        for (Pedido p : pedidos.listarPedidos(sessao)) {
            if (p.getStatus() == StatusPedido.EM_ANDAMENTO || p.getStatus() == StatusPedido.EM_FINALIZACAO) {
                cancelaveis.add(p);
            }
        }
        Pedido p = escolherPedido(cancelaveis, "Pedido a cancelar:");
        if (p != null) {
            cancelar(p);
        }
    }

    /** UC07 - histórico de compras (o Cliente vê o próprio; o Administrador escolhe o cliente). */
    public void historico() throws Exception {
        int clienteId;
        if (sessao.isAdministrador()) {
            Cliente c = escolherCliente();
            if (c == null) {
                return;
            }
            clienteId = c.getId();
        } else {
            clienteId = sessao.getClienteLogado().getId();
        }
        Pedido p = escolherPedido(pedidos.historicoDoCliente(clienteId, sessao), "Histórico - escolha um pedido:");
        if (p != null) {
            Entrada.mostrarTexto("Pedido " + p.getId(), Formatador.pedidoDetalhe(p));
        }
    }

    /** UC13 (parte) - lista todos os pedidos visíveis ao perfil, com status. */
    public void listarPedidos() {
        List<Pedido> lista = pedidos.listarPedidos(sessao);
        if (lista.isEmpty()) {
            Entrada.mensagem("Nenhum pedido encontrado.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        lista.forEach(p -> sb.append(Formatador.pedidoLinha(p)).append('\n'));
        Entrada.mostrarTexto("Pedidos", sb.toString());
    }
}
