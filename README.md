# Cdatra

Projeto de aprendizado em Java 17 para reduzir o trabalho repetitivo com JDBC.
Hoje possui análise de campos com `@Column`, geração de `CREATE TABLE` e `INSERT`,
execução JDBC e mapeamento de resultados para objetos. As peças ainda são chamadas
separadamente; não há uma API de CRUD pronta.

## Rodar os testes

Com JDK 17 e Maven disponíveis, na pasta do projeto:

```shell
mvn test
```

No IntelliJ IDEA, abra o `pom.xml` como projeto Maven, configure o JDK 17 e use
**Maven → Lifecycle → test**. Também é possível executar uma classe de teste pelo
ícone ao lado dela no editor. O Maven baixa JUnit e H2 na primeira execução.
Os testes usam H2 em memória: não exigem servidor nem alteram um banco externo.

Para executar só um grupo:

```shell
mvn -Dtest=AnalysisTest test
mvn -Dtest=ObjectMapperTest test
mvn -Dtest=SqlExecutorTest test
mvn -Dtest=SqlGeneratorTest test
```

São 53 casos, contando as entradas dos testes parametrizados:

| Grupo | Casos | O que verifica |
| --- | ---: | --- |
| AnalysisTest | 7 | Metadados e valores; entradas nulas; nomes vazios e duplicados; campos privados/protegidos; campos ignorados e valores nulos |
| ObjectMapperTest | 14 | Entradas inválidas; colunas ausentes/extras; nomes sem distinção de maiúsculas; nulos; tipos incompatíveis; construtor sem argumentos |
| SqlExecutorTest | 11 | Entradas inválidas; consulta vazia; múltiplas linhas e colunas; aliases; fechamento em falhas; fluxo integrado |
| SqlGeneratorTest | 21 | Validação de tabelas e INSERT; tipos suportados; escape de apóstrofos; NULL; validação após SchemaBuilder |

O teste `createsInsertsQueriesAndMapsMultipleEntities` reúne o fluxo real:

```text
Analysis → SchemaBuilder → CREATE TABLE → ValueAnalysis → INSERT
→ SELECT → ObjectMapper → conferência dos valores
```

## Regras desta revisão

- `ObjectMapper.map` recebe as colunas de **um registro**. Lista nula/vazia,
  item nulo ou nome de coluna nulo/em branco gera `IllegalArgumentException`.
- Colunas extras são ignoradas; uma `@Column` ausente no resultado é erro.
  `null` em campo objeto é permitido; em primitivo é rejeitado.
- A entidade mapeada precisa de um construtor sem argumentos acessível.
  Não há conversão automática entre tipos incompatíveis.
- `SqlExecutor.query` retorna uma lista vazia se o SELECT não encontrar registros.
  Cada item dessa lista representa um registro; só os registros existentes vão ao mapper.
- `Statement` e `ResultSet` são fechados mesmo em falhas. Quem forneceu a
  `Connection` continua responsável por fechá-la.
- `Analysis` e `ValueAnalysis` rejeitam nomes de `@Column` em branco ou duplicados,
  incluindo nomes que diferem apenas por maiúsculas/minúsculas.
- Entidades sem `@Column` produzem listas vazias na análise. `SchemaBuilder`,
  `Table`, `Row` e `ColumnDefinition` continuam simples; a validação da tabela
  fica no `SqlGenerator`, tanto para CREATE quanto para INSERT.

## Avaliação e próximos passos

A avaliação do prompt faz sentido quanto à base: reflection, metadados, SQL,
JDBC e retorno para objetos já estão presentes. O teste integrado confirma esse
ciclo. A estimativa de “65%–70%” não tem critério verificável; é mais útil medir
o que a biblioteca consegue fazer.

`@Entity` existe, mas seu nome de tabela ainda não é consumido pelas peças atuais:
o nome é passado manualmente ao `SchemaBuilder`. `UPDATE`, `DELETE` e uma camada
que coordene as peças ainda faltam para o objetivo de reaproveitar CRUD nos próximos
projetos. Eles não foram implementados nesta revisão.

Para praticar Java, a estrutura atual é uma base coerente e não precisa agora de
relacionamentos, migrations, cache ou lazy loading. Depois desta bateria, faz
sentido avançar em uma funcionalidade útil por vez. A definição de `v0.0.1` é sua;
CRUD completo é um marco razoável para o seu objetivo, mas não uma regra universal.

Antes de reutilizar com dados externos, um próximo passo importante será usar
parâmetros JDBC (`PreparedStatement`) em vez de montar os valores dentro do SQL.
Nesta revisão, foi mantido e testado o escape de apóstrofos já existente.
Também foram preservados os nomes atuais da API (`analize`, `columName` e
`tableNaame`) para evitar uma renomeação fora do escopo.
