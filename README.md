# Cdatra

Projeto de aprendizado em Java 17 para reduzir o trabalho repetitivo com JDBC.
Repository coordena análise de entidades, geração de SQL, execução JDBC e
mapeamento de resultados para objetos.

Hoje há criação de tabela, INSERT com retorno de chave gerada, buscas, UPDATE e
DELETE. As entidades usam `@Entity`, `@Column`, `@Id` e `@GeneratedValue`. Os nomes
atuais da API foram preservados: `analize`, `columName` e `tableNaame`.

## Organização dos packages

O código de produção usa a raiz `io.github.lordcarvel.cdatra`, dividido por
responsabilidade:

| Package | Classes |
| --- | --- |
| `annotation` | Column, Entity, Id, GeneratedValue |
| `metadata` | Analysis, ColumnDefinition, Table, SchemaBuilder |
| `mapping` | ValueAnalysis, ObjectMapper, Row, Container |
| `query` | Operator, LogicalOperator, QueryCondition, QueryFilter |
| `sql` | SqlGenerator, SqlType, TypeMapper |
| `jdbc` | DatabaseConnection, SqlExecutor |
| `repository` | Repository |

Main, User e Address ficam em `src/test/java/io/github/lordcarvel/cdatra/example/`
e continuam sendo usados pelos testes. Esses exemplos não fazem parte do JAR.
A classe vazia Schema foi removida.

A reorganização preserva os corpos dos métodos e os testes existentes. Código
que usava os packages anteriores precisa atualizar seus imports. As coordenadas
Maven permanecem definidas no `pom.xml`, sem alteração nesta reorganização.

## Avaliação da v0.0.1

A validação final de 08/10/2026 possui **224 casos distintos**, todos passando:

- Zero falhas, zero erros e zero testes ignorados na suíte padrão e na auditoria.
- Os 25 casos que antes falhavam foram corrigidos e integrados à execução padrão.
- Cobertura na revisão das correções: **96,88% das linhas** e **94,22% das decisões**.

Foram corrigidos o escape da chave textual no UPDATE, a validação do DELETE,
labels duplicados no mapper, herança de campos, campos static, entidades com
apenas ID gerado e validações de tipos, nomes e valores não finitos. As assinaturas
existentes foram preservadas, sem criar métodos de produção.

O [relatório das correções](docs/testing/v0.0.1-fixes.md) apresenta o resultado
atual e seus limites. As [evidências finais](docs/testing/v0.0.1-fixed-results.json)
registram os casos e a cobertura. O
[relatório anterior](docs/testing/v0.0.1-report.md) e seu
[inventário](docs/testing/v0.0.1-results.json) permanecem como histórico.

Os relatórios versionados registram a revisão das correções, anterior à
reorganização dos packages. A versão do artefato é definida no `pom.xml`.

## Rodar os testes

Com JDK 17 e Maven, na raiz do projeto:

```shell
mvn test
mvn -Pcoverage clean verify
```

Os testes usam H2 em memória, sem servidor e sem alterar bancos externos. Maven
baixa as dependências na primeira execução. No IntelliJ IDEA, abra o `pom.xml`
como projeto Maven, configure o JDK 17 e use **Maven → Lifecycle → test**.

O perfil `coverage` gera HTML, XML e CSV em `target/site/jacoco/`. O comando
`verify` também gera o JAR em `target/`.

Para executar grupos individuais:

```shell
mvn -Dtest=AnalysisTest,EntityMetadataTest test
mvn -Dtest=TypeMapperTest test
mvn -Dtest=ObjectMapperTest,ObjectMapperEdgeTest test
mvn -Dtest=SqlGeneratorTest,SqlQueryTest test
mvn -Dtest=SqlExecutorTest,GeneratedKeyExecutorTest test
mvn -Dtest=RepositoryIntegrationTest test
mvn -Dtest=SystemSmokeTest test
```

Para repetir a auditoria ou executar somente os contratos de regressão:

```shell
mvn -Pdefect-audit test
mvn -Pdefect-audit -Dtest=KnownDefectsTest test
```

**Ambos os comandos devem passar.** O nome `KnownDefectsTest` foi mantido como
referência à auditoria original. A tag e a exclusão foram removidas: todos esses
casos também rodam em `mvn test`. O perfil `defect-audit` apenas separa os arquivos
de resultados em outro diretório.

Resultados ficam em `target/surefire-reports` e, para a auditoria, em
`target/defect-audit-reports`. O comando `clean` remove esses arquivos. O JSON
versionado é uma fotografia da avaliação, sem atualização automática por Maven.

## Comportamento verificado

- Repository oferece `createTable`, `save`, `findAll`, `findById`, `findBy`,
  `findByConditions`, `findByFilters`, `update` e `delete`.
- IDs Integer/Long gerados são atribuídos à entidade após save; IDs manuais também
  foram testados. Operações por ID exigem `@Id`.
- O mapper recebe as colunas de um registro. Colunas extras são ignoradas; colunas
  anotadas ausentes são erro. O construtor sem argumentos precisa ser acessível.
- Nulos são aceitos em campos objeto; campos primitivos não podem receber null.
  Os tipos suportados são int/Integer, long/Long, boolean/Boolean, double/Double
  e String. Não há uma camada geral de conversão de tipos.
- Listas de QueryCondition usam AND. QueryFilter acrescenta AND/OR, com a
  precedência nativa de SQL e sem agrupamento explícito por parênteses. A API não
  garante ordenação sem ORDER BY.
- Buscas sem resultado retornam lista vazia; findById retorna null.
- Statement, PreparedStatement e ResultSet são fechados nos caminhos testados de
  sucesso e falha. Quem forneceu a Connection continua responsável por fechá-la.
- Commit e rollback pertencem ao chamador. Foram testados CRUD em transações e
  visibilidade entre duas conexões; não foi testado acesso simultâneo por threads.
- Campos herdados participam da análise e do mapeamento; campos static e sintéticos
  são ignorados. IDs gerados precisam ser int/Integer ou long/Long.
- Tabelas e colunas devem usar identificadores simples: `[A-Za-z_][A-Za-z0-9_]*`.
  As consultas exigem valores de tipos compatíveis com os tipos SQL das colunas.
- Entidades com apenas um ID gerado usam DEFAULT VALUES no save. Sem campos para
  atualizar, Repository.update valida o ID e não executa um UPDATE vazio.

A avaliação foi feita com H2 2.5.250 em memória. Não certifica outros bancos,
persistência em disco ou desempenho em produção. Nomes reservados pelo banco
precisam ser evitados; a API não faz quoting de identificadores. O gerador mantém
valores dentro do SQL com escape de texto, sem introduzir uma API de parâmetros.
