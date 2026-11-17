# Sistema de Gerenciamento Hoteleiro

Backend de uma aplicação de **Sistema de Gerenciamento Hoteleiro**, desenvolvido com **Java e Spring Boot**.

O projeto fornece uma API REST que servirá como backend para a aplicação frontend correspondente.

## Tecnologias

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Validation
- Swagger / OpenAPI
- Maven

## Estrutura do Projeto

```text
src/main/java/com/hotelmanagement/
├── controller/    # Endpoints da API REST
├── dto/           # Objetos de entrada e saída da API
├── entity/        # Entidades JPA
├── repository/    # Acesso ao banco de dados
├── service/       # Regras de negócio
├── exception/     # Tratamento de exceções
└── config/        # Configurações da aplicação
```

A aplicação segue uma arquitetura em camadas:

```text
Requisição HTTP
      ↓
  Controller
      ↓
   Service
      ↓
 Repository
      ↓
Banco de Dados
```

DTOs são utilizados na camada da API para separar os dados recebidos e enviados das entidades JPA utilizadas internamente pela aplicação.

## Requisitos do Projeto

O backend deverá conter:

- Entidades modeladas com JPA
- Repositórios para acesso ao banco de dados
- Serviços contendo regras de negócio
- Controladores REST
- Operações de cadastro, consulta, atualização e exclusão (CRUD)
- Integração com banco de dados
- Utilização de DTOs
- Organização adequada dos pacotes
- Documentação da API com Swagger/OpenAPI

## Executando o Projeto

### Pré-requisitos

- Java 21
- Git

### Clonar o repositório

```bash
git clone <url-do-repositorio>
cd hotel-management
```

### Executar com Maven Wrapper

No macOS/Linux:

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, a aplicação será executada em:

```text
http://localhost:8080
```

## Documentação da API

O Swagger/OpenAPI será utilizado para documentar e testar os endpoints da API REST.

O endereço da interface do Swagger será adicionado após a configuração da documentação da API.

## Banco de Dados

O projeto utiliza PostgreSQL persistente remoto. Antes de executar, configure
as variáveis de ambiente `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
A URL segue o formato `jdbc:postgresql://<host>:5432/<banco>`.

O banco e o usuário devem existir previamente. O usuário precisa ter permissão
para criar e alterar tabelas no esquema utilizado.

Na inicialização, o Hibernate cria tabelas ausentes e aplica alterações de esquema
suportadas pelo modo `spring.jpa.hibernate.ddl-auto=update`. Esse mecanismo não
realiza migrações de dados nem renomeia colunas automaticamente.

Após a preparação das tabelas, a aplicação cadastra os tipos de quarto `Normal`
e `Suíte` quando não encontra seus nomes, sem diferenciar maiúsculas de minúsculas.
Reiniciar a aplicação preserva os registros existentes; um tipo inicial excluído
ou renomeado será cadastrado novamente no próximo início.

### Validação manual da inicialização

1. Configure a conexão e execute `./mvnw spring-boot:run`.
2. No cliente PostgreSQL, consulte `SELECT * FROM room_type;` e confirme os dois tipos iniciais.
3. Reinicie a aplicação e repita a consulta: os registros devem manter seus IDs,
   sem novas cópias dos tipos iniciais.

## Status do Projeto

🚧 **Em desenvolvimento**

Estão implementadas a modelagem de quartos e tipos, a inicialização dos tipos
e a API de CRUD de tipos de quarto. As demais funcionalidades serão
implementadas de forma incremental.

## API de tipos de quarto

| Método | Rota | Resultado de sucesso |
| --- | --- | --- |
| GET | `/api/room-types` | `200`: lista de tipos ordenada por ID |
| GET | `/api/room-types/{id}` | `200`: tipo encontrado |
| POST | `/api/room-types` | `201`: tipo criado e cabeçalho `Location` |
| PUT | `/api/room-types/{id}` | `200`: tipo atualizado |
| DELETE | `/api/room-types/{id}` | `204`: exclusão sem corpo de resposta |

POST e PUT recebem `name` e `description`. O nome é obrigatório, não pode
conter apenas espaços e aceita até 255 caracteres. Espaços nas extremidades
do nome são removidos ao salvar. A descrição é opcional e aceita até 255
caracteres. No PUT, omitir a descrição remove a descrição anterior.
As respostas de criação, consulta e atualização contêm `id`, `name` e `description`.

Uma requisição inválida retorna `400`; um ID inexistente retorna `404`.
Excluir um tipo associado a quartos retorna `409`, sem excluir os quartos.
Os erros usam o formato Problem Details; erros de validação incluem
o campo `errors`, com mensagens por campo. Falhas de integridade retornam
`409` sem expor detalhes internos do banco.

Nesta etapa os endpoints ainda não possuem autenticação. A restrição de
escrita ao perfil de recepcionista será implementada na etapa de segurança.
Swagger também permanece pendente.

### Validação manual do CRUD

Com a aplicação conectada ao PostgreSQL, consulte os tipos iniciais:

```bash
curl -i http://localhost:8080/api/room-types
```

Cadastre um tipo e anote o `id` retornado:

```bash
curl -i -X POST http://localhost:8080/api/room-types \
  -H 'Content-Type: application/json' \
  -d '{"name":"Premium","description":"Acomodação premium."}'
```

Nos comandos seguintes, substitua `<id>` pelo ID cadastrado:

```bash
curl -i 'http://localhost:8080/api/room-types/<id>'

curl -i -X PUT 'http://localhost:8080/api/room-types/<id>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Premium atualizado","description":"Descrição atualizada."}'

curl -i -X DELETE 'http://localhost:8080/api/room-types/<id>'

curl -i 'http://localhost:8080/api/room-types/<id>'
```

Confirme `200`, `200`, `204` e `404`, respectivamente. Verifique também:

- POST com `{"name":"   "}` deve retornar `400` com erro no campo `name`.
- POST com nome ausente ou nulo e campos com mais de 255 caracteres deve retornar `400`.
- POST com `{"name":"Executivo"}` deve retornar `201`, com descrição nula.
- PUT e DELETE com ID inexistente devem retornar `404`.
- JSON malformado ou ID não numérico na URL deve retornar `400`.
- Quando houver quartos cadastrados, excluir seu tipo deve retornar `409`
  e preservar o tipo e seus quartos.
- Reiniciar a aplicação deve preservar os tipos cadastrados e suas alterações,
  observando a reposição dos tipos iniciais descrita na seção do banco.
