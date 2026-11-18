package marlistore.model;

/** A quem uma promoção se aplica: toda a loja, uma categoria ou um produto. */
public enum EscopoPromocao {
    /** Todos os produtos da loja. */
    TODOS("Toda a loja"),
    /** Os produtos de uma categoria. */
    CATEGORIA("Categoria"),
    /** Um único produto. */
    PRODUTO("Produto");

    private final String rotulo;

    EscopoPromocao(String rotulo) {
        this.rotulo = rotulo;
    }

    /** @return nome em português, para exibição */
    public String getRotulo() {
        return rotulo;
    }
}
