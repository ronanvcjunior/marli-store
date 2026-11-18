package marlistore.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.Cliente;
import marlistore.persistencia.BancoDeDados;
import marlistore.session.PerfilUsuario;
import marlistore.session.SessaoUsuario;

/** Monta um sistema completo em uma pasta temporária, com relógio fixo em 17/11/2026 12:00 UTC. */
public class Cenario {

    public static final String CPF_1 = "529.982.247-25";
    public static final String CPF_2 = "111.444.777-35";
    public static final String CPF_3 = "390.533.447-05";

    public final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("UTC"));
    public final BancoDeDados banco;
    public final CatalogoService catalogo;
    public final ClienteService clientes;
    public final PromocaoService promocoes;
    public final PedidoService pedidos;
    public final RelatorioService relatorios;

    public Cenario(Path pasta) {
        banco = new BancoDeDados(pasta);
        banco.carregar();
        catalogo = new CatalogoService(banco.getCategorias(), banco.getProdutos(), banco.getPedidos());
        clientes = new ClienteService(banco.getClientes(), banco.getPedidos());
        promocoes = new PromocaoService(banco.getPromocoes(), banco.getCategorias(), banco.getProdutos());
        pedidos = new PedidoService(banco.getPedidos(), banco.getProdutos(), banco.getClientes(), promocoes,
                new MotorDescontos(), clock);
        relatorios = new RelatorioService(banco.getPedidos(), clock);
    }

    public SessaoUsuario admin() throws DadosInvalidosException {
        return new SessaoUsuario("Marli", PerfilUsuario.ADMINISTRADOR, null);
    }

    public SessaoUsuario sessaoDe(Cliente c) throws DadosInvalidosException {
        return new SessaoUsuario(c.getNome(), PerfilUsuario.CLIENTE, c);
    }

    public Cliente novoCliente(String nome, String cpf) throws DadosInvalidosException {
        return clientes.cadastrar(nome, cpf, nome.toLowerCase().replace(" ", ".") + "@email.com", "(62) 99999-0000");
    }
}
