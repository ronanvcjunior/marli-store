package marlistore.model;

/**
 * Identidade comum às entidades do domínio. Cada classe concreta mantém o
 * próprio contador estático de IDs ({@code proximoId}), incrementado no
 * construtor (RNF-04); os IDs nunca são reaproveitados (RN-02).
 */
public abstract class EntidadeBase implements Persistivel {

    /** Identificador único da entidade dentro da sua classe. */
    protected final int id;

    /**
     * @param id identificador atribuído pela classe concreta
     */
    protected EntidadeBase(int id) {
        this.id = id;
    }

    /** @return identificador da entidade */
    public int getId() {
        return id;
    }

    /** @return descrição curta, em português, para listas e mensagens */
    public abstract String getDescricao();

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && ((EntidadeBase) o).id == id;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().getName().hashCode() + id;
    }

    @Override
    public String toString() {
        return getDescricao();
    }
}
