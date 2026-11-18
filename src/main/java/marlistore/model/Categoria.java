package marlistore.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import marlistore.exception.DadosInvalidosException;

/**
 * Agrupa produtos e declara os campos específicos que cada produto da
 * categoria deve preencher (garantia, validade etc.). Novas categorias são
 * dados cadastrados pela administradora, sem alteração de código (NEG-06).
 */
public class Categoria extends EntidadeBase {

    private static int proximoId = 1;

    private String nome;
    private final List<String> camposEspecificos = new ArrayList<>();

    /**
     * Cria uma categoria nova, consumindo o próximo ID somente se os dados forem válidos.
     *
     * @param nome nome da categoria (obrigatório)
     * @throws DadosInvalidosException se o nome for vazio
     */
    public Categoria(String nome) throws DadosInvalidosException {
        super(proximoId);
        this.nome = Validacao.texto(nome, "nome da categoria");
        proximoId++;
    }

    private Categoria(int id, String nome) {
        super(id);
        this.nome = nome;
    }

    /** @return próximo ID que será atribuído a uma categoria */
    public static int getProximoId() {
        return proximoId;
    }

    /**
     * Ajusta o contador de IDs. Usado na carga dos dados; o contador nunca recua.
     *
     * @param valor novo valor do contador (ignorado se menor que o atual)
     */
    public static void setProximoId(int valor) {
        proximoId = Math.max(proximoId, valor);
    }

    /** @return nome da categoria */
    public String getNome() {
        return nome;
    }

    /**
     * @param nome novo nome (obrigatório)
     * @throws DadosInvalidosException se o nome for vazio
     */
    public void setNome(String nome) throws DadosInvalidosException {
        this.nome = Validacao.texto(nome, "nome da categoria");
    }

    /** @return lista imutável dos campos específicos declarados */
    public List<String> getCamposEspecificos() {
        return Collections.unmodifiableList(camposEspecificos);
    }

    /**
     * Declara um campo específico (ex.: "garantia"). Repetições são rejeitadas sem diferenciar maiúsculas.
     *
     * @param campo nome do campo
     * @throws DadosInvalidosException se o campo for vazio ou já existir
     */
    public void adicionarCampo(String campo) throws DadosInvalidosException {
        String c = Validacao.texto(campo, "campo específico");
        for (String existente : camposEspecificos) {
            if (existente.equalsIgnoreCase(c)) {
                throw new DadosInvalidosException("O campo \"" + c + "\" já existe nesta categoria.");
            }
        }
        camposEspecificos.add(c);
    }

    /**
     * Remove um campo específico, se existir.
     *
     * @param campo nome do campo
     */
    public void removerCampo(String campo) {
        camposEspecificos.removeIf(c -> c.equalsIgnoreCase(campo));
    }

    @Override
    public String getDescricao() {
        return nome;
    }

    @Override
    public String toLinha() {
        return FormatoLinha.juntar(String.valueOf(id), nome,
                FormatoLinha.juntar(',', camposEspecificos.toArray(new String[0])));
    }

    /**
     * Reconstrói uma categoria a partir de uma linha gravada, sem consumir o contador.
     *
     * @param linha linha produzida por {@link #toLinha()}
     * @return a categoria
     */
    public static Categoria fromLinha(String linha) {
        List<String> p = FormatoLinha.dividir(linha);
        Categoria c = new Categoria(Integer.parseInt(p.get(0)), p.get(1));
        if (p.size() > 2 && !p.get(2).isEmpty()) {
            c.camposEspecificos.addAll(FormatoLinha.dividir(',', p.get(2)));
        }
        return c;
    }
}
