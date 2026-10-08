# Cdatra

Projeto de aprendizado em Java 17 para reduzir o trabalho repetitivo com JDBC.
Repository coordena análise de entidades, geração de SQL, execução JDBC e
mapeamento de resultados para objetos.

Hoje há criação de tabela, INSERT com retorno de chave gerada, buscas, UPDATE e
DELETE. As entidades usam `@Entity`, `@Column`, `@Id` e `@GeneratedValue`. Os nomes
atuais da API foram preservados: `analize`, `columName` e `tableNaame`.

## Avaliação da v0.0.1

A bateria de 08/10/2026 possui **206 casos distintos**:

- **181 casos regulares passam**, individualmente por classe e na suíte conjunta.
- **25 casos de auditoria falham**, reproduzindo 11 grupos de defeitos e limitações.
- Cobertura regular: **97,90% das linhas** e **98,31% das decisões**.

O UPDATE com chave textual contendo apóstrofo pode alterar registros indevidos.
O DELETE direto pelo SqlGenerator aceita identificadores que ampliam sua condição.
Há sobrescrita silenciosa de labels duplicados no mapper e validações
inconsistentes. **Um build regular verde não aprova a release.**

O [relatório completo](docs/testing/v0.0.1-report.md) apresenta casos, reproduções,
prioridades e limites. O [inventário de resultados](docs/testing/v0.0.1-results.json)
registra cada caso, os resultados por classe e a cobertura medida.

Esta revisão não corrigiu o código de produção. v0.0.1 identifica o marco
solicitado; o `pom.xml` continua com `1.0-SNAPSHOT`.

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

Para incluir todos os contratos de defeitos ou executar somente esses casos:

```shell
mvn -Pdefect-audit test
mvn -Pdefect-audit -Dtest=KnownDefectsTest test
```

**A auditoria deve falhar na revisão avaliada.** Seus testes têm a tag
`known-defect`, excluída explicitamente do perfil regular. Não estão desabilitados
e não aprovam o comportamento incorreto: verificam o contrato esperado e mostram
a falha atual. Quando corrigidos, devem passar e sair dessa categoria.

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

A avaliação foi feita com H2 2.5.250 em memória. Não certifica outros bancos,
persistência em disco ou desempenho em produção. O relatório detalha os problemas
com herança, campos static, entidade com apenas ID gerado, valores não finitos
e entradas inválidas.
