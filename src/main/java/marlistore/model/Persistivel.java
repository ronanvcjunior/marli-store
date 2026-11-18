package marlistore.model;

/**
 * Contrato das entidades que podem ser gravadas em arquivo de texto, uma linha
 * por entidade (RF-23).
 */
public interface Persistivel {

    /**
     * Converte a entidade em uma linha de texto. A reconstrução é feita por um
     * método estático {@code fromLinha} de cada classe, que não consome o
     * contador de IDs.
     *
     * @return linha de texto representando a entidade
     */
    String toLinha();
}
