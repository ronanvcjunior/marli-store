package marlistore.ui;

import java.util.Optional;
import javax.swing.JOptionPane;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.Cliente;
import marlistore.service.CatalogoService;
import marlistore.service.ClienteService;
import marlistore.service.PedidoService;
import marlistore.service.PromocaoService;
import marlistore.service.RelatorioService;
import marlistore.session.PerfilUsuario;
import marlistore.session.SessaoUsuario;

/**
 * Tela principal (UC01 e UC02): pergunta perfil e nome, identifica o cliente
 * pelo CPF (oferecendo o autocadastro) e abre o menu do perfil. Cancelar em
 * qualquer janela da abertura encerra o programa.
 */
public class TelaPrincipal {

    private final CatalogoService catalogo;
    private final ClienteService clientes;
    private final PromocaoService promocoes;
    private final PedidoService pedidos;
    private final RelatorioService relatorios;

    /**
     * @param catalogo   serviço de catálogo
     * @param clientes   serviço de clientes
     * @param promocoes  serviço de promoções
     * @param pedidos    serviço de pedidos
     * @param relatorios serviço de relatórios
     */
    public TelaPrincipal(CatalogoService catalogo, ClienteService clientes, PromocaoService promocoes,
                         PedidoService pedidos, RelatorioService relatorios) {
        this.catalogo = catalogo;
        this.clientes = clientes;
        this.promocoes = promocoes;
        this.pedidos = pedidos;
        this.relatorios = relatorios;
    }

    /** Inicia a sessão e abre o menu correspondente ao perfil. */
    public void iniciar() {
        SessaoUsuario sessao = iniciarSessao();
        if (sessao == null) {
            return;
        }
        MenuCliente menuCliente = new MenuCliente(sessao, catalogo, clientes, pedidos);
        if (sessao.isAdministrador()) {
            new MenuAdmin(sessao, menuCliente, catalogo, clientes, promocoes,
                    new PainelRelatorios(relatorios)).exibir();
        } else {
            menuCliente.exibir();
        }
        Entrada.mensagem("Até logo, " + sessao.getNome() + "!");
    }

    private SessaoUsuario iniciarSessao() {
        while (true) {
            int perfil = JOptionPane.showOptionDialog(null, "Bem-vindo(a) à Marli Store!\nEscolha seu perfil:",
                    Entrada.TITULO, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Administrador", "Cliente"}, "Cliente");
            if (perfil < 0) {
                return null;
            }
            String nome = Entrada.textoObrigatorio("Seu nome:", "");
            if (nome == null) {
                return null;
            }
            try {
                if (perfil == 0) {
                    return new SessaoUsuario(nome, PerfilUsuario.ADMINISTRADOR, null);
                }
                String cpf = Entrada.textoObrigatorio("Seu CPF:", "");
                if (cpf == null) {
                    return null;
                }
                Optional<Cliente> c = clientes.buscarPorCpf(cpf);
                if (c.isPresent()) {
                    return new SessaoUsuario(nome, PerfilUsuario.CLIENTE, c.get());
                }
                if (!Entrada.confirmar("CPF não cadastrado. Deseja se cadastrar agora?")) {
                    continue;
                }
                Cliente novo = cadastrar(nome, cpf);
                if (novo != null) {
                    return new SessaoUsuario(nome, PerfilUsuario.CLIENTE, novo);
                }
            } catch (DadosInvalidosException e) {
                Entrada.erro(e.getMessage());
            }
        }
    }

    /** UC02 - cadastro do cliente durante a abertura da sessão. */
    private Cliente cadastrar(String nome, String cpf) {
        String n = Entrada.textoObrigatorio("Nome completo:", nome);
        String email = n == null ? null : Entrada.textoObrigatorio("E-mail:", "");
        String tel = email == null ? null : Entrada.textoObrigatorio("Telefone:", "");
        if (tel == null) {
            return null;
        }
        try {
            return clientes.cadastrar(n, cpf, email, tel);
        } catch (DadosInvalidosException e) {
            Entrada.erro(e.getMessage());
            return null;
        } catch (RuntimeException e) {
            Entrada.erro("Não foi possível gravar o cadastro.");
            return null;
        }
    }
}
