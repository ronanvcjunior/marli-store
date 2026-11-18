package marlistore.service;

/** Períodos aceitos pelo relatório de vendas (RF-17). */
public enum PeriodoPredefinido {
    /** Do mesmo dia do ano anterior até hoje. */
    ULTIMO_ANO("Último ano"),
    /** Dos últimos seis meses até hoje. */
    ULTIMOS_6_MESES("Últimos 6 meses"),
    /** Do mesmo dia do mês anterior até hoje. */
    ULTIMO_MES("Último mês"),
    /** Do dia 1 do mês corrente até hoje. */
    MES_ATUAL("Mês atual"),
    /** Datas livres informadas pelo usuário. */
    PERSONALIZADO("Datas livres");

    private final String rotulo;

    PeriodoPredefinido(String rotulo) {
        this.rotulo = rotulo;
    }

    /** @return nome em português */
    public String getRotulo() {
        return rotulo;
    }
}
