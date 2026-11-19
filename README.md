# Marli Store

Sistema desktop de gestão de loja virtual, feito em **Java 17** com interface em **`JOptionPane`**, persistência em **arquivos de texto** e **86 testes JUnit 5**. Trabalho final da disciplina, no Instituto de Informática da UFG.

A Marli Store é uma microempresa de varejo online (eletrônicos, beleza e utilidades domésticas) que controlava pedidos em caderno, estoque em planilha e descontos por estimativa. O sistema substitui esses controles e resolve os problemas que eles causavam:

| Problema | Como o sistema resolve |
|---|---|
| Venda de produto sem estoque | O estoque nunca fica negativo (`EstoqueInsuficienteException`) |
| Desconto manual e sem padrão | Promoções cadastradas como dados e aplicadas automaticamente |
| Sem histórico de pedidos e clientes | Histórico por cliente e relatórios de vendas por período |
| Nova categoria exige trabalho extra | Categorias com campos próprios, cadastradas pela interface |
| Dados em controles manuais | Persistência em arquivos, com backup por cópia de pasta |

## Funcionalidades

- **Catálogo:** categorias com campos específicos (garantia, validade etc.) e produtos com preço, estoque e categoria.
- **Promoções como dados:** escopo (toda a loja, categoria ou produto), condições (quantidade mínima, vigência) e efeito (percentual, valor fixo ou preço promocional, por unidade ou por grupo de N). Cobrem desconto simples, por quantidade, escalonado e "leve X pague Y", sem alterar código.
- **Clientes:** nome, CPF válido e único, e-mail e telefone, com autocadastro ao iniciar a sessão.
- **Pedidos:** vários itens, verificação de estoque, melhor promoção por item, total atualizado, conclusão e cancelamento com devolução de estoque.
- **Consultas:** histórico por cliente e relatório de vendas por período, com filtros por cliente e produto.
- **Perfis:** *Administrador* (gerencia tudo) e *Cliente* (catálogo, promoções vigentes, próprios pedidos e próprio histórico).

## Requisitos

- JDK 17 ou superior
- Maven 3.8+ (opcional; sem ele basta o JDK)

## Como executar

**Com Maven:**

```bash
mvn package                                       # compila, testa e gera target/marli-store-1.0.0.jar
java -jar target/marli-store-1.0.0.jar            # usa a pasta ./dados
java -jar target/marli-store-1.0.0.jar /outro/caminho/dados   # pasta de dados alternativa
```

**Sem Maven (só o JDK):**

```bash
mkdir -p out
javac --release 17 -encoding UTF-8 -d out $(find src/main -name '*.java')
java -cp out marlistore.Main
```

No PowerShell, troque o `javac` por:

```powershell
javac --release 17 -encoding UTF-8 -d out (Get-ChildItem -Recurse src/main -Filter *.java).FullName
```

**Pela IDE:** abra o projeto como Maven e execute a classe `marlistore.Main`.

## Primeiros passos

O repositório já traz uma pasta `dados/` com dados de exemplo (categorias, 10 produtos, 5 clientes, 9 promoções e 8 pedidos), então dá para usar o sistema logo de início:

1. Ao abrir, escolha o perfil e informe seu nome (obrigatório; identifica quem criou cada pedido).
2. Como **Cliente**, informe um CPF cadastrado, por exemplo `529.982.247-25` (Ana Souza). Um CPF novo e válido leva ao autocadastro.
3. Como **Administrador**, o menu dá acesso a categorias, produtos, reposição de estoque, clientes, promoções, pedidos e relatórios.

Para começar do zero, apague ou renomeie a pasta `dados/`: o sistema cria os arquivos ao gravar. **Backup:** copie a pasta `dados/` inteira (`categorias.dat`, `produtos.dat`, `clientes.dat`, `promocoes.dat`, `pedidos.dat`, `itens.dat` e `contadores.dat`).

## Regras de negócio principais

- **Estoque:** a soma das quantidades de um produto no pedido não pode passar do saldo. A baixa ocorre ao entrar em `EM_FINALIZACAO` (tudo ou nada); a devolução, ao voltar a `EM_ANDAMENTO` ou ao cancelar depois da baixa. Concluir não mexe no estoque.
- **Fluxo do pedido:** `VAZIO` → `EM_ANDAMENTO` → `EM_FINALIZACAO` → `CONCLUIDO`, com `CANCELADO` possível a partir dos dois estados intermediários. `CONCLUIDO` e `CANCELADO` são finais.
- **Promoções:** por item vale a de maior valor em reais entre as ativas, vigentes e do escopo do produto. Não se acumulam, o desconto não passa do subtotal do item e, em empate, vale a de menor ID.
- **Preço congelado:** o item guarda nome, preço, desconto e promoção do momento da inclusão; alterações posteriores não afetam pedidos existentes.
- **IDs:** sequenciais e nunca reaproveitados, nem após exclusão ou reinício.
- **Exclusões:** pedem confirmação e são bloqueadas quando há vínculos (categoria com produtos; produto ou cliente com pedidos).

## Arquitetura

A interface chama apenas serviços, que recebem os repositórios pelo construtor. As regras de negócio ficam no modelo e no motor de descontos, de modo que podem ser testadas sem a interface.

```
ui  →  service  →  persistencia  →  dados/*.dat
         ↓
   model · session · exception
```

| Pacote | Conteúdo |
|---|---|
| `marlistore.model` | `EntidadeBase` (abstrata), `Persistivel` (interface), `Categoria`, `Produto`, `Cliente`, `Pedido`, `ItemPedido`, `Promocao`, `StatusPedido`, `TipoDesconto` (enum com Strategy), `EscopoPromocao`, `ResultadoDesconto` e utilitários (`Validacao`, `Moeda`, `FormatoLinha`) |
| `marlistore.exception` | `EstoqueInsuficienteException`, `OperacaoNaoPermitidaException`, `DadosInvalidosException` |
| `marlistore.persistencia` | `Repositorio<T>`, `RepositorioPedidos`, `Contadores`, `BancoDeDados` |
| `marlistore.service` | `CatalogoService`, `ClienteService`, `PromocaoService`, `PedidoService`, `RelatorioService`, `MotorDescontos`, `RelatorioVendas`, `PeriodoPredefinido` |
| `marlistore.session` | `SessaoUsuario`, `PerfilUsuario` |
| `marlistore.ui` | `TelaPrincipal`, `MenuCliente`, `MenuAdmin`, `PainelRelatorios`, `Entrada`, `Formatador`, `Acao` |

Padrões usados: Repository, Service Layer, Strategy (`TipoDesconto`), State (`StatusPedido`), Value Object (`ItemPedido`, `ResultadoDesconto`) e injeção de dependência (`Main` cria os repositórios e os injeta nos serviços).

## Persistência

Cada entidade é gravada em uma linha de texto (`toLinha`) e reconstruída por um método estático (`fromLinha`), em arquivos dentro de `dados/`.

- **Gravação segura:** o conteúdo vai para um arquivo temporário que substitui o definitivo ao final.
- **Carga:** segue a ordem de dependência (categorias, produtos, clientes, promoções, pedidos e itens) e resolve as referências por ID. Linhas ilegíveis vão para o log (`java.util.logging`) sem impedir a abertura.
- **Contadores:** `contadores.dat` guarda o próximo ID de cada classe e nunca recua, o que impede o reaproveitamento de números após uma exclusão.

## Testes

```bash
mvn test        # executa os 86 testes JUnit 5
```

Os testes cobrem validações (CPF, datas, valores decimais), serialização, entidades, fluxo e transições do `Pedido`, os exemplos numéricos de desconto (leve 3 pague 2 = R$ 50, R$ 0 e R$ 100 com preço de R$ 50; escalonado com 12 unidades = R$ 90), disputa entre promoções, promoções vigentes, controle de acesso por perfil, exclusões com vínculo, relatórios por período e filtros, além dos indicadores de aceitação (persistência após reabrir, IDs sem reaproveitamento em 50 operações com exclusões, devolução de estoque e relatório com 1.000 pedidos em menos de 2 s).

## Documentação (Javadoc)

A pasta `docs/` não é versionada; gere o Javadoc localmente:

```bash
mvn javadoc:javadoc        # saída em target/site/apidocs/index.html
```

Ou, só com o JDK:

```bash
javadoc -encoding UTF-8 -docencoding UTF-8 -d docs -sourcepath src/main/java -subpackages marlistore
```

O documento de definição do projeto (problema, requisitos, casos de uso e diagramas de classes, versão 1.2) acompanha o trabalho e descreve UC01 a UC13, RN-01 a RN-09 e RF-01 a RF-25, todos implementados.

## Fora do escopo

Login com senha, pagamento, nota fiscal, frete, notificações, backup em nuvem, versão web ou mobile, descontos sobre o total do pedido, cupons e acúmulo de promoções. O sistema roda em uma única máquina, com um usuário por vez; sem autenticação, o perfil Cliente organiza a interface, mas não é uma barreira de segurança (o `PedidoService` também aplica a restrição de acesso aos próprios pedidos).

## Equipe

Gabriel Kötter · Guilherme Arraes · Guilherme Sales · Ronan Vieira
