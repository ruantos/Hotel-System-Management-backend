**Aluno: Robson Juan Oliveira (RA: 00000859894)**

# Sistema de Gerenciamento Hoteleiro

Projeto acadêmico de uma API REST para gerenciar quartos, tipos de acomodação,
características, usuários e reservas de um hotel. Clientes podem consultar a
disponibilidade, solicitar reservas, alterar datas e cancelar reservas.
Recepcionistas administram os cadastros e aprovam as reservas.

Desenvolvido com Java 21, Spring Boot, Spring Data JPA e PostgreSQL, com
autenticação por sessão e controle de acesso por perfil.

## Swagger

Após iniciar a aplicação, acesse:

- [Swagger UI](http://localhost:8080/swagger-ui.html): documentação e testes das rotas.
- [OpenAPI JSON](http://localhost:8080/v3/api-docs): especificação da API.

Para testar rotas protegidas, cadastre um cliente em `POST /api/auth/register`
ou use o recepcionista inicial. Faça login em `POST /api/auth/login` com `email`
e `password`. O Swagger gerencia o cookie de sessão e o token CSRF automaticamente.

## Instalação e configuração

Pré-requisitos: JDK 21 e um banco PostgreSQL disponível. O Maven é executado
pelo wrapper incluído no projeto.

1. Na raiz do projeto, copie o arquivo de variáveis de ambiente:

   ```bash
   cp .env.example .env
   ```

2. Edite o `.env` com os dados de conexão do banco:

   ```dotenv
   DB_URL=jdbc:postgresql://HOST:5432/BANCO?user=USUARIO&password=SENHA&sslmode=require
   ```

   Ajuste `sslmode` conforme o servidor PostgreSQL. Use valores sem aspas e
   codifique caracteres especiais das credenciais para URL. O banco deve existir
   e o usuário precisa ter permissão para criar e alterar tabelas.

3. Para criar o primeiro recepcionista, configure também no `.env`:

   ```dotenv
   BOOTSTRAP_RECEPTIONIST_ENABLED=true
   BOOTSTRAP_RECEPTIONIST_NAME=Recepção
   BOOTSTRAP_RECEPTIONIST_EMAIL=recepcao@example.com
   BOOTSTRAP_RECEPTIONIST_PASSWORD=DEFINA_UMA_SENHA_FORTE
   ```

   Use uma senha de 8 a 128 caracteres. A conta é criada na inicialização
   somente se não houver recepcionista cadastrado. Após criá-la, defina
   `BOOTSTRAP_RECEPTIONIST_ENABLED=false` e remova a senha do `.env`.

4. Inicie a aplicação a partir da raiz:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows, use `mvnw.cmd spring-boot:run`. A aplicação fica disponível em
   `http://localhost:8080`, e as tabelas são criadas ou atualizadas automaticamente.

O `.env` é carregado automaticamente e não é versionado. Para mudar a porta,
defina `PORT` (padrão: `8080`).

### Recuperação de senha (opcional)

No `.env`, defina `PASSWORD_RESET_ENABLED=true`, configure
`PASSWORD_RESET_FRONTEND_URL` com a página de redefinição de senha,
`PASSWORD_RESET_FROM` com o remetente e as variáveis `SMTP_*` do provedor de
e-mail, conforme o [.env.example](.env.example).
