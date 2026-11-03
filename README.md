# Cdatra

## O que é

Cdatra é uma biblioteca em Java para reduzir o trabalho repetitivo de acesso a
banco de dados. A partir de uma classe anotada, cria tabelas, salva registros,
faz consultas, atualiza e exclui dados usando `Repository<T>`.

Funciona com JDBC e com o **Cloudflare D1**. O mesmo `Repository` atende os dois;
muda só o adapter que ele recebe.

Uma entidade começa assim:

```java
@Entity(tableNaame = "users")
public class User {

    @Id
    @GeneratedValue
    @Column(columName = "id")
    private int id;

    @Column(columName = "name")
    private String name;
}
```

E o uso básico é:

```java
Repository<User> repository = new Repository<>(User.class, connection);

repository.createTable();

repository.save(user);

List<User> users = repository.findAll();
```

Esse resumo pressupõe uma conexão JDBC aberta e um objeto `user`. Os exemplos
abaixo mostram os imports, a entidade completa e a abertura da conexão.

## Requisitos

- JDK 17.
- Maven para compilar, instalar e executar os testes.
- Uma conexão JDBC e o driver do banco no classpath da aplicação, ou um Worker
  do Cloudflare D1 publicado (veja [Usando o Cloudflare D1](#usando-o-cloudflare-d1)).

O ambiente validado usa Java 17, Maven 3.9.14, **H2 2.5.250 em memória** e
**Cloudflare D1**. O projeto inclui o driver do H2 como dependência de runtime e
usa JUnit 5.11.4 nos testes. O acesso ao D1 não adiciona dependências: usa o
`java.net.http.HttpClient` do JDK e um leitor de JSON próprio.

## Instalação

Para instalar a biblioteca no repositório Maven local:

```shell
git clone https://github.com/LordCarvel/Cdatra.git
cd Cdatra
mvn clean install
```

Depois, adicione esta dependência ao `pom.xml` da sua aplicação:

```xml
<dependency>
    <groupId>org.com.dev.carvel</groupId>
    <artifactId>Cdatra</artifactId>
    <version>0.0.2</version>
</dependency>
```

Essas são as coordenadas Maven atuais. Os packages Java usam a raiz
`io.github.lordcarvel.cdatra`. A instalação descrita usa o build local; não
pressupõe publicação no Maven Central.

O build gera `target/Cdatra-0.0.2.jar`. Esse JAR não contém suas dependências;
num projeto Maven, o driver H2 é resolvido pela dependência de runtime.

## Primeira entidade

Crie `User.java` na sua aplicação. Este exemplo adiciona apenas os construtores
e acessores usados nas próximas seções:

```java
package example;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.Entity;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;

@Entity(tableNaame = "users")
public class User {

    @Id
    @GeneratedValue
    @Column(columName = "id")
    private int id;

    @Column(columName = "name")
    private String name;

    public User () {

    }

    public User (String name) {

        this.name = name;
    }

    public int getId () {

        return id;
    }

    public String getName () {

        return name;
    }

    public void setName (String name) {

        this.name = name;
    }
}
```

Para carregar resultados, a classe precisa de um construtor sem argumentos
acessível. Os campos anotados podem ser privados: o mapeamento usa reflexão.
Campos sem `@Column` são ignorados.

Os atributos se chamam **`tableNaame`** e **`columName`** na API atual. Use essa
grafia nos exemplos.

## Criando Repository

O Repository recebe a classe da entidade e uma `java.sql.Connection` aberta.
Este `Main.java`, no mesmo package da entidade acima, executa o ciclo básico:

```java
package example;

import io.github.lordcarvel.cdatra.repository.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {

    public static void main (String[] args) throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:cdatra_example", "sa", "")) {

            Repository<User> repository = new Repository<>(User.class, connection);
            repository.createTable();

            User user = new User("Carvel");
            repository.save(user);

            List<User> users = repository.findAll();

            for (User saved : users) {

                System.out.println(saved.getId() + " | " + saved.getName());
            }
        }
    }
}
```

O banco desse exemplo existe em memória enquanto a conexão estiver aberta.
Quem fornece a conexão é responsável por fechá-la. O `throws Exception` do exemplo
existe por causa do `DriverManager.getConnection`: as operações do `Repository`
não declaram exceções checadas (veja [Erros](#erros)).

As próximas seções mostram trechos de uso dentro de uma conexão aberta. São
exemplos separados; não é necessário criar a mesma tabela novamente depois de
executar o ciclo acima.

## Criando tabela

```java
repository.createTable();
```

Usa o nome de `@Entity` e as colunas de `@Column`. O campo com `@Id` vira a chave
primária; com `@GeneratedValue`, o banco gera seu valor.

A chamada executa `CREATE TABLE`, sem `IF NOT EXISTS`. Se a tabela já existe, o
banco pode retornar erro. Não há atualização automática de uma tabela existente.

## Salvando

```java
User user = new User("Carvel");
repository.save(user);

int generatedId = user.getId();
```

`save` executa um INSERT. O ID gerado pelo banco é atribuído ao próprio objeto;
o valor inicial desse campo não é enviado no INSERT. IDs manuais também são
aceitos quando o campo tem `@Id` e não tem `@GeneratedValue`.

`save` não escolhe automaticamente entre INSERT e UPDATE. Para alterar um
registro existente, use `update`.

## Consultando

```java
List<User> users = repository.findAll();

User userById = repository.findById(user.getId());

List<User> matches = repository.findBy("name", "Carvel", Operator.EQUAL);
```

Para a última consulta, importe:

```java
import io.github.lordcarvel.cdatra.query.Operator;
```

`findById` retorna `null` quando o registro não existe. As buscas que retornam
listas devolvem uma lista vazia quando não há resultados. Sem ORDER BY, a ordem
dos resultados não é garantida.

O nome passado a `findBy` é o nome da coluna declarado em `@Column`.

## Atualizando

```java
user.setName("Carvel atualizado");
repository.update(user);
```

O UPDATE usa o ID do objeto para localizar o registro e atualiza os demais
campos anotados. O ID não é alterado. Valores nulos em campos objeto também são
enviados, podendo substituir valores existentes por NULL.

Não há acompanhamento automático de mudanças. Alterar o objeto só afeta o banco
quando `update` é chamado. A operação retorna `void`, sem informar a quantidade
de registros afetados.

## Excluindo

```java
repository.delete(user);
```

O DELETE usa o ID do objeto. `findById`, `update` e `delete` exigem uma entidade
com `@Id`. A operação retorna `void`; excluir um ID inexistente não gera um
resultado indicando se algum registro foi removido.

## Consultas com operadores

Importe `io.github.lordcarvel.cdatra.query.Operator`. Os operadores disponíveis são:

| Operador | Comparação SQL |
| --- | --- |
| `Operator.EQUAL` | `=` |
| `Operator.NOT_EQUAL` | `!=` |
| `Operator.GREATER_THAN` | `>` |
| `Operator.LESS_THAN` | `<` |
| `Operator.GREATER_THAN_OR_EQUAL` | `>=` |
| `Operator.LESS_THAN_OR_EQUAL` | `<=` |
| `Operator.LIKE` | `LIKE` |

Exemplo:

```java
List<User> users = repository.findBy(
        "id",
        10,
        Operator.GREATER_THAN
);
```

Representa a condição:

```sql
id > 10
```

Nulos podem ser consultados com `EQUAL` e `NOT_EQUAL`, que geram `IS NULL` e
`IS NOT NULL`:

```java
List<User> withoutName = repository.findBy("name", null, Operator.EQUAL);
```

Comparações de ordem com null são rejeitadas. O valor precisa ter um tipo
compatível com a coluna: para um ID `int`, use `10`; para um ID `long`, use `10L`.

`LIKE` busca por parte de um texto. Quem chama escreve os curingas: `%` vale por
qualquer sequência de caracteres, e `_`, por exatamente um.

```java
List<User> users = repository.findBy("name", "Car%", Operator.LIKE);
```

O valor precisa ser um texto; `null` e números são recusados. O comportamento
depende do banco: veja as diferenças em [Limitações](#limitações).

## Consultas AND/OR

Importe os tipos usados nos exemplos:

```java
import io.github.lordcarvel.cdatra.query.LogicalOperator;
import io.github.lordcarvel.cdatra.query.Operator;
import io.github.lordcarvel.cdatra.query.QueryCondition;
import io.github.lordcarvel.cdatra.query.QueryFilter;
import java.util.List;
```

Para combinar condições apenas com AND, use `findByConditions`:

```java
QueryCondition condition1 = new QueryCondition("id", 1, Operator.GREATER_THAN);
QueryCondition condition2 = new QueryCondition("name", "Carvel", Operator.EQUAL);

List<User> users = repository.findByConditions(List.of(condition1, condition2));
```

Representa `id > 1 AND name = 'Carvel'`.

Para escolher AND ou OR entre condições, use `findByFilters`:

```java
QueryCondition condition1 = new QueryCondition("id", 1, Operator.GREATER_THAN);
QueryCondition condition2 = new QueryCondition("name", "Carvel", Operator.EQUAL);

List<User> andUsers = repository.findByFilters(List.of(
        new QueryFilter(condition1, null),
        new QueryFilter(condition2, LogicalOperator.AND)
));

List<User> orUsers = repository.findByFilters(List.of(
        new QueryFilter(condition1, null),
        new QueryFilter(condition2, LogicalOperator.OR)
));
```

O operador lógico de cada filtro liga sua condição à anterior. O primeiro não
tem condição anterior, então seu operador é ignorado e pode ser `null`. A partir
do segundo filtro, o operador lógico é obrigatório.

Listas de condições ou filtros precisam ter pelo menos um elemento. Na mistura
de AND e OR, vale a precedência nativa do SQL: AND é avaliado antes de OR.

Para mudar essa ordem, agrupe filtros com `QueryFilter.group`. Cada grupo vira um
par de parênteses:

```java
QueryFilter idIsOne = new QueryFilter(new QueryCondition("id", 1, Operator.EQUAL), null);
QueryFilter nameIsCarvel = new QueryFilter(new QueryCondition("name", "Carvel", Operator.EQUAL), LogicalOperator.OR);
QueryFilter idAboveTwo = new QueryFilter(new QueryCondition("id", 2, Operator.GREATER_THAN), LogicalOperator.AND);

List<User> users = repository.findByFilters(List.of(
        QueryFilter.group(List.of(idIsOne, nameIsCarvel), null),
        idAboveTwo
));
```

Representa `(id = 1 OR name = 'Carvel') AND id > 2`. O operador lógico do grupo o
liga ao filtro anterior, como em um filtro comum. Um grupo pode conter outros
grupos, mas não pode ficar vazio.

## Usando o Cloudflare D1

A aplicação Java não fala direto com o D1: ela envia os comandos por HTTP a um
Worker próprio, que tem acesso ao banco. Assim, a aplicação guarda apenas o segredo
desse Worker, e não um token com poder sobre a conta da Cloudflare. O código do
Worker está na pasta `worker/`.

### Publicando o Worker

Dentro de `worker/`, entre na sua conta e crie o banco:

```shell
npx wrangler login
npx wrangler d1 create cdatra
```

Coloque o `database_id` mostrado pelo comando em `worker/wrangler.toml`. Depois
publique o Worker e configure o segredo:

```shell
npx wrangler deploy
npx wrangler secret put CDATRA_SECRET
```

Enquanto o segredo não existir, o Worker recusa todos os pedidos. Use um valor
longo e aleatório, e não o coloque no código nem no git.

Em um repositório público, não é preciso publicar os dados da conta: copie o
arquivo para `worker/wrangler.prod.toml`, que o git ignora, coloque os valores
reais nele e publique com `npx wrangler deploy --config wrangler.prod.toml`.

Para testar sem usar a conta da Cloudflare, crie `worker/.dev.vars`, também
ignorado pelo git, com `CDATRA_SECRET=<um valor de teste>`, e rode
`npx wrangler dev`. O banco fica na sua máquina.

### Conectando a aplicação

Crie um `D1DatabaseAdapter` com a URL do Worker e o segredo, e entregue-o ao
`Repository` no lugar da conexão:

```java
import io.github.lordcarvel.cdatra.d1.D1DatabaseAdapter;
import io.github.lordcarvel.cdatra.repository.Repository;

D1DatabaseAdapter adapter = new D1DatabaseAdapter(
        System.getenv("CDATRA_WORKER_URL"),
        System.getenv("CDATRA_SECRET")
);

Repository<User> repository = new Repository<>(User.class, adapter);

repository.createTable();
repository.save(new User("Carvel"));

List<User> users = repository.findAll();
```

O adapter escolhe o dialeto do banco, então o `createTable` já gera SQL do D1. O
restante da API é igual: as seções anteriores valem para os dois bancos.

## Erros

Todas as falhas do banco chegam como `CdatraException`
(`io.github.lordcarvel.cdatra.exception`), uma exceção não checada. No JDBC, o
erro original fica como causa. No D1, a mensagem traz o código do Worker e o texto
do banco, por exemplo `SQL_ERROR: D1_ERROR: no such table: users: SQLITE_ERROR`.

Uso incorreto da API, como coluna inexistente ou valor de tipo incompatível, gera
`IllegalArgumentException` antes de ir ao banco.

## Annotations

Todas ficam em `io.github.lordcarvel.cdatra.annotation`:

| Annotation | Uso |
| --- | --- |
| `@Entity(tableNaame = "users")` | Define o nome da tabela da classe. |
| `@Column(columName = "name")` | Inclui o campo no mapeamento e define o nome da coluna. |
| `@Id` | Marca a única chave primária da entidade; exige `@Column`. |
| `@GeneratedValue` | Pede geração do ID pelo banco; exige `@Id`, `@Column` e int/Integer ou long/Long. |

Campos herdados participam do mapeamento. Campos static e sintéticos são
ignorados. A classe concreta usada no Repository precisa de `@Entity`.

Entidades sem ID podem ser salvas e consultadas pelas buscas em lista; operações
por ID exigem `@Id`. A tabela precisa ter pelo menos uma coluna anotada.

## Tipos suportados

| Tipo Java | Tipo SQL no H2 | Tipo SQL no D1 |
| --- | --- | --- |
| `int`, `Integer` | `INTEGER` | `INTEGER` |
| `long`, `Long` | `BIGINT` | `INTEGER` |
| `boolean`, `Boolean` | `BOOLEAN` | `INTEGER` |
| `double`, `Double` | `DOUBLE` | `REAL` |
| `String` | `VARCHAR` | `TEXT` |

Campos objeto podem receber null; campos primitivos não podem receber NULL do
banco. NaN e infinidades são rejeitados. Não há suporte a datas, BigDecimal,
enums, coleções ou objetos relacionados como colunas.

Na leitura, o valor é convertido para o tipo do campo quando o banco devolve outro
tipo, como o D1, que devolve `boolean` como número. Um número que não cabe no
campo é recusado, e não cortado.

## Limitações

O escopo validado é Java 17 com H2 2.5.250 em memória e com o Cloudflare D1. Não
foram certificados PostgreSQL, MySQL, SQLite direto, acesso simultâneo por threads,
persistência em disco no H2 ou desempenho em produção.

A versão atual tem estes limites:

- Sem migrations, relacionamentos, JOINs, paginação ou API de ORDER BY.
- Uma única coluna de ID; sem chave primária composta.
- Nomes de tabela e coluna seguem `[A-Za-z_][A-Za-z0-9_]*`. Não há quoting de
  identificadores ou nomes qualificados por schema; evite palavras reservadas.
- Valores vão sempre como parâmetros (`?`), nunca no texto do SQL. Nomes de tabela
  e coluna não podem ser parâmetros; por isso são validados.
- Sem seleção parcial de campos pelo Repository.
- No JDBC, commit e rollback pertencem ao chamador. O Repository usa a configuração
  de transação da conexão recebida e não inicia nem confirma transações sozinho.

No Cloudflare D1:

- Não há transações. Cada comando é independente, e a lib não oferece comandos em
  lote.
- `long` só é aceito até ±(2⁵³ − 1). Valores maiores são recusados antes do envio,
  porque o Worker é JavaScript e perderia precisão.
- `LIKE` ignora maiúsculas e minúsculas só nas letras sem acento: `car%` acha
  `Carvel`, mas `JOÃO` não acha `João`. No H2, ele sempre diferencia.
- O padrão do `LIKE` aceita no máximo 50 bytes; acima disso, vem `SQL_ERROR`.
- Cada comando é uma chamada HTTP. Fora da Cloudflare, isso custa mais tempo do
  que um banco na mesma rede.
- Valem os [limites do D1](https://developers.cloudflare.com/d1/platform/limits/),
  como o tamanho máximo do banco e o de cada linha.

Os testes verificam CRUD, filtros, transações e visibilidade entre duas conexões,
mas não garantem ausência de todo bug nem compatibilidade com outros bancos.

## Status da versão

A versão do `pom.xml` é **0.0.2**.

Nesta versão, os **582 testes** do Java passaram no build com cobertura e na
repetição da auditoria: zero falhas, zero erros e nenhum teste ignorado. Para
reproduzir:

```shell
mvn -Pcoverage clean verify
mvn -Pdefect-audit test
```

Para a suíte padrão, use `mvn test`. Para um grupo específico, por exemplo:

```shell
mvn -Dtest=RepositoryIntegrationTest test
```

O perfil `defect-audit` executa a mesma suíte e separa os resultados em
`target/defect-audit-reports/`. A suíte padrão grava em `target/surefire-reports/`.
O perfil `coverage` gera o relatório em `target/site/jacoco/index.html`.

Os 37 testes do Worker usam o `node:test`, sem dependências, e não rodam pelo Maven.
Dentro de `worker/`:

```shell
npm test
```

Eles usam um banco falso. O caminho completo, do `Repository` ao D1, foi conferido
manualmente com `wrangler dev` e com um banco real; essa conferência não faz parte
das suítes.

Os documentos da v0.0.1 continuam como histórico. O
[relatório das correções](docs/testing/v0.0.1-fixes.md) e suas
[evidências](docs/testing/v0.0.1-fixed-results.json) registram a revisão anterior
à reorganização. O [relatório da auditoria inicial](docs/testing/v0.0.1-report.md)
e seu [inventário](docs/testing/v0.0.1-results.json) permanecem como histórico.
Esses arquivos são fotografias das respectivas revisões, sem atualização
automática pelo Maven.

O código de produção está em `io.github.lordcarvel.cdatra`, nos packages
`adapter`, `annotation`, `d1`, `exception`, `internal`, `jdbc`, `mapping`,
`metadata`, `query`, `repository` e `sql`. O package `internal` não é API pública.
Os exemplos internos Main, User e Address ficam em `src/test/java`, no package
`io.github.lordcarvel.cdatra.example`, e não fazem parte do JAR.

Antes de criar uma tag, confira o estado e os commits finais:

```shell
git status
git log --oneline -10
git diff
```

O esperado é uma árvore de trabalho limpa e nenhuma alteração esquecida no
diff. A biblioteca não é publicada no Maven Central.
