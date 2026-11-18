package marlistore.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.Validacao;

/**
 * Entrada e saída por {@link JOptionPane} (RNF-08). Os métodos de leitura
 * devolvem {@code null} quando o usuário cancela e repetem a pergunta, com
 * mensagem específica, quando a entrada é inválida (RNF-11).
 */
public final class Entrada {

    /** Título das janelas. */
    public static final String TITULO = "Marli Store";

    private Entrada() {
    }

    /**
     * Lê um texto (pode ser vazio).
     *
     * @param mensagem pergunta
     * @param padrao   valor inicial (pode ser nulo)
     * @return texto digitado, ou {@code null} se cancelado
     */
    public static String texto(String mensagem, String padrao) {
        Object r = JOptionPane.showInputDialog(null, mensagem, TITULO, JOptionPane.QUESTION_MESSAGE,
                null, null, padrao == null ? "" : padrao);
        return r == null ? null : r.toString().trim();
    }

    /**
     * Lê um texto obrigatório, repetindo enquanto estiver vazio.
     *
     * @param mensagem pergunta
     * @param padrao   valor inicial
     * @return texto, ou {@code null} se cancelado
     */
    public static String textoObrigatorio(String mensagem, String padrao) {
        while (true) {
            String t = texto(mensagem, padrao);
            if (t == null) {
                return null;
            }
            try {
                return Validacao.texto(t, "solicitado");
            } catch (DadosInvalidosException e) {
                erro("Este campo é obrigatório.");
            }
        }
    }

    /**
     * Lê um inteiro.
     *
     * @param mensagem pergunta
     * @param padrao   valor inicial (pode ser nulo)
     * @return o inteiro, ou {@code null} se cancelado
     */
    public static Integer inteiro(String mensagem, Integer padrao) {
        while (true) {
            String t = texto(mensagem, padrao == null ? "" : padrao.toString());
            if (t == null) {
                return null;
            }
            try {
                return Validacao.inteiro(t, "valor");
            } catch (DadosInvalidosException e) {
                erro(e.getMessage());
            }
        }
    }

    /**
     * Lê um valor decimal (aceita vírgula).
     *
     * @param mensagem pergunta
     * @param padrao   valor inicial (pode ser nulo)
     * @return o valor, ou {@code null} se cancelado
     */
    public static BigDecimal decimal(String mensagem, String padrao) {
        while (true) {
            String t = texto(mensagem, padrao);
            if (t == null) {
                return null;
            }
            try {
                return Validacao.decimal(t, "valor");
            } catch (DadosInvalidosException e) {
                erro(e.getMessage());
            }
        }
    }

    /**
     * Lê uma data dd/mm/aaaa; vazio vale "sem data" quando {@code opcional}.
     *
     * @param mensagem pergunta
     * @param opcional se vazio é aceito
     * @return a data; {@code null} se vazio (opcional)
     * @throws CancelamentoException se o usuário cancelar
     */
    public static LocalDate data(String mensagem, boolean opcional) {
        while (true) {
            String t = texto(mensagem + (opcional ? " (dd/mm/aaaa, vazio = sem data)" : " (dd/mm/aaaa)"), "");
            if (t == null) {
                throw new CancelamentoException();
            }
            if (t.isEmpty() && opcional) {
                return null;
            }
            try {
                return Validacao.data(t);
            } catch (DadosInvalidosException e) {
                erro(e.getMessage());
            }
        }
    }

    /**
     * Pede uma confirmação Sim/Não.
     *
     * @param mensagem pergunta
     * @return {@code true} se confirmado
     */
    public static boolean confirmar(String mensagem) {
        return JOptionPane.showConfirmDialog(null, mensagem, TITULO, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Mostra uma lista de opções e devolve a posição escolhida.
     *
     * @param mensagem pergunta
     * @param opcoes   textos das opções
     * @return índice escolhido, ou -1 se cancelado ou sem opções
     */
    public static int escolher(String mensagem, List<String> opcoes) {
        if (opcoes.isEmpty()) {
            return -1;
        }
        Object r = JOptionPane.showInputDialog(null, mensagem, TITULO, JOptionPane.QUESTION_MESSAGE, null,
                opcoes.toArray(), opcoes.get(0));
        return r == null ? -1 : opcoes.indexOf(r);
    }

    /**
     * Exibe uma informação.
     *
     * @param mensagem texto
     */
    public static void mensagem(String mensagem) {
        JOptionPane.showMessageDialog(null, mensagem, TITULO, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Exibe uma mensagem de erro, sem rastro técnico.
     *
     * @param mensagem texto em português
     */
    public static void erro(String mensagem) {
        JOptionPane.showMessageDialog(null, mensagem, TITULO, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Exibe um texto longo em área rolável.
     *
     * @param titulo janela
     * @param texto  conteúdo
     */
    public static void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane painel = new JScrollPane(area);
        painel.setPreferredSize(new Dimension(640, 360));
        JOptionPane.showMessageDialog(null, painel, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    /** Sinaliza que o usuário cancelou uma entrada no meio de um assistente. */
    public static class CancelamentoException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        /** Cria o sinal de cancelamento. */
        public CancelamentoException() {
            super("Operação cancelada.");
        }
    }
}
