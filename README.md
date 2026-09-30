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

O projeto contará com integração com banco de dados.

As configurações e instruções para conexão serão adicionadas após a configuração do ambiente de banco de dados.

## Status do Projeto

🚧 **Em desenvolvimento**

O projeto encontra-se atualmente em sua fase inicial de configuração. As entidades, regras de negócio, endpoints REST e integração com banco de dados serão implementados de forma incremental.