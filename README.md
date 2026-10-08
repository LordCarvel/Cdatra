# Cdatra

## O que é

Cdatra é uma biblioteca de aprendizado em Java para reduzir o trabalho repetitivo
com JDBC. A partir de uma classe anotada, cria tabelas, salva registros, faz
consultas, atualiza e exclui dados usando `Repository<T>`.

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
- Uma conexão JDBC e o driver do banco no classpath da aplicação.

O ambiente validado usa Java 17, Maven 3.9.14 e **H2 2.5.250 em memória**.
H2 é o banco atualmente testado para a v0.0.1. O projeto inclui esse driver como
dependência de runtime e usa JUnit 5.11.4 nos testes.

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
    <version>0.0.1</version>
</dependency>
```

Essas são as coordenadas Maven atuais. Os packages Java usam a raiz
`io.github.lordcarvel.cdatra`. A instalação descrita usa o build local; não
pressupõe publicação no Maven Central.

O build gera `target/Cdatra-0.0.1.jar`. Esse JAR não contém suas dependências;
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
grafia nos exemplos da v0.0.1.

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
Quem fornece a conexão é responsável por fechá-la. As operações podem lançar
exceções JDBC e de reflexão; o exemplo usa `throws Exception` para manter o foco
no fluxo básico.

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

Importe `io.github.lordcarvel.cdatra.query.Operator`. A v0.0.1 suporta:

| Operador | Comparação SQL |
| --- | --- |
| `Operator.EQUAL` | `=` |
| `Operator.NOT_EQUAL` | `!=` |
| `Operator.GREATER_THAN` | `>` |
| `Operator.LESS_THAN` | `<` |
| `Operator.GREATER_THAN_OR_EQUAL` | `>=` |
| `Operator.LESS_THAN_OR_EQUAL` | `<=` |

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
de AND e OR, vale a precedência nativa do SQL: AND é avaliado antes de OR. A API
não oferece agrupamento explícito por parênteses.

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

| Tipo Java | Tipo SQL |
| --- | --- |
| `int`, `Integer` | `INTEGER` |
| `long`, `Long` | `BIGINT` |
| `boolean`, `Boolean` | `BOOLEAN` |
| `double`, `Double` | `DOUBLE` |
| `String` | `VARCHAR` |

Campos objeto podem receber null; campos primitivos não podem receber NULL do
banco. NaN e infinidades são rejeitados. Não há conversão automática geral de
tipos, suporte a datas, BigDecimal, enums, coleções ou objetos relacionados como
colunas.

## Limitações da v0.0.1

O escopo validado é Java 17 com H2 2.5.250 em memória. Não foram certificados
PostgreSQL, MySQL, SQLite, acesso simultâneo por threads, persistência em disco,
falhas de rede ou desempenho em produção.

A versão atual tem estes limites:

- Sem migrations, relacionamentos, JOINs, paginação ou API de ORDER BY.
- Uma única coluna de ID; sem chave primária composta.
- Nomes de tabela e coluna seguem `[A-Za-z_][A-Za-z0-9_]*`. Não há quoting de
  identificadores ou nomes qualificados por schema; evite palavras reservadas.
- SQL gerado como texto, com validação de nomes e tipos e escape de apóstrofos.
  Não há uma API de parâmetros JDBC para os valores.
- Sem conversão geral de tipos ou seleção parcial de campos pelo Repository.
- Commit e rollback pertencem ao chamador. O Repository usa a configuração de
  transação da conexão recebida e não inicia nem confirma transações sozinho.

Os testes verificam CRUD, filtros, transações e visibilidade entre duas conexões,
mas não garantem ausência de todo bug nem compatibilidade com outros bancos.

## Status da versão

A versão do `pom.xml` é **0.0.1**. Esta preparação reúne versão e documentação,
sem adicionar funcionalidades à biblioteca.

Após a reorganização dos packages, os **224 casos distintos** passaram no build
com cobertura e na repetição da auditoria: zero falhas, zero erros e nenhum teste
ignorado. Para reproduzir:

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

O [relatório das correções](docs/testing/v0.0.1-fixes.md) e suas
[evidências](docs/testing/v0.0.1-fixed-results.json) registram a revisão anterior
à reorganização. O [relatório da auditoria inicial](docs/testing/v0.0.1-report.md)
e seu [inventário](docs/testing/v0.0.1-results.json) permanecem como histórico.
Esses arquivos são fotografias das respectivas revisões, sem atualização
automática pelo Maven.

O código de produção está em `io.github.lordcarvel.cdatra`, nos packages
`annotation`, `metadata`, `mapping`, `query`, `sql`, `jdbc` e `repository`.
Os exemplos internos Main, User e Address ficam em `src/test/java`, no package
`io.github.lordcarvel.cdatra.example`, e não fazem parte do JAR.

Antes de criar uma tag, confira o estado e os commits finais:

```shell
git status
git log --oneline -10
git diff
```

O esperado é uma árvore de trabalho limpa e nenhuma alteração esquecida no
diff. Este preparo não cria uma tag nem publica um pacote no Maven Central.
