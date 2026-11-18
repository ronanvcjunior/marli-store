package marlistore.session;

/** Perfis de acesso (RN-06). Sem senha, o perfil organiza a interface mas não é barreira de segurança. */
public enum PerfilUsuario {
    /** Gerencia catálogo, clientes, promoções, pedidos e relatórios. */
    ADMINISTRADOR("Administrador"),
    /** Consulta produtos e gerencia os próprios pedidos e histórico. */
    CLIENTE("Cliente");

    private final String rotulo;

    PerfilUsuario(String rotulo) {
        this.rotulo = rotulo;
    }

    /** @return nome do perfil em português */
    public String getRotulo() {
        return rotulo;
    }
}
