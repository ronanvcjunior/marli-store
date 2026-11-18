package marlistore.model;

import java.util.List;
import marlistore.exception.DadosInvalidosException;

/**
 * Comprador identificado por CPF (e não por nome, para evitar homônimos).
 * O CPF é guardado apenas com dígitos.
 */
public class Cliente extends EntidadeBase {

    private static int proximoId = 1;

    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    /**
     * Cria um cliente novo, validando todos os campos.
     *
     * @param nome     nome (obrigatório)
     * @param cpf      CPF válido, com ou sem pontuação
     * @param email    e-mail com formato válido
     * @param telefone telefone com 8 a 13 dígitos
     * @throws DadosInvalidosException se algum campo for inválido
     */
    public Cliente(String nome, String cpf, String email, String telefone) throws DadosInvalidosException {
        super(proximoId);
        this.nome = Validacao.texto(nome, "nome do cliente");
        this.cpf = validarCpf(cpf);
        this.email = validarEmail(email);
        this.telefone = validarTelefone(telefone);
        proximoId++;
    }

    private Cliente(int id, String nome, String cpf, String email, String telefone) {
        super(id);
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
    }

    /** @return próximo ID que será atribuído a um cliente */
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

    /**
     * Verifica se um CPF é válido (formato e dígitos verificadores).
     *
     * @param cpf CPF com ou sem pontuação
     * @return {@code true} se válido
     */
    public static boolean cpfValido(String cpf) {
        return Validacao.cpfValido(cpf);
    }

    private static String validarCpf(String cpf) throws DadosInvalidosException {
        if (!Validacao.cpfValido(cpf)) {
            throw new DadosInvalidosException("CPF inválido.");
        }
        return Validacao.apenasDigitos(cpf);
    }

    private static String validarEmail(String email) throws DadosInvalidosException {
        String e = Validacao.texto(email, "e-mail");
        if (!e.matches("^[^@\\s;]+@[^@\\s;]+\\.[^@\\s;]+$")) {
            throw new DadosInvalidosException("E-mail inválido.");
        }
        return e;
    }

    private static String validarTelefone(String telefone) throws DadosInvalidosException {
        String t = Validacao.apenasDigitos(Validacao.texto(telefone, "telefone"));
        if (t.length() < 8 || t.length() > 13) {
            throw new DadosInvalidosException("Telefone inválido: informe de 8 a 13 dígitos.");
        }
        return t;
    }

    /** @return nome do cliente */
    public String getNome() {
        return nome;
    }

    /** @return CPF, somente dígitos */
    public String getCpf() {
        return cpf;
    }

    /** @return CPF formatado como 000.000.000-00 */
    public String getCpfFormatado() {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    /** @return e-mail do cliente */
    public String getEmail() {
        return email;
    }

    /** @return telefone, somente dígitos */
    public String getTelefone() {
        return telefone;
    }

    /**
     * Altera todos os dados do cliente, validando-os antes de gravar qualquer um.
     *
     * @param nome     novo nome
     * @param cpf      novo CPF
     * @param email    novo e-mail
     * @param telefone novo telefone
     * @throws DadosInvalidosException se algum campo for inválido (nada é alterado)
     */
    public void atualizar(String nome, String cpf, String email, String telefone)
            throws DadosInvalidosException {
        String n = Validacao.texto(nome, "nome do cliente");
        String c = validarCpf(cpf);
        String e = validarEmail(email);
        String t = validarTelefone(telefone);
        this.nome = n;
        this.cpf = c;
        this.email = e;
        this.telefone = t;
    }

    @Override
    public String getDescricao() {
        return nome + " (CPF " + getCpfFormatado() + ")";
    }

    @Override
    public String toLinha() {
        return FormatoLinha.juntar(String.valueOf(id), nome, cpf, email, telefone);
    }

    /**
     * Reconstrói um cliente a partir de uma linha gravada, sem consumir o contador.
     *
     * @param linha linha produzida por {@link #toLinha()}
     * @return o cliente
     */
    public static Cliente fromLinha(String linha) {
        List<String> p = FormatoLinha.dividir(linha);
        return new Cliente(Integer.parseInt(p.get(0)), p.get(1), p.get(2), p.get(3), p.get(4));
    }
}
