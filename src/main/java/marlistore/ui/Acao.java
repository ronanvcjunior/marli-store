package marlistore.ui;

import java.io.UncheckedIOException;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import marlistore.exception.OperacaoNaoPermitidaException;

/** Ação de interface que pode lançar as exceções do sistema; o tratamento é centralizado em {@link #executar(Acao)}. */
@FunctionalInterface
public interface Acao {

    /**
     * Executa a ação.
     *
     * @throws Exception qualquer falha, tratada por {@link #executar(Acao)}
     */
    void rodar() throws Exception;

    /**
     * Executa a ação tratando as exceções com mensagens claras, sem rastro técnico (RNF-11).
     *
     * @param acao ação a executar
     */
    static void executar(Acao acao) {
        try {
            acao.rodar();
        } catch (Entrada.CancelamentoException e) {
            // usuário desistiu: volta ao menu sem mensagem
        } catch (EstoqueInsuficienteException | OperacaoNaoPermitidaException | DadosInvalidosException e) {
            Entrada.erro(e.getMessage());
        } catch (UncheckedIOException e) {
            Entrada.erro("Não foi possível acessar os arquivos de dados. Verifique a pasta dados/.");
        } catch (Exception e) {
            Entrada.erro("Ocorreu um erro inesperado. A operação foi interrompida.");
        }
    }
}
