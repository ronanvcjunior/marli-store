package marlistore.persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import marlistore.model.EntidadeBase;

/**
 * Isola a lista em memória ({@link ArrayList}) e o arquivo de texto de um tipo
 * de entidade (padrão Repository). A gravação é segura: os dados vão para um
 * arquivo temporário que substitui o definitivo ao final (RNF-07). Linhas
 * ilegíveis são registradas em log, sem impedir a abertura.
 *
 * @param <T> tipo da entidade
 */
public class Repositorio<T extends EntidadeBase> {

    private static final Logger LOG = Logger.getLogger(Repositorio.class.getName());

    /** Entidades em memória. */
    protected final List<T> itens = new ArrayList<>();
    /** Arquivo principal do repositório. */
    protected final Path arquivo;
    private final Function<String, T> fabrica;
    private Runnable aposSalvar = () -> { };

    /**
     * @param arquivo arquivo de texto onde as entidades são gravadas
     * @param fabrica função que reconstrói uma entidade a partir de uma linha ({@code fromLinha})
     */
    public Repositorio(Path arquivo, Function<String, T> fabrica) {
        this.arquivo = arquivo;
        this.fabrica = fabrica;
    }

    /**
     * Define uma ação executada após cada gravação (usada para gravar os contadores de ID).
     *
     * @param acao ação a executar
     */
    public void setAposSalvar(Runnable acao) {
        this.aposSalvar = acao;
    }

    /**
     * Inclui uma entidade na lista em memória (não grava em arquivo).
     *
     * @param entidade entidade a incluir
     */
    public void incluir(T entidade) {
        itens.add(entidade);
    }

    /**
     * Remove uma entidade pelo ID (não grava em arquivo).
     *
     * @param id identificador
     * @return {@code true} se algo foi removido
     */
    public boolean remover(int id) {
        return itens.removeIf(e -> e.getId() == id);
    }

    /**
     * @param id identificador
     * @return a entidade, se existir
     */
    public Optional<T> buscarPorId(int id) {
        return itens.stream().filter(e -> e.getId() == id).findFirst();
    }

    /**
     * @param id identificador
     * @return a entidade ou {@code null} (útil como resolvedor de referências)
     */
    public T obter(int id) {
        return buscarPorId(id).orElse(null);
    }

    /** @return cópia da lista de entidades, em ordem de inclusão */
    public List<T> listar() {
        return new ArrayList<>(itens);
    }

    /** @return quantidade de entidades */
    public int tamanho() {
        return itens.size();
    }

    /** @return arquivo principal deste repositório */
    public Path getArquivo() {
        return arquivo;
    }

    /** Carrega as entidades do arquivo; linhas ilegíveis vão para o log. Arquivo ausente = lista vazia. */
    public void carregar() {
        itens.clear();
        if (!Files.exists(arquivo)) {
            return;
        }
        int n = 0;
        for (String linha : lerLinhas(arquivo)) {
            n++;
            if (linha.isBlank()) {
                continue;
            }
            try {
                itens.add(fabrica.apply(linha));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Linha ilegível ignorada em {0} (linha {1}): {2}",
                        new Object[]{arquivo.getFileName(), n, e.toString()});
            }
        }
    }

    /**
     * Grava todas as entidades de forma segura e executa a ação pós-gravação.
     *
     * @throws UncheckedIOException se o arquivo não puder ser gravado
     */
    public void salvar() {
        List<String> linhas = new ArrayList<>();
        for (T e : itens) {
            linhas.add(e.toLinha());
        }
        gravarSeguro(arquivo, linhas);
        aposSalvar.run();
    }

    /**
     * Lê todas as linhas de um arquivo UTF-8.
     *
     * @param caminho arquivo a ler
     * @return linhas do arquivo
     */
    protected static List<String> lerLinhas(Path caminho) {
        try {
            return Files.readAllLines(caminho, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + caminho.getFileName(), e);
        }
    }

    /**
     * Grava linhas em arquivo temporário e o move sobre o arquivo definitivo.
     *
     * @param destino arquivo definitivo
     * @param linhas  conteúdo
     */
    protected static void gravarSeguro(Path destino, List<String> linhas) {
        try {
            Path pasta = destino.toAbsolutePath().getParent();
            Files.createDirectories(pasta);
            Path temp = pasta.resolve(destino.getFileName() + ".tmp");
            Files.write(temp, linhas, StandardCharsets.UTF_8);
            try {
                Files.move(temp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar " + destino.getFileName(), e);
        }
    }
}
