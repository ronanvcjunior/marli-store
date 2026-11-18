# Marli Store — Sistema de Gestão de Loja Virtual

Sistema desktop em **Java 17+**, de uso local, com interface exclusivamente em `JOptionPane`,
persistência em arquivos de texto (`dados/`), documentação **Javadoc** e testes **JUnit 5**.
Implementa o documento "Definição, Requisitos e Modelagem" (v1.0): UC01–UC13, RN-01–RN-09, RF-01–RF-25.

## Como executar

Com Maven:

```bash
mvn test                      # roda os 81 testes JUnit
mvn javadoc:javadoc           # gera target/site/apidocs
mvn package                   # gera target/marli-store-1.0.0.jar
java -jar target/marli-store-1.0.0.jar          # usa a pasta ./dados
java -jar target/marli-store-1.0.0.jar /caminho/dados   # pasta de dados alternativa
```

Sem Maven (apenas JDK 17+):

```bash
mkdir -p out && javac --release 17 -d out $(find src/main -name '*.java')
java -cp out marlistore.Main
javadoc -encoding UTF-8 -docencoding UTF-8 -d docs/api -sourcepath src/main/java -subpackages marlistore
```

A pasta `docs/api/` já contém o Javadoc gerado (abra `docs/api/index.html`).
**Backup:** copie a pasta `dados/` (`categorias.dat`, `produtos.dat`, `clientes.dat`, `promocoes.dat`,
`pedidos.dat`, `itens.dat`, `contadores.dat`).

## Estrutura (RNF-02)

| Pacote | Conteúdo |
|---|---|
| `marlistore.model` | `EntidadeBase` (abstrata), `Persistivel` (interface), `Categoria`, `Produto`, `Cliente`, `Pedido`, `ItemPedido`, `Promocao`, `StatusPedido`, `TipoDesconto` (enum com Strategy), `EscopoPromocao`, `ResultadoDesconto`, utilitários `Validacao`, `Moeda`, `FormatoLinha` |
| `marlistore.exception` | `EstoqueInsuficienteException`, `OperacaoNaoPermitidaException`, `DadosInvalidosException` |
| `marlistore.persistencia` | `Repositorio<T>`, `RepositorioPedidos`, `Contadores`, `BancoDeDados` |
| `marlistore.service` | `CatalogoService`, `ClienteService`, `PromocaoService`, `PedidoService`, `RelatorioService`, `MotorDescontos`, `RelatorioVendas`, `PeriodoPredefinido` |
| `marlistore.session` | `SessaoUsuario`, `PerfilUsuario` |
| `marlistore.ui` | `TelaPrincipal`, `MenuCliente`, `MenuAdmin`, `PainelRelatorios`, `Entrada`, `Formatador`, `Acao` |

## Decisões de implementação

- **IDs (RN-02, RNF-04):** contador estático `proximoId` em cada classe concreta, incrementado no construtor
  *somente depois* de validar os dados (entrada inválida não gasta ID). Os contadores são gravados em
  `contadores.dat` a cada gravação e nunca recuam; o carregamento ainda os protege pelo maior ID existente.
- **Estoque (RN-01, RN-04):** baixa ao entrar em `EM_FINALIZACAO` (tudo ou nada), devolução ao voltar a
  `EM_ANDAMENTO` ou cancelar após a baixa; concluir não mexe no estoque.
- **Promoções (RN-07):** uma única classe de dados; o `MotorDescontos` escolhe, por item, a de maior valor em
  reais (empate: menor ID), sem acúmulo e limitada ao subtotal. Os descontos são recalculados a cada alteração
  enquanto o pedido está em edição e ficam congelados desde a finalização (RN-08).
- **Persistência (RNF-07):** gravação em arquivo temporário substituído ao final; linhas ilegíveis vão para o
  log (`java.util.logging`) sem impedir a abertura.
- **Pequenas extensões ao diagrama:** `BancoDeDados` (ordem de carga e contadores), `RepositorioPedidos`
  (pedidos + `itens.dat`), `Acao` (tratamento central de exceções na interface), `Promocao.rascunho`
  (resumo do assistente sem gastar ID) e os métodos `fromLinha` de `Produto`/`Pedido`/`ItemPedido` recebem uma
  função resolvedora de referências por ID, como descrito na seção 5.6 do documento.
- **Relatório com filtro de produto:** os totais somam apenas os itens que correspondem ao produto filtrado.
- **Sem autenticação:** o perfil Cliente organiza a interface, mas o `PedidoService` também aplica a RN-06.

## Testes (JUnit 5) — 81 testes

Cobrem validações (CPF, datas, decimais), serialização, entidades, fluxo e transições de `Pedido`, os exemplos
numéricos da seção 5.3 (leve 3 pague 2 = R$ 50/0/100; escalonado 12 un. = R$ 90; 10% a partir de 3 = R$ 15),
disputa entre promoções, controle de acesso por perfil, exclusões com vínculo (RN-09), relatórios por período e
filtros, e os indicadores KPI-01 a KPI-08 (persistência após reabrir, IDs sem reaproveitamento em 50 operações
com exclusões, devolução de estoque, desempenho com 1.000 pedidos).
