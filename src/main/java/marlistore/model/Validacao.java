package marlistore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import marlistore.exception.DadosInvalidosException;

/**
 * Regras de validação de entradas (RF-25). Todas as falhas geram
 * {@link DadosInvalidosException} com mensagem específica em português.
 */
public final class Validacao {

    /** Formato de data aceito e exibido: dd/mm/aaaa. */
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private Validacao() {
    }

    /**
     * Exige texto não vazio.
     *
     * @param valor texto informado
     * @param campo nome do campo, usado na mensagem
     * @return texto sem espaços nas pontas
     * @throws DadosInvalidosException se o texto for nulo ou vazio
     */
    public static String texto(String valor, String campo) throws DadosInvalidosException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
        return valor.trim();
    }

    /**
     * Exige valor monetário maior que zero.
     *
     * @param valor valor informado
     * @param campo nome do campo, usado na mensagem
     * @return o mesmo valor, com duas casas decimais
     * @throws DadosInvalidosException se o valor for nulo ou menor ou igual a zero
     */
    public static BigDecimal valorPositivo(BigDecimal valor, String campo) throws DadosInvalidosException {
        if (valor == null || valor.signum() <= 0) {
            throw new DadosInvalidosException("O campo " + campo + " deve ser maior que zero.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Exige inteiro maior que zero.
     *
     * @param valor valor informado
     * @param campo nome do campo, usado na mensagem
     * @return o mesmo valor
     * @throws DadosInvalidosException se o valor for menor ou igual a zero
     */
    public static int inteiroPositivo(int valor, String campo) throws DadosInvalidosException {
        if (valor <= 0) {
            throw new DadosInvalidosException("O campo " + campo + " deve ser um número inteiro positivo.");
        }
        return valor;
    }

    /**
     * Exige inteiro maior ou igual a zero.
     *
     * @param valor valor informado
     * @param campo nome do campo, usado na mensagem
     * @return o mesmo valor
     * @throws DadosInvalidosException se o valor for negativo
     */
    public static int inteiroNaoNegativo(int valor, String campo) throws DadosInvalidosException {
        if (valor < 0) {
            throw new DadosInvalidosException("O campo " + campo + " não pode ser negativo.");
        }
        return valor;
    }

    /**
     * Interpreta uma data no formato dd/mm/aaaa.
     *
     * @param texto texto informado
     * @return a data correspondente
     * @throws DadosInvalidosException se o texto não for uma data válida
     */
    public static LocalDate data(String texto) throws DadosInvalidosException {
        try {
            return LocalDate.parse(texto == null ? "" : texto.trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new DadosInvalidosException("Data inválida. Use o formato dd/mm/aaaa.");
        }
    }

    /**
     * Interpreta um valor decimal em formato brasileiro ou simples ("12,50" ou "12.50").
     *
     * @param texto texto informado
     * @param campo nome do campo, usado na mensagem
     * @return o valor decimal
     * @throws DadosInvalidosException se o texto não for numérico
     */
    public static BigDecimal decimal(String texto, String campo) throws DadosInvalidosException {
        try {
            String t = texto == null ? "" : texto.trim().replace("R$", "").trim();
            if (t.contains(",")) {
                t = t.replace(".", "").replace(',', '.');
            }
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException("O campo " + campo + " deve ser um número (ex.: 12,50).");
        }
    }

    /**
     * Interpreta um número inteiro.
     *
     * @param texto texto informado
     * @param campo nome do campo, usado na mensagem
     * @return o inteiro
     * @throws DadosInvalidosException se o texto não for um inteiro
     */
    public static int inteiro(String texto, String campo) throws DadosInvalidosException {
        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException("O campo " + campo + " deve ser um número inteiro.");
        }
    }

    /**
     * Remove tudo que não for dígito.
     *
     * @param s texto original
     * @return apenas os dígitos
     */
    public static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /**
     * Verifica um CPF pelo formato (11 dígitos) e pelos dígitos verificadores.
     *
     * @param cpf CPF com ou sem pontuação
     * @return {@code true} se o CPF for válido
     */
    public static boolean cpfValido(String cpf) {
        String d = apenasDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(d, 9) == d.charAt(9) - '0'
                && digitoVerificador(d, 10) == d.charAt(10) - '0';
    }

    private static int digitoVerificador(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
