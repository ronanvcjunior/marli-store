package marlistore.session;

import static org.junit.jupiter.api.Assertions.*;

import marlistore.exception.DadosInvalidosException;
import marlistore.model.Cliente;
import org.junit.jupiter.api.Test;

class SessaoUsuarioTest {

    @Test
    void administradorPrecisaApenasDeNome() throws Exception {
        SessaoUsuario s = new SessaoUsuario("Marli", PerfilUsuario.ADMINISTRADOR, null);
        assertTrue(s.isAdministrador());
        assertNull(s.getClienteLogado());
        assertThrows(DadosInvalidosException.class, () -> new SessaoUsuario(" ", PerfilUsuario.ADMINISTRADOR, null));
        assertThrows(DadosInvalidosException.class, () -> new SessaoUsuario("Marli", null, null));
    }

    @Test
    void clientePrecisaDeCadastro() throws Exception {
        Cliente c = new Cliente("Ana", "52998224725", "a@b.com", "62999990000");
        SessaoUsuario s = new SessaoUsuario("Ana", PerfilUsuario.CLIENTE, c);
        assertFalse(s.isAdministrador());
        assertSame(c, s.getClienteLogado());
        assertThrows(DadosInvalidosException.class, () -> new SessaoUsuario("Ana", PerfilUsuario.CLIENTE, null));
    }
}
