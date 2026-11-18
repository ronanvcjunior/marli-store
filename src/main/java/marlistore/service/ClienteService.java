package marlistore.service;

import java.util.List;
import java.util.Optional;
import marlistore.exception.DadosInvalidosException;
import marlistore.exception.OperacaoNaoPermitidaException;
import marlistore.model.Cliente;
import marlistore.model.Pedido;
import marlistore.model.Validacao;
import marlistore.persistencia.Repositorio;

/** Serviço de clientes (RF-04, RF-05): CPF válido e único; exclusão bloqueada com pedidos (RN-09). */
public class ClienteService {

    private final Repositorio<Cliente> clientes;
    private final Repositorio<Pedido> pedidos;

    /**
     * @param clientes repositório de clientes
     * @param pedidos  repositório de pedidos (para verificar vínculos)
     */
    public ClienteService(Repositorio<Cliente> clientes, Repositorio<Pedido> pedidos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    /**
     * Cadastra um cliente (também usado no autocadastro do perfil Cliente).
     *
     * @param nome     nome
     * @param cpf      CPF válido e ainda não cadastrado
     * @param email    e-mail
     * @param telefone telefone
     * @return o cliente criado
     * @throws DadosInvalidosException se algum dado for inválido ou o CPF já existir
     */
    public Cliente cadastrar(String nome, String cpf, String email, String telefone)
            throws DadosInvalidosException {
        exigirCpfLivre(cpf, -1);
        Cliente c = new Cliente(nome, cpf, email, telefone);
        clientes.incluir(c);
        clientes.salvar();
        return c;
    }

    /**
     * Altera os dados de um cliente.
     *
     * @param id       ID do cliente
     * @param nome     novo nome
     * @param cpf      novo CPF
     * @param email    novo e-mail
     * @param telefone novo telefone
     * @return o cliente alterado
     * @throws DadosInvalidosException se não existir, algum dado for inválido ou o CPF pertencer a outro cliente
     */
    public Cliente alterar(int id, String nome, String cpf, String email, String telefone)
            throws DadosInvalidosException {
        Cliente c = buscarPorId(id);
        exigirCpfLivre(cpf, id);
        c.atualizar(nome, cpf, email, telefone);
        clientes.salvar();
        return c;
    }

    private void exigirCpfLivre(String cpf, int idIgnorado) throws DadosInvalidosException {
        if (!Validacao.cpfValido(cpf)) {
            throw new DadosInvalidosException("CPF inválido.");
        }
        String d = Validacao.apenasDigitos(cpf);
        for (Cliente c : clientes.listar()) {
            if (c.getId() != idIgnorado && c.getCpf().equals(d)) {
                throw new DadosInvalidosException("Já existe um cliente com este CPF.");
            }
        }
    }

    /**
     * Exclui um cliente sem pedidos.
     *
     * @param id ID do cliente
     * @throws DadosInvalidosException       se não existir
     * @throws OperacaoNaoPermitidaException se o cliente tiver pedidos (RN-09)
     */
    public void excluir(int id) throws DadosInvalidosException, OperacaoNaoPermitidaException {
        Cliente c = buscarPorId(id);
        if (pedidos.listar().stream().anyMatch(p -> p.getCliente().getId() == id)) {
            throw new OperacaoNaoPermitidaException(
                    "O cliente " + c.getNome() + " tem pedidos e não pode ser excluído.");
        }
        clientes.remover(id);
        clientes.salvar();
    }

    /**
     * @param id ID do cliente
     * @return o cliente
     * @throws DadosInvalidosException se não existir
     */
    public Cliente buscarPorId(int id) throws DadosInvalidosException {
        return clientes.buscarPorId(id)
                .orElseThrow(() -> new DadosInvalidosException("Cliente " + id + " não encontrado."));
    }

    /**
     * @param cpf CPF com ou sem pontuação
     * @return o cliente, se cadastrado
     */
    public Optional<Cliente> buscarPorCpf(String cpf) {
        String d = Validacao.apenasDigitos(cpf);
        return clientes.listar().stream().filter(c -> c.getCpf().equals(d)).findFirst();
    }

    /**
     * Busca clientes cujo nome contém o texto (sem diferenciar maiúsculas nem acentos).
     *
     * @param trecho parte do nome
     * @return clientes encontrados
     */
    public List<Cliente> buscarPorNome(String trecho) {
        String t = CatalogoService.normalizar(trecho);
        List<Cliente> r = clientes.listar();
        r.removeIf(c -> !CatalogoService.normalizar(c.getNome()).contains(t));
        return r;
    }

    /** @return todos os clientes */
    public List<Cliente> listar() {
        return clientes.listar();
    }
}
