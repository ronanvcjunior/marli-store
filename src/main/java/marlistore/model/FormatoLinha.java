package marlistore.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário de serialização em linha de texto com escape. Os campos são
 * separados por um caractere; barra invertida, separador e quebras de linha
 * dentro dos valores são escapados.
 */
public final class FormatoLinha {

    /** Separador padrão entre campos de uma linha. */
    public static final char SEP = ';';

    private FormatoLinha() {
    }

    /**
     * Junta campos em uma linha usando o separador padrão.
     *
     * @param campos valores (null vira texto vazio)
     * @return linha pronta para gravação
     */
    public static String juntar(String... campos) {
        return juntar(SEP, campos);
    }

    /**
     * Junta campos em uma linha usando o separador informado.
     *
     * @param sep    caractere separador
     * @param campos valores (null vira texto vazio)
     * @return linha pronta para gravação
     */
    public static String juntar(char sep, String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(escapar(campos[i] == null ? "" : campos[i], sep));
        }
        return sb.toString();
    }

    /**
     * Divide uma linha pelo separador padrão, desfazendo o escape.
     *
     * @param linha linha lida do arquivo
     * @return campos da linha
     */
    public static List<String> dividir(String linha) {
        return dividir(SEP, linha);
    }

    /**
     * Divide uma linha pelo separador informado, desfazendo o escape.
     *
     * @param sep   caractere separador
     * @param linha linha lida do arquivo
     * @return campos da linha
     */
    public static List<String> dividir(char sep, String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '\\' && i + 1 < linha.length()) {
                atual.append(c).append(linha.charAt(++i));
            } else if (c == sep) {
                campos.add(desfazer(atual.toString()));
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(desfazer(atual.toString()));
        return campos;
    }

    private static String escapar(String s, char sep) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == sep) {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String desfazer(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
