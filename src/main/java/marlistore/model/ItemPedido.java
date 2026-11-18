package marlistore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Linha de um pedido (value object). Guarda nome, preço, desconto e promoção do
 * momento da inclusão, de modo que alterações posteriores no catálogo não
 * afetam pedidos existentes (RN-08).
 */
public final class ItemPedido {

    private final Produto produto;
    private final String nomeProduto;
    private final BigDecimal precoUnitario;
    private final int quantidade;
    private BigDecimal descontoAplicado = BigDecimal.ZERO.setScale(2);
    private String promocaoAplicada = "";

    /**
     * Cria um item congelando o nome e o preço atuais do produto.
     *
     * @param produto    produto vendido
     * @param quantidade quantidade (positiva)
     */
    public ItemPedido(Produto produto, int quantidade) {
        this(produto, produto.getNome(), produto.getPreco(), quantidade);
    }

    private ItemPedido(Produto produto, String nomeProduto, BigDecimal precoUnitario, int quantidade) {
        this.produto = produto;
        this.nomeProduto = nomeProduto;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    /** @return produto vendido */
    public Produto getProduto() {
        return produto;
    }

    /** @return nome do produto no momento da inclusão */
    public String getNomeProduto() {
        return nomeProduto;
    }

    /** @return preço unitário no momento da inclusão */
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    /** @return quantidade */
    public int getQuantidade() {
        return quantidade;
    }

    /** @return desconto total do item, em reais */
    public BigDecimal getDescontoAplicado() {
        return descontoAplicado;
    }

    /** @return nome da promoção aplicada (vazio se nenhuma) */
    public String getPromocaoAplicada() {
        return promocaoAplicada;
    }

    /**
     * Cria uma cópia do item com outra quantidade e sem desconto (que deve ser recalculado).
     *
     * @param novaQuantidade nova quantidade
     * @return novo item
     */
    public ItemPedido comQuantidade(int novaQuantidade) {
        return new ItemPedido(produto, nomeProduto, precoUnitario, novaQuantidade);
    }

    /** @return preço × quantidade */
    public BigDecimal calcularSubtotalBruto() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    /** @return subtotal bruto menos o desconto */
    public BigDecimal calcularSubtotalLiquido() {
        return calcularSubtotalBruto().subtract(descontoAplicado);
    }

    /**
     * Grava o desconto calculado pelo motor, nunca acima do subtotal.
     *
     * @param r resultado da avaliação de promoções
     */
    public void aplicarDesconto(ResultadoDesconto r) {
        this.descontoAplicado = r.getValor().min(calcularSubtotalBruto()).setScale(2, RoundingMode.HALF_UP);
        this.promocaoAplicada = r.getPromocao() == null ? "" : r.getPromocao().getNome();
    }

    /**
     * Serializa o item em uma linha de {@code itens.dat}.
     *
     * @param pedidoId ID do pedido dono do item
     * @return linha de texto
     */
    public String toLinha(int pedidoId) {
        return FormatoLinha.juntar(String.valueOf(pedidoId), String.valueOf(produto.getId()), nomeProduto,
                precoUnitario.toPlainString(), String.valueOf(quantidade),
                descontoAplicado.toPlainString(), promocaoAplicada);
    }

    /**
     * Lê o ID do pedido de uma linha de {@code itens.dat}.
     *
     * @param linha linha gravada
     * @return ID do pedido dono
     */
    public static int pedidoIdDaLinha(String linha) {
        return Integer.parseInt(FormatoLinha.dividir(linha).get(0));
    }

    /**
     * Reconstrói um item a partir de uma linha gravada.
     *
     * @param linha           linha produzida por {@link #toLinha(int)}
     * @param resolverProduto função que devolve o produto pelo ID
     * @return o item
     * @throws IllegalArgumentException se o produto não existir
     */
    public static ItemPedido fromLinha(String linha, Function<Integer, Produto> resolverProduto) {
        List<String> p = FormatoLinha.dividir(linha);
        Produto produto = resolverProduto.apply(Integer.parseInt(p.get(1)));
        if (produto == null) {
            throw new IllegalArgumentException("Produto inexistente: " + p.get(1));
        }
        ItemPedido item = new ItemPedido(produto, p.get(2), new BigDecimal(p.get(3)), Integer.parseInt(p.get(4)));
        item.descontoAplicado = new BigDecimal(p.get(5));
        item.promocaoAplicada = p.size() > 6 ? p.get(6) : "";
        return item;
    }
}
