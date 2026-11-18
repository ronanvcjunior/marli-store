package marlistore.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import marlistore.model.Categoria;
import marlistore.model.Cliente;
import marlistore.model.EscopoPromocao;
import marlistore.model.Produto;
import marlistore.model.Promocao;
import marlistore.model.TipoDesconto;
import marlistore.service.CatalogoService;
import marlistore.service.ClienteService;
import marlistore.service.PromocaoService;
import marlistore.session.SessaoUsuario;

/**
 * Menu do Administrador (UC03 a UC13). Reaproveita o {@link MenuCliente} para as
 * funções de pedidos e oferece cadastros, promoções e relatórios.
 */
public class MenuAdmin {

    private static final List<String> OPCOES = List.of(
            "Catálogo, pedidos e histórico (funções do Cliente)", "Categorias", "Produtos",
            "Repor estoque", "Clientes", "Promoções", "Todos os pedidos", "Relatórios de vendas", "Sair");
    private static final List<String> CRUD = List.of("Cadastrar", "Alterar", "Excluir", "Consultar", "Listar");

    private final SessaoUsuario sessao;
    private final MenuCliente menuCliente;
    private final CatalogoService catalogo;
    private final ClienteService clientes;
    private final PromocaoService promocoes;
    private final PainelRelatorios painel;

    /**
     * @param sessao      sessão atual
     * @param menuCliente menu do Cliente, reaproveitado
     * @param catalogo    serviço de catálogo
     * @param clientes    serviço de clientes
     * @param promocoes   serviço de promoções
     * @param painel      painel de relatórios
     */
    public MenuAdmin(SessaoUsuario sessao, MenuCliente menuCliente, CatalogoService catalogo,
                     ClienteService clientes, PromocaoService promocoes, PainelRelatorios painel) {
        this.sessao = sessao;
        this.menuCliente = menuCliente;
        this.catalogo = catalogo;
        this.clientes = clientes;
        this.promocoes = promocoes;
        this.painel = painel;
    }

    /** Exibe o menu do Administrador até o usuário sair. */
    public void exibir() {
        while (true) {
            int op = Entrada.escolher("Administrador: " + sessao.getNome(), OPCOES);
            if (op < 0 || op == 8) {
                return;
            }
            switch (op) {
                case 0: menuCliente.exibir(); break;
                case 1: Acao.executar(this::menuCategorias); break;
                case 2: Acao.executar(this::menuProdutos); break;
                case 3: Acao.executar(this::reporEstoque); break;
                case 4: Acao.executar(this::menuClientes); break;
                case 5: Acao.executar(this::menuPromocoes); break;
                case 6: Acao.executar(menuCliente::listarPedidos); break;
                default: painel.exibir();
            }
        }
    }

    private static int cancelar() {
        throw new Entrada.CancelamentoException();
    }

    private static <T> T exigir(T valor) {
        if (valor == null) {
            cancelar();
        }
        return valor;
    }

    private static void mostrarLista(String titulo, List<String> linhas) {
        if (linhas.isEmpty()) {
            Entrada.mensagem("Nenhum registro encontrado.");
        } else {
            Entrada.mostrarTexto(titulo, String.join("\n", linhas));
        }
    }

    // ---------- categorias (UC08) ----------

    private Categoria escolherCategoria() {
        List<Categoria> lista = catalogo.listarCategorias();
        List<String> t = new ArrayList<>();
        lista.forEach(c -> t.add(c.getId() + " - " + c.getNome()));
        int i = Entrada.escolher("Categoria:", t);
        if (i < 0) {
            if (lista.isEmpty()) {
                Entrada.erro("Não há categorias cadastradas.");
            }
            cancelar();
        }
        return lista.get(i);
    }

    private List<String> lerCampos(String padrao) {
        String t = exigir(Entrada.texto("Campos específicos separados por vírgula (ex.: garantia, voltagem):", padrao));
        List<String> r = new ArrayList<>();
        for (String s : t.split(",")) {
            if (!s.isBlank()) {
                r.add(s.trim());
            }
        }
        return r;
    }

    private void menuCategorias() throws Exception {
        int op = Entrada.escolher("Categorias", CRUD);
        if (op < 0) {
            return;
        }
        switch (op) {
            case 0: {
                String nome = exigir(Entrada.textoObrigatorio("Nome da categoria:", ""));
                Categoria c = catalogo.cadastrarCategoria(nome, lerCampos(""));
                Entrada.mensagem("Categoria " + c.getId() + " cadastrada.");
                break;
            }
            case 1: {
                Categoria c = escolherCategoria();
                String nome = exigir(Entrada.textoObrigatorio("Nome:", c.getNome()));
                catalogo.alterarCategoria(c.getId(), nome, lerCampos(String.join(", ", c.getCamposEspecificos())));
                Entrada.mensagem("Categoria alterada.");
                break;
            }
            case 2: {
                Categoria c = escolherCategoria();
                if (Entrada.confirmar("Excluir a categoria " + c.getNome() + "?")) {
                    catalogo.excluirCategoria(c.getId());
                    Entrada.mensagem("Categoria excluída.");
                }
                break;
            }
            case 3: {
                Categoria c = escolherCategoria();
                Entrada.mensagem(c.getId() + " - " + c.getNome() + "\nCampos: "
                        + (c.getCamposEspecificos().isEmpty() ? "(nenhum)" : String.join(", ", c.getCamposEspecificos())));
                break;
            }
            default: {
                List<String> l = new ArrayList<>();
                catalogo.listarCategorias().forEach(c -> l.add(c.getId() + " - " + c.getNome()
                        + " | campos: " + String.join(", ", c.getCamposEspecificos())));
                mostrarLista("Categorias", l);
            }
        }
    }

    // ---------- produtos (UC09, UC10) ----------

    private Produto escolherProduto() {
        List<Produto> lista = catalogo.listarProdutos(null);
        List<String> t = new ArrayList<>();
        lista.forEach(p -> t.add(Formatador.produtoLinha(p)));
        int i = Entrada.escolher("Produto:", t);
        if (i < 0) {
            if (lista.isEmpty()) {
                Entrada.erro("Não há produtos cadastrados.");
            }
            cancelar();
        }
        return lista.get(i);
    }

    private Map<String, String> lerCaracteristicas(Categoria cat, Map<String, String> atuais) {
        Map<String, String> r = new LinkedHashMap<>();
        for (String campo : cat.getCamposEspecificos()) {
            String v = exigir(Entrada.texto(campo + ":", atuais == null ? "" : atuais.getOrDefault(campo, "")));
            if (!v.isEmpty()) {
                r.put(campo, v);
            }
        }
        return r;
    }

    private void menuProdutos() throws Exception {
        int op = Entrada.escolher("Produtos", CRUD);
        if (op < 0) {
            return;
        }
        switch (op) {
            case 0: {
                String nome = exigir(Entrada.textoObrigatorio("Nome do produto:", ""));
                BigDecimal preco = exigir(Entrada.decimal("Preço (R$):", ""));
                Integer estoque = exigir(Entrada.inteiro("Estoque inicial:", 0));
                Categoria cat = escolherCategoria();
                Produto p = catalogo.cadastrarProduto(nome, preco, estoque, cat.getId(), lerCaracteristicas(cat, null));
                Entrada.mensagem("Produto " + p.getId() + " cadastrado.");
                break;
            }
            case 1: {
                Produto p = escolherProduto();
                String nome = exigir(Entrada.textoObrigatorio("Nome:", p.getNome()));
                BigDecimal preco = exigir(Entrada.decimal("Preço (R$):", p.getPreco().toPlainString().replace('.', ',')));
                Categoria cat = escolherCategoria();
                catalogo.alterarProduto(p.getId(), nome, preco, cat.getId(), lerCaracteristicas(cat, p.getCaracteristicas()));
                Entrada.mensagem("Produto alterado.");
                break;
            }
            case 2: {
                Produto p = escolherProduto();
                if (Entrada.confirmar("Excluir o produto " + p.getNome() + "?")) {
                    catalogo.excluirProduto(p.getId());
                    Entrada.mensagem("Produto excluído.");
                }
                break;
            }
            case 3:
                menuCliente.consultarCatalogo();
                break;
            default: {
                List<String> l = new ArrayList<>();
                catalogo.listarProdutos(null).forEach(p -> l.add(Formatador.produtoLinha(p)));
                mostrarLista("Produtos", l);
            }
        }
    }

    private void reporEstoque() throws Exception {
        Produto p = escolherProduto();
        Integer qtd = exigir(Entrada.inteiro("Quantidade a repor para " + p.getNome() + ":", 1));
        catalogo.reporEstoque(p.getId(), qtd);
        Entrada.mensagem("Novo estoque de " + p.getNome() + ": " + p.getEstoque());
    }

    // ---------- clientes (UC11) ----------

    private Cliente escolherCliente() {
        List<Cliente> lista = clientes.listar();
        List<String> t = new ArrayList<>();
        lista.forEach(c -> t.add(Formatador.clienteLinha(c)));
        int i = Entrada.escolher("Cliente:", t);
        if (i < 0) {
            if (lista.isEmpty()) {
                Entrada.erro("Não há clientes cadastrados.");
            }
            cancelar();
        }
        return lista.get(i);
    }

    private void menuClientes() throws Exception {
        int op = Entrada.escolher("Clientes", CRUD);
        if (op < 0) {
            return;
        }
        switch (op) {
            case 0: {
                String nome = exigir(Entrada.textoObrigatorio("Nome:", ""));
                String cpf = exigir(Entrada.textoObrigatorio("CPF:", ""));
                String email = exigir(Entrada.textoObrigatorio("E-mail:", ""));
                String tel = exigir(Entrada.textoObrigatorio("Telefone:", ""));
                Cliente c = clientes.cadastrar(nome, cpf, email, tel);
                Entrada.mensagem("Cliente " + c.getId() + " cadastrado.");
                break;
            }
            case 1: {
                Cliente c = escolherCliente();
                String nome = exigir(Entrada.textoObrigatorio("Nome:", c.getNome()));
                String cpf = exigir(Entrada.textoObrigatorio("CPF:", c.getCpfFormatado()));
                String email = exigir(Entrada.textoObrigatorio("E-mail:", c.getEmail()));
                String tel = exigir(Entrada.textoObrigatorio("Telefone:", c.getTelefone()));
                clientes.alterar(c.getId(), nome, cpf, email, tel);
                Entrada.mensagem("Cliente alterado.");
                break;
            }
            case 2: {
                Cliente c = escolherCliente();
                if (Entrada.confirmar("Excluir o cliente " + c.getNome() + "?")) {
                    clientes.excluir(c.getId());
                    Entrada.mensagem("Cliente excluído.");
                }
                break;
            }
            case 3: {
                String t = exigir(Entrada.textoObrigatorio("CPF ou parte do nome:", ""));
                List<String> l = new ArrayList<>();
                clientes.buscarPorCpf(t).ifPresent(c -> l.add(Formatador.clienteLinha(c)));
                if (l.isEmpty()) {
                    clientes.buscarPorNome(t).forEach(c -> l.add(Formatador.clienteLinha(c)));
                }
                mostrarLista("Clientes", l);
                break;
            }
            default: {
                List<String> l = new ArrayList<>();
                clientes.listar().forEach(c -> l.add(Formatador.clienteLinha(c)));
                mostrarLista("Clientes", l);
            }
        }
    }

    // ---------- promoções (UC12) ----------

    private Promocao escolherPromocao() {
        List<Promocao> lista = promocoes.listar();
        List<String> t = new ArrayList<>();
        lista.forEach(p -> t.add(p.getId() + " - " + p.descricao()));
        int i = Entrada.escolher("Promoção:", t);
        if (i < 0) {
            if (lista.isEmpty()) {
                Entrada.erro("Não há promoções cadastradas.");
            }
            cancelar();
        }
        return lista.get(i);
    }

    private void menuPromocoes() throws Exception {
        int op = Entrada.escolher("Promoções", Arrays.asList(
                "Cadastrar (assistente)", "Alterar", "Ativar ou desativar", "Excluir", "Listar"));
        if (op < 0) {
            return;
        }
        switch (op) {
            case 0:
                assistente(null);
                break;
            case 1:
                assistente(escolherPromocao());
                break;
            case 2: {
                Promocao p = escolherPromocao();
                promocoes.ativar(p.getId(), !p.isAtiva());
                Entrada.mensagem("Promoção " + (p.isAtiva() ? "ativada." : "desativada."));
                break;
            }
            case 3: {
                Promocao p = escolherPromocao();
                if (Entrada.confirmar("Excluir a promoção " + p.getNome() + "?")) {
                    promocoes.excluir(p.getId());
                    Entrada.mensagem("Promoção excluída.");
                }
                break;
            }
            default: {
                List<String> l = new ArrayList<>();
                promocoes.listar().forEach(p -> l.add(p.getId() + " - " + p.descricao()));
                mostrarLista("Promoções", l);
            }
        }
    }

    /** Assistente de promoção: nome, escopo e alvo, condições, efeito, grupo e vigência; resumo antes de gravar. */
    private void assistente(Promocao atual) throws Exception {
        String nome = exigir(Entrada.textoObrigatorio("Nome da promoção:", atual == null ? "" : atual.getNome()));
        EscopoPromocao[] escopos = EscopoPromocao.values();
        List<String> nEsc = new ArrayList<>();
        for (EscopoPromocao e : escopos) {
            nEsc.add(e.getRotulo());
        }
        int ie = Entrada.escolher("A promoção vale para:", nEsc);
        if (ie < 0) {
            cancelar();
        }
        EscopoPromocao escopo = escopos[ie];
        Integer alvo = null;
        if (escopo == EscopoPromocao.CATEGORIA) {
            alvo = escolherCategoria().getId();
        } else if (escopo == EscopoPromocao.PRODUTO) {
            alvo = escolherProduto().getId();
        }
        int qtdMin = exigir(Entrada.inteiro("Quantidade mínima (1 = sem mínimo):", atual == null ? 1 : atual.getQtdMinima()));
        TipoDesconto[] tipos = TipoDesconto.values();
        List<String> nTipos = new ArrayList<>();
        for (TipoDesconto t : tipos) {
            nTipos.add(t.getRotulo());
        }
        int it = Entrada.escolher("Efeito:", nTipos);
        if (it < 0) {
            cancelar();
        }
        BigDecimal valor = exigir(Entrada.decimal("Valor (percentual de 0,01 a 100 ou reais):",
                atual == null ? "" : atual.getValor().toPlainString().replace('.', ',')));
        int grupo = exigir(Entrada.inteiro("Tamanho do grupo (1 = todas as unidades; ex.: leve 3 pague 2 = 3):",
                atual == null ? 1 : atual.getTamanhoGrupo()));
        int premiadas = exigir(Entrada.inteiro("Unidades premiadas por grupo (ex.: leve 3 pague 2 = 1):",
                atual == null ? 1 : atual.getUnidadesPremiadas()));
        LocalDate ini = Entrada.data("Início da vigência", true);
        LocalDate fim = Entrada.data("Fim da vigência", true);

        Promocao previa = Promocao.rascunho(nome, escopo, alvo, qtdMin, grupo, premiadas, tipos[it], valor, ini, fim);
        if (!Entrada.confirmar("Resumo da promoção:\n\n" + previa.descricao() + "\n\nGravar?")) {
            return;
        }
        if (atual == null) {
            promocoes.cadastrar(nome, escopo, alvo, qtdMin, grupo, premiadas, tipos[it], valor, ini, fim);
            Entrada.mensagem("Promoção cadastrada.");
        } else {
            promocoes.alterar(atual.getId(), nome, escopo, alvo, qtdMin, grupo, premiadas, tipos[it], valor, ini, fim);
            Entrada.mensagem("Promoção alterada.");
        }
    }
}
