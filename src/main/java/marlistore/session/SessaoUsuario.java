package marlistore.session;

import marlistore.exception.DadosInvalidosException;
import marlistore.model.Cliente;
import marlistore.model.Validacao;

/**
 * Usuário da sessão atual: perfil, nome (que identifica quem criou cada pedido)
 * e, no perfil Cliente, o cadastro de cliente associado (RF-19).
 */
public class SessaoUsuario {

    private final String nome;
    private final PerfilUsuario perfil;
    private final Cliente clienteLogado;

    /**
     * @param nome          nome do usuário (obrigatório)
     * @param perfil        perfil de acesso
     * @param clienteLogado cadastro do cliente (obrigatório no perfil Cliente; ignorado no Administrador)
     * @throws DadosInvalidosException se faltar nome, perfil ou cliente
     */
    public SessaoUsuario(String nome, PerfilUsuario perfil, Cliente clienteLogado)
            throws DadosInvalidosException {
        this.nome = Validacao.texto(nome, "nome do usuário");
        if (perfil == null) {
            throw new DadosInvalidosException("Escolha um perfil.");
        }
        if (perfil == PerfilUsuario.CLIENTE && clienteLogado == null) {
            throw new DadosInvalidosException("O perfil Cliente precisa de um cadastro de cliente.");
        }
        this.perfil = perfil;
        this.clienteLogado = perfil == PerfilUsuario.CLIENTE ? clienteLogado : null;
    }

    /** @return nome do usuário */
    public String getNome() {
        return nome;
    }

    /** @return perfil de acesso */
    public PerfilUsuario getPerfil() {
        return perfil;
    }

    /** @return cliente da sessão, ou {@code null} para o Administrador */
    public Cliente getClienteLogado() {
        return clienteLogado;
    }

    /** @return {@code true} se o perfil é Administrador */
    public boolean isAdministrador() {
        return perfil == PerfilUsuario.ADMINISTRADOR;
    }
}
