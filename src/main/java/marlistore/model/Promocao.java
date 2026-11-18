package marlistore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import marlistore.exception.DadosInvalidosException;

/**
 * Promoção modelada como dado: combina escopo (a quem se aplica), condições
 * (quantidade mínima e vigência) e efeito (percentual, valor fixo ou preço
 * promocional, aplicado a todas as unidades ou a cada grupo de N unidades).
 * Cobre desconto simples, por quantidade, escalonado e "leve X pague Y".
 *
 * <p>As unidades premiadas são {@code (quantidade ÷ tamanhoGrupo, parte inteira)
 * × unidadesPremiadas}; o desconto é o produto dessas unidades pelo desconto por
 * unidade, limitado ao subtotal do item. Com grupo 1 e uma unidade premiada
 * (padrões), todas as unidades recebem o desconto.</p>
 */
public class Promocao extends EntidadeBase {

    private static int proximoId = 1;

    private String nome;
    private boolean ativa = true;
    private LocalDate inicioVigencia;
    private LocalDate fimVigencia;
    private EscopoPromocao escopo;
    private Integer alvoId;
    private int qtdMinima;
    private int tamanhoGrupo;
    private int unidadesPremiadas;
    private TipoDesconto tipoDesconto;
    private BigDecimal valor;

    /**
     * Cria uma promoção ativa.
     *
     * @param nome              nome da promoção
     * @param escopo            toda a loja, categoria ou produto
     * @param alvoId            ID da categoria ou do produto (nulo para {@link EscopoPromocao#TODOS})
     * @param qtdMinima         quantidade mínima para valer (1 ou mais)
     * @param tamanhoGrupo      tamanho do grupo de unidades (1 ou mais)
     * @param unidadesPremiadas unidades premiadas por grupo (de 1 até o tamanho do grupo)
     * @param tipoDesconto      efeito da promoção
     * @param valor             valor do efeito (percentual de 0,01 a 100, ou reais)
     * @param inicioVigencia    primeiro dia de vigência (nulo = sem início)
     * @param fimVigencia       último dia de vigência (nulo = sem fim)
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public Promocao(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima, int tamanhoGrupo,
                    int unidadesPremiadas, TipoDesconto tipoDesconto, BigDecimal valor,
                    LocalDate inicioVigencia, LocalDate fimVigencia) throws DadosInvalidosException {
        super(proximoId);
        validar(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
        atribuir(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
        proximoId++;
    }

    private Promocao(int id) {
        super(id);
    }

    /**
     * Cria uma promoção provisória (ID 0) apenas para exibir o resumo antes de gravar.
     * Não consome o contador de IDs e não deve ser persistida.
     *
     * @param nome              nome
     * @param escopo            escopo
     * @param alvoId            alvo do escopo
     * @param qtdMinima         quantidade mínima
     * @param tamanhoGrupo      tamanho do grupo
     * @param unidadesPremiadas unidades premiadas por grupo
     * @param tipoDesconto      efeito
     * @param valor             valor
     * @param inicioVigencia    início da vigência
     * @param fimVigencia       fim da vigência
     * @return promoção provisória válida
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public static Promocao rascunho(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima,
                                    int tamanhoGrupo, int unidadesPremiadas, TipoDesconto tipoDesconto,
                                    BigDecimal valor, LocalDate inicioVigencia, LocalDate fimVigencia)
            throws DadosInvalidosException {
        validar(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
        Promocao p = new Promocao(0);
        p.atribuir(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
        return p;
    }

    /** @return próximo ID que será atribuído a uma promoção */
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

    private static void validar(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima,
                                int tamanhoGrupo, int unidadesPremiadas, TipoDesconto tipo, BigDecimal valor,
                                LocalDate inicio, LocalDate fim) throws DadosInvalidosException {
        Validacao.texto(nome, "nome da promoção");
        if (escopo == null || tipo == null) {
            throw new DadosInvalidosException("Escopo e efeito da promoção são obrigatórios.");
        }
        if (escopo != EscopoPromocao.TODOS && alvoId == null) {
            throw new DadosInvalidosException("Informe a categoria ou o produto da promoção.");
        }
        Validacao.inteiroPositivo(qtdMinima, "quantidade mínima");
        Validacao.inteiroPositivo(tamanhoGrupo, "tamanho do grupo");
        if (unidadesPremiadas < 1 || unidadesPremiadas > tamanhoGrupo) {
            throw new DadosInvalidosException(
                    "As unidades premiadas devem estar entre 1 e o tamanho do grupo (" + tamanhoGrupo + ").");
        }
        if (valor == null || valor.compareTo(new BigDecimal("0.01")) < 0) {
            throw new DadosInvalidosException("O valor da promoção deve ser de pelo menos 0,01.");
        }
        if (tipo == TipoDesconto.PERCENTUAL && valor.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DadosInvalidosException("O percentual deve estar entre 0,01 e 100.");
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new DadosInvalidosException("O fim da vigência não pode ser anterior ao início.");
        }
    }

    private void atribuir(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima, int tamanhoGrupo,
                          int unidadesPremiadas, TipoDesconto tipo, BigDecimal valor,
                          LocalDate inicio, LocalDate fim) {
        this.nome = nome.trim();
        this.escopo = escopo;
        this.alvoId = escopo == EscopoPromocao.TODOS ? null : alvoId;
        this.qtdMinima = qtdMinima;
        this.tamanhoGrupo = tamanhoGrupo;
        this.unidadesPremiadas = unidadesPremiadas;
        this.tipoDesconto = tipo;
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.inicioVigencia = inicio;
        this.fimVigencia = fim;
    }

    /**
     * Altera todos os campos da promoção, validando antes de gravar (nada muda se houver erro).
     *
     * @param nome              nome
     * @param escopo            escopo
     * @param alvoId            alvo do escopo
     * @param qtdMinima         quantidade mínima
     * @param tamanhoGrupo      tamanho do grupo
     * @param unidadesPremiadas unidades premiadas por grupo
     * @param tipoDesconto      efeito
     * @param valor             valor do efeito
     * @param inicioVigencia    início da vigência
     * @param fimVigencia       fim da vigência
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public void atualizar(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima, int tamanhoGrupo,
                          int unidadesPremiadas, TipoDesconto tipoDesconto, BigDecimal valor,
                          LocalDate inicioVigencia, LocalDate fimVigencia) throws DadosInvalidosException {
        validar(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
        atribuir(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipoDesconto, valor,
                inicioVigencia, fimVigencia);
    }

    /**
     * Ativa ou desativa a promoção.
     *
     * @param ativa {@code true} para ativar
     */
    public void ativar(boolean ativa) {
        this.ativa = ativa;
    }

    /** @return {@code true} se a promoção está ativa */
    public boolean isAtiva() {
        return ativa;
    }

    /** @return nome da promoção */
    public String getNome() {
        return nome;
    }

    /** @return escopo */
    public EscopoPromocao getEscopo() {
        return escopo;
    }

    /** @return ID da categoria ou do produto alvo, ou {@code null} */
    public Integer getAlvoId() {
        return alvoId;
    }

    /** @return quantidade mínima */
    public int getQtdMinima() {
        return qtdMinima;
    }

    /** @return tamanho do grupo */
    public int getTamanhoGrupo() {
        return tamanhoGrupo;
    }

    /** @return unidades premiadas por grupo */
    public int getUnidadesPremiadas() {
        return unidadesPremiadas;
    }

    /** @return efeito da promoção */
    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }

    /** @return valor do efeito */
    public BigDecimal getValor() {
        return valor;
    }

    /** @return primeiro dia de vigência, ou {@code null} */
    public LocalDate getInicioVigencia() {
        return inicioVigencia;
    }

    /** @return último dia de vigência, ou {@code null} */
    public LocalDate getFimVigencia() {
        return fimVigencia;
    }

    /**
     * @param hoje data de referência
     * @return {@code true} se a data está dentro da vigência
     */
    public boolean vigenteEm(LocalDate hoje) {
        return (inicioVigencia == null || !hoje.isBefore(inicioVigencia))
                && (fimVigencia == null || !hoje.isAfter(fimVigencia));
    }

    /**
     * Informa se a promoção vale para o item: ativa, vigente, escopo cobre o
     * produto e a quantidade atinge a mínima.
     *
     * @param item item do pedido
     * @param hoje data de referência
     * @return {@code true} se a promoção é aplicável
     */
    public boolean aplicaA(ItemPedido item, LocalDate hoje) {
        if (!ativa || !vigenteEm(hoje) || item.getQuantidade() < qtdMinima) {
            return false;
        }
        Produto p = item.getProduto();
        return switch (escopo) {
            case TODOS -> true;
            case CATEGORIA -> alvoId != null && p.getCategoria().getId() == alvoId;
            case PRODUTO -> alvoId != null && p.getId() == alvoId;
            default -> false;
        };
    }

    /**
     * Calcula o desconto total para uma quantidade de unidades de um preço.
     *
     * @param precoUnitario preço unitário
     * @param qtd           quantidade comprada
     * @return desconto em reais, limitado ao subtotal
     */
    public BigDecimal calcularDesconto(BigDecimal precoUnitario, int qtd) {
        int premiadas = (qtd / tamanhoGrupo) * unidadesPremiadas;
        BigDecimal desconto = tipoDesconto.descontoPorUnidade(precoUnitario, valor)
                .multiply(BigDecimal.valueOf(premiadas));
        return desconto.min(precoUnitario.multiply(BigDecimal.valueOf(qtd))).setScale(2, RoundingMode.HALF_UP);
    }

    /** @return resumo da promoção em português, exibido antes de gravar */
    public String descricao() {
        StringBuilder sb = new StringBuilder(nome).append(": ");
        switch (tipoDesconto) {
            case PERCENTUAL:
                sb.append(valor.stripTrailingZeros().toPlainString()).append("% de desconto");
                break;
            case VALOR_FIXO:
                sb.append(Moeda.formatar(valor)).append(" a menos por unidade");
                break;
            default:
                sb.append("preço promocional de ").append(Moeda.formatar(valor)).append(" por unidade");
        }
        if (tamanhoGrupo > 1 || unidadesPremiadas > 1) {
            sb.append(", em ").append(unidadesPremiadas).append(" unidade(s) a cada grupo de ")
                    .append(tamanhoGrupo);
        }
        sb.append(", ");
        switch (escopo) {
            case TODOS:
                sb.append("em toda a loja");
                break;
            case CATEGORIA:
                sb.append("na categoria de código ").append(alvoId);
                break;
            default:
                sb.append("no produto de código ").append(alvoId);
        }
        if (qtdMinima > 1) {
            sb.append(", a partir de ").append(qtdMinima).append(" unidades");
        }
        if (inicioVigencia != null || fimVigencia != null) {
            sb.append(", vigência ")
                    .append(inicioVigencia == null ? "sem início" : inicioVigencia.format(Validacao.DATA))
                    .append(" a ")
                    .append(fimVigencia == null ? "sem fim" : fimVigencia.format(Validacao.DATA));
        }
        return sb.append(ativa ? " [ativa]" : " [inativa]").toString();
    }

    @Override
    public String getDescricao() {
        return nome;
    }

    @Override
    public String toLinha() {
        return FormatoLinha.juntar(String.valueOf(id), nome, String.valueOf(ativa),
                inicioVigencia == null ? "" : inicioVigencia.toString(),
                fimVigencia == null ? "" : fimVigencia.toString(),
                escopo.name(), alvoId == null ? "" : String.valueOf(alvoId),
                String.valueOf(qtdMinima), String.valueOf(tamanhoGrupo), String.valueOf(unidadesPremiadas),
                tipoDesconto.name(), valor.toPlainString());
    }

    /**
     * Reconstrói uma promoção a partir de uma linha gravada, sem consumir o contador.
     *
     * @param linha linha produzida por {@link #toLinha()}
     * @return a promoção
     */
    public static Promocao fromLinha(String linha) {
        List<String> p = FormatoLinha.dividir(linha);
        Promocao promo = new Promocao(Integer.parseInt(p.get(0)));
        promo.nome = p.get(1);
        promo.ativa = Boolean.parseBoolean(p.get(2));
        promo.inicioVigencia = p.get(3).isEmpty() ? null : LocalDate.parse(p.get(3));
        promo.fimVigencia = p.get(4).isEmpty() ? null : LocalDate.parse(p.get(4));
        promo.escopo = EscopoPromocao.valueOf(p.get(5));
        promo.alvoId = p.get(6).isEmpty() ? null : Integer.valueOf(p.get(6));
        promo.qtdMinima = Integer.parseInt(p.get(7));
        promo.tamanhoGrupo = Integer.parseInt(p.get(8));
        promo.unidadesPremiadas = Integer.parseInt(p.get(9));
        promo.tipoDesconto = TipoDesconto.valueOf(p.get(10));
        promo.valor = new BigDecimal(p.get(11));
        return promo;
    }
}
