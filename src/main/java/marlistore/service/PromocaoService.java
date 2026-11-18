package marlistore.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import marlistore.exception.DadosInvalidosException;
import marlistore.model.Categoria;
import marlistore.model.EscopoPromocao;
import marlistore.model.Produto;
import marlistore.model.Promocao;
import marlistore.model.TipoDesconto;
import marlistore.persistencia.Repositorio;

/**
 * Serviço de promoções (RF-06). Promoções são dados: um desconto novo é uma
 * combinação nova de campos, sem recompilar (RNF-13).
 */
public class PromocaoService {

    private final Repositorio<Promocao> promocoes;
    private final Repositorio<Categoria> categorias;
    private final Repositorio<Produto> produtos;

    /**
     * @param promocoes  repositório de promoções
     * @param categorias repositório de categorias (valida o alvo do escopo)
     * @param produtos   repositório de produtos (valida o alvo do escopo)
     */
    public PromocaoService(Repositorio<Promocao> promocoes, Repositorio<Categoria> categorias,
                           Repositorio<Produto> produtos) {
        this.promocoes = promocoes;
        this.categorias = categorias;
        this.produtos = produtos;
    }

    private void validarAlvo(EscopoPromocao escopo, Integer alvoId) throws DadosInvalidosException {
        if (escopo == EscopoPromocao.CATEGORIA && (alvoId == null || categorias.obter(alvoId) == null)) {
            throw new DadosInvalidosException("Categoria da promoção não encontrada.");
        }
        if (escopo == EscopoPromocao.PRODUTO && (alvoId == null || produtos.obter(alvoId) == null)) {
            throw new DadosInvalidosException("Produto da promoção não encontrado.");
        }
    }

    /**
     * Cadastra uma promoção ativa.
     *
     * @param nome              nome
     * @param escopo            escopo
     * @param alvoId            categoria ou produto alvo (nulo para toda a loja)
     * @param qtdMinima         quantidade mínima
     * @param tamanhoGrupo      tamanho do grupo
     * @param unidadesPremiadas unidades premiadas por grupo
     * @param tipo              efeito
     * @param valor             valor do efeito
     * @param inicio            início da vigência (opcional)
     * @param fim               fim da vigência (opcional)
     * @return a promoção criada
     * @throws DadosInvalidosException se algum dado for inválido
     */
    public Promocao cadastrar(String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima,
                              int tamanhoGrupo, int unidadesPremiadas, TipoDesconto tipo, BigDecimal valor,
                              LocalDate inicio, LocalDate fim) throws DadosInvalidosException {
        validarAlvo(escopo, alvoId);
        Promocao p = new Promocao(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipo,
                valor, inicio, fim);
        promocoes.incluir(p);
        promocoes.salvar();
        return p;
    }

    /**
     * Altera todos os campos de uma promoção.
     *
     * @param id                ID da promoção
     * @param nome              nome
     * @param escopo            escopo
     * @param alvoId            alvo
     * @param qtdMinima         quantidade mínima
     * @param tamanhoGrupo      tamanho do grupo
     * @param unidadesPremiadas unidades premiadas por grupo
     * @param tipo              efeito
     * @param valor             valor
     * @param inicio            início da vigência
     * @param fim               fim da vigência
     * @return a promoção alterada
     * @throws DadosInvalidosException se não existir ou algum dado for inválido
     */
    public Promocao alterar(int id, String nome, EscopoPromocao escopo, Integer alvoId, int qtdMinima,
                            int tamanhoGrupo, int unidadesPremiadas, TipoDesconto tipo, BigDecimal valor,
                            LocalDate inicio, LocalDate fim) throws DadosInvalidosException {
        Promocao p = buscar(id);
        validarAlvo(escopo, alvoId);
        p.atualizar(nome, escopo, alvoId, qtdMinima, tamanhoGrupo, unidadesPremiadas, tipo, valor, inicio, fim);
        promocoes.salvar();
        return p;
    }

    /**
     * Ativa ou desativa uma promoção.
     *
     * @param id    ID da promoção
     * @param ativa {@code true} para ativar
     * @throws DadosInvalidosException se não existir
     */
    public void ativar(int id, boolean ativa) throws DadosInvalidosException {
        buscar(id).ativar(ativa);
        promocoes.salvar();
    }

    /**
     * Exclui uma promoção. Pedidos existentes mantêm o nome e o desconto congelados (RN-08).
     *
     * @param id ID da promoção
     * @throws DadosInvalidosException se não existir
     */
    public void excluir(int id) throws DadosInvalidosException {
        buscar(id);
        promocoes.remover(id);
        promocoes.salvar();
    }

    /**
     * @param id ID da promoção
     * @return a promoção
     * @throws DadosInvalidosException se não existir
     */
    public Promocao buscar(int id) throws DadosInvalidosException {
        return promocoes.buscarPorId(id)
                .orElseThrow(() -> new DadosInvalidosException("Promoção " + id + " não encontrada."));
    }

    /** @return todas as promoções */
    public List<Promocao> listar() {
        return promocoes.listar();
    }

    /**
     * @param hoje data de referência
     * @return promoções ativas e vigentes na data
     */
    public List<Promocao> listarVigentes(LocalDate hoje) {
        List<Promocao> r = promocoes.listar();
        r.removeIf(p -> !p.isAtiva() || !p.vigenteEm(hoje));
        return r;
    }

    /**
     * Resumo em português da promoção, com o nome da categoria ou do produto alvo.
     *
     * @param p         promoção
     * @param comStatus {@code true} para incluir [ativa]/[inativa] (visão do Administrador)
     * @return texto descritivo
     */
    public String descrever(Promocao p, boolean comStatus) {
        String alvo = null;
        if (p.getEscopo() == EscopoPromocao.CATEGORIA && p.getAlvoId() != null) {
            Categoria c = categorias.obter(p.getAlvoId());
            alvo = c == null ? null : c.getNome();
        } else if (p.getEscopo() == EscopoPromocao.PRODUTO && p.getAlvoId() != null) {
            Produto pr = produtos.obter(p.getAlvoId());
            alvo = pr == null ? null : pr.getNome();
        }
        return comStatus ? p.descricao(alvo) : p.descricaoPublica(alvo);
    }

    /**
     * Promoções ativas e vigentes cujo escopo cobre o produto.
     *
     * @param produto produto consultado
     * @param hoje    data de referência
     * @return promoções vigentes que incluem o produto
     */
    public List<Promocao> promocoesDoProduto(Produto produto, LocalDate hoje) {
        List<Promocao> r = listarVigentes(hoje);
        r.removeIf(p -> !p.cobreProduto(produto));
        return r;
    }
}
