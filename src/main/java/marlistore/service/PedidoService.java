package marlistore.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.EstoqueInsuficienteException;
import marlistore.exception.OperacaoNaoPermitidaException;
import marlistore.model.Cliente;
import marlistore.model.Pedido;
import marlistore.model.Produto;
import marlistore.model.StatusPedido;
import marlistore.persistencia.Repositorio;
import marlistore.session.SessaoUsuario;

/**
 * Serviço de pedidos (RF-07 a RF-16): criação, itens, aplicação de promoções,
 * finalização, conclusão, cancelamento e histórico. Aplica o controle de acesso
 * por perfil (RN-06): o Cliente só enxerga e altera os próprios pedidos.
 *
 * <p>Os descontos são recalculados a cada alteração enquanto o pedido está em
 * edição e ficam congelados a partir da finalização (RN-08).</p>
 */
public class PedidoService {

    private final Repositorio<Pedido> pedidos;
    private final Repositorio<Produto> produtos;
    private final Repositorio<Cliente> clientes;
    private final PromocaoService promocoes;
    private final MotorDescontos motor;
    private final Clock clock;

    /**
     * @param pedidos    repositório de pedidos
     * @param produtos   repositório de produtos (gravado quando o estoque muda)
     * @param clientes   repositório de clientes
     * @param promocoes  serviço de promoções
     * @param motor      motor de descontos
     * @param clock      relógio (injetável para testes)
     */
    public PedidoService(Repositorio<Pedido> pedidos, Repositorio<Produto> produtos,
                         Repositorio<Cliente> clientes, PromocaoService promocoes,
                         MotorDescontos motor, Clock clock) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.clientes = clientes;
        this.promocoes = promocoes;
        this.motor = motor;
        this.clock = clock;
    }

    /**
     * Versão com o relógio do sistema.
     *
     * @param pedidos   repositório de pedidos
     * @param produtos  repositório de produtos
     * @param clientes  repositório de clientes
     * @param promocoes serviço de promoções
     * @param motor     motor de descontos
     */
    public PedidoService(Repositorio<Pedido> pedidos, Repositorio<Produto> produtos,
                         Repositorio<Cliente> clientes, PromocaoService promocoes, MotorDescontos motor) {
        this(pedidos, produtos, clientes, promocoes, motor, Clock.systemDefaultZone());
    }

    private void verificarAcesso(Pedido p, SessaoUsuario s) throws OperacaoNaoPermitidaException {
        if (!s.isAdministrador() && p.getCliente().getId() != s.getClienteLogado().getId()) {
            throw new OperacaoNaoPermitidaException("Você só pode acessar os seus próprios pedidos.");
        }
    }

    private void recalcular(Pedido p) {
        motor.aplicarAoPedido(p, promocoes.listar(), LocalDate.now(clock));
    }

    private void gravar(boolean estoqueMudou) {
        if (estoqueMudou) {
            produtos.salvar();
        }
        pedidos.salvar();
    }

    /**
     * Cria um pedido VAZIO (carrinho), que só é gravado ao receber o primeiro item.
     *
     * @param clienteId ID do cliente do pedido
     * @param sessao    sessão atual
     * @return o pedido criado
     * @throws DadosInvalidosException       se o cliente não existir
     * @throws OperacaoNaoPermitidaException se um Cliente tentar criar pedido de outro cliente
     */
    public Pedido criarPedido(int clienteId, SessaoUsuario sessao)
            throws DadosInvalidosException, OperacaoNaoPermitidaException {
        Cliente c = clientes.obter(clienteId);
        if (c == null) {
            throw new DadosInvalidosException("Cliente " + clienteId + " não encontrado.");
        }
        if (!sessao.isAdministrador() && sessao.getClienteLogado().getId() != clienteId) {
            throw new OperacaoNaoPermitidaException("Você só pode criar pedidos para o seu próprio cadastro.");
        }
        return new Pedido(c, sessao.getNome(), LocalDateTime.now(clock));
    }

    /**
     * Adiciona um item (soma se o produto já estiver no pedido), aplica promoções e grava.
     *
     * @param pedido    pedido em edição
     * @param produtoId ID do produto
     * @param qtd       quantidade
     * @param sessao    sessão atual
     * @throws EstoqueInsuficienteException  se a soma exceder o estoque
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     * @throws DadosInvalidosException       se o produto não existir ou a quantidade for inválida
     */
    public void adicionarItem(Pedido pedido, int produtoId, int qtd, SessaoUsuario sessao)
            throws EstoqueInsuficienteException, OperacaoNaoPermitidaException, DadosInvalidosException {
        verificarAcesso(pedido, sessao);
        Produto prod = produtos.obter(produtoId);
        if (prod == null) {
            throw new DadosInvalidosException("Produto " + produtoId + " não encontrado.");
        }
        pedido.adicionarItem(prod, qtd);
        recalcular(pedido);
        if (pedidos.obter(pedido.getId()) == null) {
            pedidos.incluir(pedido);
        }
        gravar(false);
    }

    /**
     * Remove um item (somente EM_ANDAMENTO) e recalcula os descontos.
     *
     * @param pedido pedido
     * @param indice posição do item (a partir de 0)
     * @param sessao sessão atual
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     * @throws DadosInvalidosException       se o índice for inválido
     */
    public void removerItem(Pedido pedido, int indice, SessaoUsuario sessao)
            throws OperacaoNaoPermitidaException, DadosInvalidosException {
        verificarAcesso(pedido, sessao);
        pedido.removerItem(indice);
        recalcular(pedido);
        gravar(false);
    }

    /**
     * Altera a quantidade de um item (somente EM_ANDAMENTO) e recalcula os descontos.
     *
     * @param pedido pedido
     * @param indice posição do item (a partir de 0)
     * @param qtd    nova quantidade
     * @param sessao sessão atual
     * @throws EstoqueInsuficienteException  se exceder o estoque
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     * @throws DadosInvalidosException       se o índice ou a quantidade forem inválidos
     */
    public void alterarQuantidade(Pedido pedido, int indice, int qtd, SessaoUsuario sessao)
            throws EstoqueInsuficienteException, OperacaoNaoPermitidaException, DadosInvalidosException {
        verificarAcesso(pedido, sessao);
        pedido.alterarQuantidade(indice, qtd);
        recalcular(pedido);
        gravar(false);
    }

    /**
     * Inicia a finalização: recalcula os descontos, baixa o estoque de todos os itens (ou de nenhum) e grava.
     *
     * @param pedido pedido
     * @param sessao sessão atual
     * @throws EstoqueInsuficienteException  se algum item não tiver mais saldo
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     */
    public void iniciarFinalizacao(Pedido pedido, SessaoUsuario sessao)
            throws EstoqueInsuficienteException, OperacaoNaoPermitidaException {
        verificarAcesso(pedido, sessao);
        if (pedido.getStatus() == StatusPedido.EM_ANDAMENTO) {
            recalcular(pedido);
        }
        pedido.iniciarFinalizacao();
        gravar(true);
    }

    /**
     * Volta o pedido para edição, devolvendo o estoque, e recalcula os descontos.
     *
     * @param pedido pedido em finalização
     * @param sessao sessão atual
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     */
    public void voltarParaAndamento(Pedido pedido, SessaoUsuario sessao) throws OperacaoNaoPermitidaException {
        verificarAcesso(pedido, sessao);
        pedido.voltarParaAndamento();
        recalcular(pedido);
        gravar(true);
    }

    /**
     * Conclui o pedido registrando data e hora (não altera o estoque).
     *
     * @param pedido pedido em finalização
     * @param sessao sessão atual
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     */
    public void concluir(Pedido pedido, SessaoUsuario sessao) throws OperacaoNaoPermitidaException {
        verificarAcesso(pedido, sessao);
        pedido.concluir(LocalDateTime.now(clock));
        gravar(false);
    }

    /**
     * Cancela o pedido, devolvendo o estoque se já foi baixado.
     *
     * @param pedido pedido em andamento ou em finalização
     * @param sessao sessão atual
     * @throws OperacaoNaoPermitidaException se o acesso ou o status não permitirem
     */
    public void cancelar(Pedido pedido, SessaoUsuario sessao) throws OperacaoNaoPermitidaException {
        verificarAcesso(pedido, sessao);
        boolean baixado = pedido.getStatus() == StatusPedido.EM_FINALIZACAO;
        pedido.cancelar(LocalDateTime.now(clock));
        gravar(baixado);
    }

    /**
     * Busca um pedido respeitando o perfil.
     *
     * @param id     ID do pedido
     * @param sessao sessão atual
     * @return o pedido
     * @throws DadosInvalidosException       se não existir
     * @throws OperacaoNaoPermitidaException se for pedido de outro cliente
     */
    public Pedido buscarPedido(int id, SessaoUsuario sessao)
            throws DadosInvalidosException, OperacaoNaoPermitidaException {
        Pedido p = pedidos.buscarPorId(id)
                .orElseThrow(() -> new DadosInvalidosException("Pedido " + id + " não encontrado."));
        verificarAcesso(p, sessao);
        return p;
    }

    /**
     * Lista pedidos visíveis ao perfil (Administrador: todos; Cliente: os próprios), com status.
     *
     * @param sessao sessão atual
     * @return pedidos ordenados por data de criação
     */
    public List<Pedido> listarPedidos(SessaoUsuario sessao) {
        List<Pedido> r = new ArrayList<>();
        for (Pedido p : pedidos.listar()) {
            if (sessao.isAdministrador() || p.getCliente().getId() == sessao.getClienteLogado().getId()) {
                r.add(p);
            }
        }
        r.sort(Comparator.comparing(Pedido::getDataCriacao).thenComparingInt(Pedido::getId));
        return r;
    }

    /**
     * Histórico de compras de um cliente (data, itens, valores e status).
     *
     * @param clienteId ID do cliente
     * @param sessao    sessão atual
     * @return pedidos do cliente
     * @throws OperacaoNaoPermitidaException se um Cliente pedir o histórico de outro
     */
    public List<Pedido> historicoDoCliente(int clienteId, SessaoUsuario sessao)
            throws OperacaoNaoPermitidaException {
        if (!sessao.isAdministrador() && sessao.getClienteLogado().getId() != clienteId) {
            throw new OperacaoNaoPermitidaException("Você só pode consultar o seu próprio histórico.");
        }
        List<Pedido> r = new ArrayList<>();
        for (Pedido p : listarPedidos(sessao)) {
            if (p.getCliente().getId() == clienteId) {
                r.add(p);
            }
        }
        return r;
    }
}
