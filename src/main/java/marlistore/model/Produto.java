package marlistore.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;

/**
 * Item vendável, com preço, controle de estoque e valores para os campos
 * específicos da sua categoria. O estoque nunca fica negativo (RN-01).
 */
public class Produto extends EntidadeBase {

    private static int proximoId = 1;

    private String nome;
    private BigDecimal preco;
    private int estoque;
    private Categoria categoria;
    private final Map<String, String> caracteristicas = new LinkedHashMap<>();

    /**
     * Cria um produto novo.
     *
     * @param nome      nome (obrigatório)
     * @param preco     preço unitário, maior que zero
     * @param estoque   saldo inicial, zero ou mais
     * @param categoria categoria do produto (obrigatória)
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public Produto(String nome, BigDecimal preco, int estoque, Categoria categoria)
            throws DadosInvalidosException {
        super(proximoId);
        this.nome = Validacao.texto(nome, "nome do produto");
        this.preco = Validacao.valorPositivo(preco, "preço");
        this.estoque = Validacao.inteiroNaoNegativo(estoque, "estoque");
        if (categoria == null) {
            throw new DadosInvalidosException("O produto precisa de uma categoria.");
        }
        this.categoria = categoria;
        proximoId++;
    }

    private Produto(int id, String nome, BigDecimal preco, int estoque, Categoria categoria) {
        super(id);
        this.nome = nome;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
    }

    /** @return próximo ID que será atribuído a um produto */
    public static int getProximoId() {
        return proximoId;
    }

    /**
     * Ajusta o contador de IDs (nunca recua).
     *
     * @param valor novo valor do contador
     */
    public static void setProximoId(int valor) {
        proximoId = Math.max(proximoId, valor);
    }

    /** @return nome do produto */
    public String getNome() {
        return nome;
    }

    /**
     * @param nome novo nome (obrigatório)
     * @throws DadosInvalidosException se for vazio
     */
    public void setNome(String nome) throws DadosInvalidosException {
        this.nome = Validacao.texto(nome, "nome do produto");
    }

    /** @return preço unitário */
    public BigDecimal getPreco() {
        return preco;
    }

    /**
     * @param preco novo preço, maior que zero
     * @throws DadosInvalidosException se for inválido
     */
    public void setPreco(BigDecimal preco) throws DadosInvalidosException {
        this.preco = Validacao.valorPositivo(preco, "preço");
    }

    /** @return saldo em estoque */
    public int getEstoque() {
        return estoque;
    }

    /** @return categoria do produto */
    public Categoria getCategoria() {
        return categoria;
    }

    /**
     * @param categoria nova categoria (obrigatória)
     * @throws DadosInvalidosException se for nula
     */
    public void setCategoria(Categoria categoria) throws DadosInvalidosException {
        if (categoria == null) {
            throw new DadosInvalidosException("O produto precisa de uma categoria.");
        }
        this.categoria = categoria;
    }

    /** @return mapa imutável com os valores dos campos específicos */
    public Map<String, String> getCaracteristicas() {
        return Collections.unmodifiableMap(caracteristicas);
    }

    /**
     * Substitui os valores dos campos específicos.
     *
     * @param valores novos valores (campo para valor)
     */
    public void setCaracteristicas(Map<String, String> valores) {
        caracteristicas.clear();
        if (valores != null) {
            caracteristicas.putAll(valores);
        }
    }

    /**
     * @param qtd quantidade desejada
     * @return {@code true} se o saldo cobre a quantidade
     */
    public boolean temEstoque(int qtd) {
        return qtd <= estoque;
    }

    /**
     * Baixa unidades do estoque.
     *
     * @param qtd quantidade a baixar (positiva)
     * @throws EstoqueInsuficienteException se a quantidade exceder o saldo
     * @throws DadosInvalidosException      se a quantidade não for positiva
     */
    public void baixarEstoque(int qtd) throws EstoqueInsuficienteException, DadosInvalidosException {
        Validacao.inteiroPositivo(qtd, "quantidade");
        if (!temEstoque(qtd)) {
            throw new EstoqueInsuficienteException(nome, estoque, qtd);
        }
        estoque -= qtd;
    }

    /**
     * Repõe ou devolve unidades ao estoque.
     *
     * @param qtd quantidade inteira positiva
     * @throws DadosInvalidosException se a quantidade não for positiva
     */
    public void reporEstoque(int qtd) throws DadosInvalidosException {
        Validacao.inteiroPositivo(qtd, "quantidade");
        estoque += qtd;
    }

    @Override
    public String getDescricao() {
        return nome;
    }

    @Override
    public String toLinha() {
        List<String> kv = new ArrayList<>();
        caracteristicas.forEach((k, v) -> {
            kv.add(k);
            kv.add(v);
        });
        return FormatoLinha.juntar(String.valueOf(id), nome, preco.toPlainString(),
                String.valueOf(estoque), String.valueOf(categoria.getId()),
                FormatoLinha.juntar(',', kv.toArray(new String[0])));
    }

    /**
     * Reconstrói um produto a partir de uma linha gravada, sem consumir o contador.
     *
     * @param linha            linha produzida por {@link #toLinha()}
     * @param resolverCategoria função que devolve a categoria pelo ID (nula se não existir)
     * @return o produto
     * @throws IllegalArgumentException se a categoria referenciada não existir
     */
    public static Produto fromLinha(String linha, Function<Integer, Categoria> resolverCategoria) {
        List<String> p = FormatoLinha.dividir(linha);
        Categoria cat = resolverCategoria.apply(Integer.parseInt(p.get(4)));
        if (cat == null) {
            throw new IllegalArgumentException("Categoria inexistente: " + p.get(4));
        }
        Produto produto = new Produto(Integer.parseInt(p.get(0)), p.get(1),
                new BigDecimal(p.get(2)), Integer.parseInt(p.get(3)), cat);
        if (p.size() > 5 && !p.get(5).isEmpty()) {
            List<String> kv = FormatoLinha.dividir(',', p.get(5));
            for (int i = 0; i + 1 < kv.size(); i += 2) {
                produto.caracteristicas.put(kv.get(i), kv.get(i + 1));
            }
        }
        return produto;
    }
}
