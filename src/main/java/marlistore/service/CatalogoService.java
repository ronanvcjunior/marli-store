package marlistore.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.OperacaoNaoPermitidaException;
import marlistore.model.Categoria;
import marlistore.model.Pedido;
import marlistore.model.Produto;
import marlistore.model.Validacao;
import marlistore.persistencia.Repositorio;

/**
 * Serviço de catálogo: categorias, produtos e estoque (RF-01 a RF-03, RF-21).
 * Exclusões são bloqueadas quando há vínculos (RN-09).
 */
public class CatalogoService {

    private final Repositorio<Categoria> categorias;
    private final Repositorio<Produto> produtos;
    private final Repositorio<Pedido> pedidos;

    /**
     * @param categorias repositório de categorias
     * @param produtos   repositório de produtos
     * @param pedidos    repositório de pedidos (para verificar vínculos na exclusão)
     */
    public CatalogoService(Repositorio<Categoria> categorias, Repositorio<Produto> produtos,
                           Repositorio<Pedido> pedidos) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    // ---------- categorias ----------

    /**
     * Cadastra uma categoria com seus campos específicos.
     *
     * @param nome   nome (obrigatório e único)
     * @param campos campos específicos (ex.: garantia, validade)
     * @return a categoria criada
     * @throws DadosInvalidosException se os dados forem inválidos ou o nome já existir
     */
    public Categoria cadastrarCategoria(String nome, List<String> campos) throws DadosInvalidosException {
        exigirNomeCategoriaLivre(nome, -1);
        List<String> validos = validarCampos(campos);
        Categoria c = new Categoria(nome);
        for (String campo : validos) {
            c.adicionarCampo(campo);
        }
        categorias.incluir(c);
        categorias.salvar();
        return c;
    }

    /**
     * Altera nome e campos específicos de uma categoria.
     *
     * @param id     ID da categoria
     * @param nome   novo nome
     * @param campos novos campos específicos
     * @return a categoria alterada
     * @throws DadosInvalidosException se não existir ou os dados forem inválidos
     */
    public Categoria alterarCategoria(int id, String nome, List<String> campos) throws DadosInvalidosException {
        Categoria c = buscarCategoria(id);
        exigirNomeCategoriaLivre(nome, id);
        List<String> validos = validarCampos(campos);
        c.setNome(nome);
        for (String antigo : new ArrayList<>(c.getCamposEspecificos())) {
            c.removerCampo(antigo);
        }
        for (String campo : validos) {
            c.adicionarCampo(campo);
        }
        categorias.salvar();
        return c;
    }

    private static List<String> validarCampos(List<String> campos) throws DadosInvalidosException {
        List<String> validos = new ArrayList<>();
        if (campos != null) {
            for (String campo : campos) {
                String c = Validacao.texto(campo, "campo específico");
                for (String v : validos) {
                    if (v.equalsIgnoreCase(c)) {
                        throw new DadosInvalidosException("O campo \"" + c + "\" foi informado duas vezes.");
                    }
                }
                validos.add(c);
            }
        }
        return validos;
    }

    private void exigirNomeCategoriaLivre(String nome, int idIgnorado) throws DadosInvalidosException {
        String n = normalizar(Validacao.texto(nome, "nome da categoria"));
        for (Categoria c : categorias.listar()) {
            if (c.getId() != idIgnorado && normalizar(c.getNome()).equals(n)) {
                throw new DadosInvalidosException("Já existe uma categoria com o nome \"" + c.getNome() + "\".");
            }
        }
    }

    /**
     * Exclui uma categoria sem produtos.
     *
     * @param id ID da categoria
     * @throws DadosInvalidosException       se não existir
     * @throws OperacaoNaoPermitidaException se houver produtos na categoria (RN-09)
     */
    public void excluirCategoria(int id) throws DadosInvalidosException, OperacaoNaoPermitidaException {
        Categoria c = buscarCategoria(id);
        boolean temProdutos = produtos.listar().stream().anyMatch(p -> p.getCategoria().getId() == id);
        if (temProdutos) {
            throw new OperacaoNaoPermitidaException(
                    "A categoria " + c.getNome() + " tem produtos e não pode ser excluída.");
        }
        categorias.remover(id);
        categorias.salvar();
    }

    /**
     * @param id ID da categoria
     * @return a categoria
     * @throws DadosInvalidosException se não existir
     */
    public Categoria buscarCategoria(int id) throws DadosInvalidosException {
        return categorias.buscarPorId(id)
                .orElseThrow(() -> new DadosInvalidosException("Categoria " + id + " não encontrada."));
    }

    /** @return todas as categorias */
    public List<Categoria> listarCategorias() {
        return categorias.listar();
    }

    // ---------- produtos ----------

    private Map<String, String> validarCaracteristicas(Categoria cat, Map<String, String> valores)
            throws DadosInvalidosException {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (valores == null) {
            return resultado;
        }
        for (Map.Entry<String, String> e : valores.entrySet()) {
            String campo = cat.getCamposEspecificos().stream()
                    .filter(c -> c.equalsIgnoreCase(e.getKey())).findFirst()
                    .orElseThrow(() -> new DadosInvalidosException(
                            "O campo \"" + e.getKey() + "\" não existe na categoria " + cat.getNome() + "."));
            if (e.getValue() != null && !e.getValue().isBlank()) {
                resultado.put(campo, e.getValue().trim());
            }
        }
        return resultado;
    }

    /**
     * Cadastra um produto.
     *
     * @param nome            nome do produto
     * @param preco           preço unitário (maior que zero)
     * @param estoque         saldo inicial (zero ou mais)
     * @param categoriaId     ID da categoria
     * @param caracteristicas valores dos campos específicos da categoria
     * @return o produto criado
     * @throws DadosInvalidosException se os dados forem inválidos
     */
    public Produto cadastrarProduto(String nome, BigDecimal preco, int estoque, int categoriaId,
                                    Map<String, String> caracteristicas) throws DadosInvalidosException {
        Categoria cat = buscarCategoria(categoriaId);
        Map<String, String> valores = validarCaracteristicas(cat, caracteristicas);
        Produto p = new Produto(nome, preco, estoque, cat);
        p.setCaracteristicas(valores);
        produtos.incluir(p);
        produtos.salvar();
        return p;
    }

    /**
     * Altera nome, preço, categoria e características de um produto. O estoque só muda por
     * reposição ou pelo fluxo de pedidos. Pedidos existentes não são afetados (RN-08).
     *
     * @param id              ID do produto
     * @param nome            novo nome
     * @param preco           novo preço
     * @param categoriaId     nova categoria
     * @param caracteristicas novos valores dos campos específicos
     * @return o produto alterado
     * @throws DadosInvalidosException se os dados forem inválidos
     */
    public Produto alterarProduto(int id, String nome, BigDecimal preco, int categoriaId,
                                  Map<String, String> caracteristicas) throws DadosInvalidosException {
        Produto p = buscarProduto(id);
        Categoria cat = buscarCategoria(categoriaId);
        Map<String, String> valores = validarCaracteristicas(cat, caracteristicas);
        Validacao.texto(nome, "nome do produto");
        Validacao.valorPositivo(preco, "preço");
        p.setNome(nome);
        p.setPreco(preco);
        p.setCategoria(cat);
        p.setCaracteristicas(valores);
        produtos.salvar();
        return p;
    }

    /**
     * Repõe estoque (RF-03).
     *
     * @param id  ID do produto
     * @param qtd quantidade inteira positiva
     * @throws DadosInvalidosException se o produto não existir ou a quantidade for inválida
     */
    public void reporEstoque(int id, int qtd) throws DadosInvalidosException {
        Produto p = buscarProduto(id);
        p.reporEstoque(qtd);
        produtos.salvar();
    }

    /**
     * Exclui um produto sem pedidos vinculados.
     *
     * @param id ID do produto
     * @throws DadosInvalidosException       se não existir
     * @throws OperacaoNaoPermitidaException se houver pedidos com o produto (RN-09)
     */
    public void excluirProduto(int id) throws DadosInvalidosException, OperacaoNaoPermitidaException {
        Produto p = buscarProduto(id);
        boolean vinculado = pedidos.listar().stream()
                .anyMatch(ped -> ped.getItens().stream().anyMatch(i -> i.getProduto().getId() == id));
        if (vinculado) {
            throw new OperacaoNaoPermitidaException(
                    "O produto " + p.getNome() + " tem pedidos e não pode ser excluído.");
        }
        produtos.remover(id);
        produtos.salvar();
    }

    /**
     * @param id ID do produto
     * @return o produto
     * @throws DadosInvalidosException se não existir
     */
    public Produto buscarProduto(int id) throws DadosInvalidosException {
        return produtos.buscarPorId(id)
                .orElseThrow(() -> new DadosInvalidosException("Produto " + id + " não encontrado."));
    }

    /**
     * Busca produtos cujo nome contém o texto (sem diferenciar maiúsculas nem acentos).
     *
     * @param trecho parte do nome
     * @return produtos encontrados
     */
    public List<Produto> buscarProdutosPorNome(String trecho) {
        String t = normalizar(trecho);
        List<Produto> r = new ArrayList<>();
        for (Produto p : produtos.listar()) {
            if (normalizar(p.getNome()).contains(t)) {
                r.add(p);
            }
        }
        return r;
    }

    /**
     * Lista produtos, opcionalmente filtrados por categoria.
     *
     * @param categoriaId ID da categoria, ou {@code null} para todas
     * @return produtos
     */
    public List<Produto> listarProdutos(Integer categoriaId) {
        List<Produto> r = new ArrayList<>();
        for (Produto p : produtos.listar()) {
            if (categoriaId == null || p.getCategoria().getId() == categoriaId) {
                r.add(p);
            }
        }
        return r;
    }

    /**
     * Lista os produtos com estoque, que são os visíveis ao perfil Cliente (UC03).
     *
     * @param categoriaId ID da categoria, ou {@code null} para todas
     * @return produtos com saldo maior que zero
     */
    public List<Produto> listarProdutosDisponiveis(Integer categoriaId) {
        List<Produto> r = listarProdutos(categoriaId);
        r.removeIf(p -> p.getEstoque() <= 0);
        return r;
    }
}
