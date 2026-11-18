package marlistore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import marlistore.exception.OperacaoNaoPermitidaException;

/**
 * Venda com itens, status, datas e totais. É o agregado raiz do domínio:
 * controla o fluxo de status (RN-03) e a movimentação de estoque (RN-04) e
 * guarda os itens por composição.
 *
 * <p>A baixa de estoque ocorre ao entrar em {@link StatusPedido#EM_FINALIZACAO};
 * a devolução, ao voltar a {@link StatusPedido#EM_ANDAMENTO} ou ao cancelar após
 * a baixa. A conclusão não altera o estoque.</p>
 */
public class Pedido extends EntidadeBase {

    private static int proximoId = 1;

    private final Cliente cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private StatusPedido status = StatusPedido.VAZIO;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataConclusao;
    private LocalDateTime dataCancelamento;
    private String usuarioSessao;

    /**
     * Cria um pedido VAZIO para o cliente, criado agora.
     *
     * @param cliente       cliente do pedido
     * @param usuarioSessao nome do usuário da sessão que criou o pedido
     * @throws DadosInvalidosException se o cliente for nulo ou o usuário vazio
     */
    public Pedido(Cliente cliente, String usuarioSessao) throws DadosInvalidosException {
        this(cliente, usuarioSessao, LocalDateTime.now());
    }

    /**
     * Cria um pedido VAZIO com data de criação explícita.
     *
     * @param cliente       cliente do pedido
     * @param usuarioSessao nome do usuário da sessão
     * @param criadoEm      data e hora de criação
     * @throws DadosInvalidosException se o cliente for nulo ou o usuário vazio
     */
    public Pedido(Cliente cliente, String usuarioSessao, LocalDateTime criadoEm) throws DadosInvalidosException {
        super(proximoId);
        if (cliente == null) {
            throw new DadosInvalidosException("O pedido precisa de um cliente.");
        }
        this.cliente = cliente;
        this.usuarioSessao = Validacao.texto(usuarioSessao, "usuário da sessão");
        this.dataCriacao = criadoEm;
        proximoId++;
    }

    private Pedido(int id, Cliente cliente) {
        super(id);
        this.cliente = cliente;
    }

    /** @return próximo ID que será atribuído a um pedido */
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

    /** @return cliente do pedido */
    public Cliente getCliente() {
        return cliente;
    }

    /** @return lista imutável dos itens */
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /** @return status atual */
    public StatusPedido getStatus() {
        return status;
    }

    /** @return data de criação */
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    /** @return data de conclusão, ou {@code null} */
    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    /** @return data de cancelamento, ou {@code null} */
    public LocalDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    /** @return nome do usuário da sessão que criou o pedido */
    public String getUsuarioSessao() {
        return usuarioSessao;
    }

    private int quantidadeDoProduto(Produto produto) {
        for (ItemPedido i : itens) {
            if (i.getProduto().getId() == produto.getId()) {
                return i.getQuantidade();
            }
        }
        return 0;
    }

    private void exigirEdicao() throws OperacaoNaoPermitidaException {
        if (status != StatusPedido.VAZIO && status != StatusPedido.EM_ANDAMENTO) {
            throw new OperacaoNaoPermitidaException(
                    "O pedido está " + status.getRotulo().toLowerCase() + " e não pode ser editado.");
        }
    }

    private void transicionar(StatusPedido destino) throws OperacaoNaoPermitidaException {
        if (!status.podeIrPara(destino)) {
            throw new OperacaoNaoPermitidaException("Não é possível mudar o pedido de "
                    + status.getRotulo() + " para " + destino.getRotulo() + ".");
        }
        status = destino;
    }

    /**
     * Adiciona um item validando o estoque (RN-01). Itens repetidos do mesmo
     * produto são somados. O primeiro item muda o pedido para EM_ANDAMENTO.
     *
     * @param produto produto a vender
     * @param qtd     quantidade (positiva)
     * @throws EstoqueInsuficienteException  se a soma exceder o saldo do produto
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em edição
     * @throws DadosInvalidosException       se a quantidade não for positiva
     */
    public void adicionarItem(Produto produto, int qtd)
            throws EstoqueInsuficienteException, OperacaoNaoPermitidaException, DadosInvalidosException {
        exigirEdicao();
        Validacao.inteiroPositivo(qtd, "quantidade");
        int total = quantidadeDoProduto(produto) + qtd;
        if (!produto.temEstoque(total)) {
            throw new EstoqueInsuficienteException(produto.getNome(), produto.getEstoque(), total);
        }
        boolean achou = false;
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).getProduto().getId() == produto.getId()) {
                itens.set(i, itens.get(i).comQuantidade(total));
                achou = true;
                break;
            }
        }
        if (!achou) {
            itens.add(new ItemPedido(produto, qtd));
        }
        if (status == StatusPedido.VAZIO) {
            transicionar(StatusPedido.EM_ANDAMENTO);
        }
    }

    private void exigirEmAndamento() throws OperacaoNaoPermitidaException {
        if (status != StatusPedido.EM_ANDAMENTO) {
            throw new OperacaoNaoPermitidaException("Só é possível alterar itens de pedidos em andamento.");
        }
    }

    private void validarIndice(int indice) throws DadosInvalidosException {
        if (indice < 0 || indice >= itens.size()) {
            throw new DadosInvalidosException("Item inexistente no pedido.");
        }
    }

    /**
     * Remove um item (somente EM_ANDAMENTO).
     *
     * @param indice posição do item na lista (a partir de 0)
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em andamento
     * @throws DadosInvalidosException       se o índice não existir
     */
    public void removerItem(int indice) throws OperacaoNaoPermitidaException, DadosInvalidosException {
        exigirEmAndamento();
        validarIndice(indice);
        itens.remove(indice);
    }

    /**
     * Altera a quantidade de um item (somente EM_ANDAMENTO), validando o estoque.
     *
     * @param indice posição do item (a partir de 0)
     * @param qtd    nova quantidade (positiva)
     * @throws EstoqueInsuficienteException  se a quantidade exceder o saldo
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em andamento
     * @throws DadosInvalidosException       se o índice ou a quantidade forem inválidos
     */
    public void alterarQuantidade(int indice, int qtd)
            throws EstoqueInsuficienteException, OperacaoNaoPermitidaException, DadosInvalidosException {
        exigirEmAndamento();
        validarIndice(indice);
        Validacao.inteiroPositivo(qtd, "quantidade");
        ItemPedido atual = itens.get(indice);
        if (!atual.getProduto().temEstoque(qtd)) {
            throw new EstoqueInsuficienteException(atual.getProduto().getNome(),
                    atual.getProduto().getEstoque(), qtd);
        }
        itens.set(indice, atual.comQuantidade(qtd));
    }

    /**
     * Inicia a finalização: reconfere o estoque e baixa todos os itens, ou
     * nenhum. O pedido passa a EM_FINALIZACAO.
     *
     * @throws EstoqueInsuficienteException  se algum item não tiver mais saldo (nada é baixado)
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em andamento ou estiver sem itens
     */
    public void iniciarFinalizacao() throws EstoqueInsuficienteException, OperacaoNaoPermitidaException {
        if (!status.podeIrPara(StatusPedido.EM_FINALIZACAO)) {
            transicionar(StatusPedido.EM_FINALIZACAO);
        }
        if (itens.isEmpty()) {
            throw new OperacaoNaoPermitidaException("O pedido não tem itens para finalizar.");
        }
        for (ItemPedido i : itens) {
            if (!i.getProduto().temEstoque(i.getQuantidade())) {
                throw new EstoqueInsuficienteException(i.getProduto().getNome(),
                        i.getProduto().getEstoque(), i.getQuantidade());
            }
        }
        try {
            for (ItemPedido i : itens) {
                i.getProduto().baixarEstoque(i.getQuantidade());
            }
        } catch (DadosInvalidosException e) {
            throw new IllegalStateException(e);
        }
        status = StatusPedido.EM_FINALIZACAO;
    }

    private void devolverEstoque() {
        try {
            for (ItemPedido i : itens) {
                i.getProduto().reporEstoque(i.getQuantidade());
            }
        } catch (DadosInvalidosException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Volta para edição: devolve o estoque baixado e retorna a EM_ANDAMENTO.
     *
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em finalização
     */
    public void voltarParaAndamento() throws OperacaoNaoPermitidaException {
        transicionar(StatusPedido.EM_ANDAMENTO);
        devolverEstoque();
    }

    /**
     * Conclui o pedido agora. Não altera o estoque.
     *
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em finalização
     */
    public void concluir() throws OperacaoNaoPermitidaException {
        concluir(LocalDateTime.now());
    }

    /**
     * Conclui o pedido na data informada. Não altera o estoque.
     *
     * @param quando data e hora da conclusão
     * @throws OperacaoNaoPermitidaException se o pedido não estiver em finalização
     */
    public void concluir(LocalDateTime quando) throws OperacaoNaoPermitidaException {
        transicionar(StatusPedido.CONCLUIDO);
        dataConclusao = quando;
    }

    /**
     * Cancela o pedido agora (RN-05), devolvendo o estoque se já foi baixado.
     *
     * @throws OperacaoNaoPermitidaException se o pedido estiver vazio, concluído ou cancelado
     */
    public void cancelar() throws OperacaoNaoPermitidaException {
        cancelar(LocalDateTime.now());
    }

    /**
     * Cancela o pedido na data informada, devolvendo o estoque se já foi baixado.
     *
     * @param quando data e hora do cancelamento
     * @throws OperacaoNaoPermitidaException se o pedido estiver vazio, concluído ou cancelado
     */
    public void cancelar(LocalDateTime quando) throws OperacaoNaoPermitidaException {
        if (status.isFinal()) {
            throw new OperacaoNaoPermitidaException(
                    "Pedidos concluídos ou cancelados não podem ser cancelados.");
        }
        boolean baixado = status == StatusPedido.EM_FINALIZACAO;
        transicionar(StatusPedido.CANCELADO);
        if (baixado) {
            devolverEstoque();
        }
        dataCancelamento = quando;
    }

    /** @return soma dos subtotais brutos */
    public BigDecimal calcularTotalBruto() {
        return itens.stream().map(ItemPedido::calcularSubtotalBruto)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** @return soma dos descontos dos itens */
    public BigDecimal calcularTotalDescontos() {
        return itens.stream().map(ItemPedido::getDescontoAplicado)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** @return total líquido (bruto menos descontos) */
    public BigDecimal calcularTotal() {
        return calcularTotalBruto().subtract(calcularTotalDescontos());
    }

    /**
     * Restaura um item lido do arquivo, sem validações de fluxo. Uso exclusivo da camada de persistência.
     *
     * @param item item gravado
     */
    public void restaurarItem(ItemPedido item) {
        itens.add(item);
    }

    @Override
    public String getDescricao() {
        return "Pedido " + id + " - " + cliente.getNome() + " - " + status.getRotulo();
    }

    @Override
    public String toLinha() {
        return FormatoLinha.juntar(String.valueOf(id), String.valueOf(cliente.getId()), status.name(),
                dataCriacao == null ? "" : dataCriacao.toString(),
                dataConclusao == null ? "" : dataConclusao.toString(),
                dataCancelamento == null ? "" : dataCancelamento.toString(), usuarioSessao);
    }

    /**
     * Reconstrói um pedido (sem itens) a partir de uma linha gravada, sem consumir o contador.
     * Os itens são acrescentados depois com {@link #restaurarItem(ItemPedido)}.
     *
     * @param linha           linha produzida por {@link #toLinha()}
     * @param resolverCliente função que devolve o cliente pelo ID
     * @return o pedido
     * @throws IllegalArgumentException se o cliente não existir
     */
    public static Pedido fromLinha(String linha, Function<Integer, Cliente> resolverCliente) {
        List<String> p = FormatoLinha.dividir(linha);
        Cliente c = resolverCliente.apply(Integer.parseInt(p.get(1)));
        if (c == null) {
            throw new IllegalArgumentException("Cliente inexistente: " + p.get(1));
        }
        Pedido ped = new Pedido(Integer.parseInt(p.get(0)), c);
        ped.status = StatusPedido.valueOf(p.get(2));
        ped.dataCriacao = p.get(3).isEmpty() ? null : LocalDateTime.parse(p.get(3));
        ped.dataConclusao = p.get(4).isEmpty() ? null : LocalDateTime.parse(p.get(4));
        ped.dataCancelamento = p.get(5).isEmpty() ? null : LocalDateTime.parse(p.get(5));
        ped.usuarioSessao = p.get(6);
        return ped;
    }
}
