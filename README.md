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

O projeto utiliza `springdoc-openapi-starter-webmvc-ui` 3.2.0 para gerar a
documentação básica a partir dos controllers e DTOs.

Com a aplicação iniciada e conectada ao PostgreSQL, acesse:

- Swagger UI: `http://localhost:8080/swagger-ui.html`.
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.

As rotas da documentação são públicas. As regras de autenticação e CSRF dos
endpoints continuam valendo. Descrições detalhadas, documentação dos filtros
de login/logout e integração da interface com CSRF serão complementadas depois.

## Banco de Dados

O projeto utiliza PostgreSQL persistente remoto. Configure `DB_URL` no arquivo
`.env` na raiz do projeto, usando `.env.example` como referência. Execute a
aplicação a partir dessa raiz para carregar o arquivo automaticamente.
A connection string deve usar o formato JDBC, incluindo as credenciais:
`jdbc:postgresql://<host>/<banco>?user=<usuario>&password=<senha>&sslmode=require`.
Caracteres especiais no usuário e na senha devem ser codificados para URL.
`DB_USERNAME` e `DB_PASSWORD` não são mais necessários.

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
e as APIs de CRUD de quartos e tipos de quarto. A modelagem de usuários e
perfis de cliente, o cadastro público de clientes, o login por sessão e o controle
de acesso a quartos e tipos também estão implementados. O fluxo de reservas inclui
solicitação, consulta, aprovação, cancelamento e consulta de disponibilidade.
As demais funcionalidades serão
implementadas de forma incremental.

## Modelagem de usuários e clientes

`User` representa a identidade de acesso, com `id`, `name`, `email`,
`passwordHash`, `role` e `active`. A tabela se chama `users`, evitando usar
`user`, que possui significado especial no PostgreSQL. O e-mail é obrigatório
e possui restrição de unicidade. `UserRole` contém `CLIENT` e `RECEPTIONIST`,
persistidos como texto com `EnumType.STRING`.

`CustomerProfile` contém `id`, `user`, `phone` e `document`. O relacionamento
com `User` é 1:1, por meio de `customer_profile.user_id`, uma chave estrangeira
obrigatória e única. Um perfil sempre referencia um usuário; um usuário pode
existir sem perfil, permitindo representar recepcionistas. O vínculo é
unidirecional, com carregamento `LAZY` e sem exclusão em cascata.

Telefone e documento são opcionais nesta modelagem. O cadastro público cria
um usuário ativo com perfil `CLIENT` e seu `CustomerProfile` na mesma transação.
O e-mail é normalizado e a senha é transformada em hash pelo serviço de cadastro.
Nenhuma conta inicial é criada. O cadastro de recepcionistas permanece pendente.

Quando o PostgreSQL estiver disponível, iniciar a aplicação com `ddl-auto=update`
deverá criar as tabelas `users` e `customer_profile`. A validação manual do
esquema deverá confirmar a unicidade de `users.email` e de
`customer_profile.user_id`, a obrigatoriedade do vínculo e a chave estrangeira
para `users.id`. A validação manual do fluxo de cadastro está descrita abaixo.

## Cadastro de clientes

`POST /api/auth/register` cria uma conta de cliente e retorna `201`.

| Campo | Regra |
| --- | --- |
| `name` | Obrigatório, não vazio, até 255 caracteres |
| `email` | Obrigatório, formato de e-mail, até 255 caracteres após remover espaços das extremidades |
| `password` | Obrigatória, não composta apenas por espaços, de 8 a 128 caracteres |
| `phone` | Opcional, até 255 caracteres |
| `document` | Opcional, até 255 caracteres |

O nome é salvo sem espaços nas extremidades. O e-mail é salvo em minúsculas e
sem espaços nas extremidades; a verificação de duplicidade ignora maiúsculas.
A senha é preservada exatamente como recebida antes do cálculo do hash.
O serviço atribui `role: CLIENT` e `active: true`; esses valores não podem
ser escolhidos pela requisição.

A resposta contém `id`, `name`, `email`, `role`, `active` e `customerProfile`
com `id`, `phone` e `document`. Senha e hash não fazem parte do DTO de resposta.
Dados inválidos retornam `400`; e-mail já cadastrado retorna `409`. A restrição
única do banco também impede cadastros simultâneos com o mesmo e-mail normalizado.
Uma falha ao salvar o perfil desfaz a criação do usuário pela transação.

`PasswordConfig` fornece um `PasswordEncoder` PBKDF2-HMAC-SHA256 com salt
aleatório de 16 bytes e 600.000 iterações. O Spring Security utiliza o mesmo
encoder para verificar a senha no login; alterar os parâmetros exige considerar
os hashes já persistidos. Cadastro não inicia sessão nem emite token.

### Validação manual do cadastro

Com PostgreSQL configurado e a aplicação iniciada, obtenha o cookie e o token
CSRF conforme a seção de autenticação abaixo. Execute com um e-mail novo:

```bash
curl -i -X POST -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana Silva","email":" Ana@Example.com ","password":"SenhaExemplo123!","phone":"81999999999","document":"Documento de exemplo"}'
```

Confirme `201`, `email: ana@example.com`, `role: CLIENT`, `active: true`
e um perfil de cliente com ID. A resposta não deve conter `password` nem
`passwordHash`. No cliente PostgreSQL, confirme o vínculo:

```sql
SELECT u.id, u.email, u.role, u.active, c.id AS profile_id, c.user_id,
       u.password_hash <> 'SenhaExemplo123!' AS password_is_not_plaintext
FROM users u
JOIN customer_profile c ON c.user_id = u.id
WHERE u.email = 'ana@example.com';
```

Verifique também:

- Repetir o cadastro com `ANA@EXAMPLE.COM` deve retornar `409`, sem novos registros.
- Campos obrigatórios ausentes ou vazios, e-mail inválido, senha com menos de
  8 ou mais de 128 caracteres e textos acima dos limites devem retornar `400`.
- Omitir telefone e documento deve permitir o cadastro de outro e-mail,
  mantendo esses campos nulos no perfil.
- Acrescentar `role: RECEPTIONIST` ou `active: false` à requisição não deve
  alterar os valores atribuídos pelo serviço: `CLIENT` e `true`.
- Reiniciar a aplicação deve preservar o usuário, o perfil e o hash.

O cadastro de recepcionistas permanece pendente.

## Autenticação e controle de acesso

A autenticação utiliza sessão HTTP mantida no servidor e o cookie `JSESSIONID`.
Não há emissão de JWT. O cookie é HttpOnly, com SameSite=Lax, e a sessão expira
após 30 minutos de inatividade. As sessões ficam em memória; reiniciar a aplicação
exige novo login. Em um ambiente HTTPS, configure `SESSION_COOKIE_SECURE=true`.

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | `200`: `headerName` e `token` para proteção CSRF |
| POST | `/api/auth/register` | `201`: cadastro público de cliente com corpo JSON |
| POST | `/api/auth/login` | `204`: login com formulário `email` e `password` |
| GET | `/api/auth/me` | `200`: dados do usuário autenticado, sem senha ou hash |
| POST | `/api/auth/logout` | `204`: invalida a sessão e remove o cookie |

O login recebe `application/x-www-form-urlencoded`, processado pelo filtro
do Spring Security. Cadastro e CRUD continuam recebendo JSON. E-mails no login
são comparados sem distinguir maiúsculas e sem espaços nas extremidades.
Senha incorreta, conta inexistente ou inativa retornam a mesma resposta `401`.
O login altera o identificador da sessão para proteger contra fixação de sessão.

Consultas GET de quartos e tipos são públicas. POST, PUT e DELETE dessas
entidades exigem `RECEPTIONIST`. Com token CSRF válido, um visitante recebe
`401` ao tentar escrever e um cliente autenticado recebe `403`. Falta ou erro
no token CSRF retorna `403`, inclusive em cadastro e login. Os erros de segurança
usam Problem Details e não redirecionam para páginas HTML de login.

As permissões e o estado ativo são carregados no login. Alterações posteriores
no banco exigem encerrar a sessão e autenticar novamente para atualizar as
autoridades; revogação imediata de sessões ainda não está implementada.

### Cookies, CSRF e Frontend

Antes de enviar POST, PUT ou DELETE, consulte `/api/auth/csrf`, guarde o
cookie e envie o token no cabeçalho indicado por `headerName` (`X-CSRF-TOKEN`).
Após login ou logout, obtenha um novo token, pois o anterior é invalidado.

No navegador, use `credentials: 'include'` nas requisições. CORS permite por
padrão `http://localhost:5173` e `http://localhost:3000`. Configure
`CORS_ALLOWED_ORIGINS` com origens explícitas separadas por vírgula para mudar
esses endereços. A configuração SameSite=Lax atende ao desenvolvimento em
localhost com portas distintas; publicação em sites distintos exigirá rever
a política de cookie e utilizar HTTPS.

### Validação manual de autenticação

Obtenha o cookie e o token:

```bash
curl -i -c /tmp/hotel-cookies.txt http://localhost:8080/api/auth/csrf
```

Copie o valor `token` retornado para a variável abaixo:

```bash
CSRF_TOKEN='cole-o-token-aqui'
```

Use esse cookie e token no cadastro apresentado anteriormente. Depois faça
login com a conta cadastrada:

```bash
curl -i -b /tmp/hotel-cookies.txt -c /tmp/hotel-cookies.txt \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  --data-urlencode 'email=ana@example.com' \
  --data-urlencode 'password=SenhaExemplo123!' \
  http://localhost:8080/api/auth/login

curl -i -b /tmp/hotel-cookies.txt http://localhost:8080/api/auth/me

curl -i -b /tmp/hotel-cookies.txt -c /tmp/hotel-cookies.txt \
  http://localhost:8080/api/auth/csrf
```

Confirme `204` no login e `200` em `/me`, com o usuário esperado e sem senha
ou hash. Atualize `CSRF_TOKEN` com o token da última resposta. Os exemplos
de escrita dos CRUDs abaixo usam esse cookie e essa variável; para sucesso
no CRUD, a sessão deverá pertencer a um recepcionista.

Não há provisionamento automático de recepcionistas. Para validar esse perfil
em desenvolvimento, um responsável pelo banco pode cadastrar uma conta de teste
pela API e, com a sessão encerrada, ajustar exclusivamente essa conta:

```sql
BEGIN;
DELETE FROM customer_profile
WHERE user_id IN (SELECT id FROM users WHERE email = 'recepcao@example.com');
UPDATE users SET role = 'RECEPTIONIST'
WHERE email = 'recepcao@example.com';
COMMIT;
```

Faça novo login com essa conta para executar os CRUDs. Esse procedimento manual
serve à validação; o mecanismo definitivo de cadastro de recepcionistas permanece
pendente. O cadastro público sempre atribui `CLIENT`.

Valide também `/me` sem cookie (`401`), login com senha errada ou conta inativa
(`401`), escrita com cliente e CSRF válido (`403`), escrita sem CSRF (`403`) e
consultas públicas sem cookie (`200`). Para sair:

```bash
curl -i -X POST -b /tmp/hotel-cookies.txt -c /tmp/hotel-cookies.txt \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" http://localhost:8080/api/auth/logout
curl -i -b /tmp/hotel-cookies.txt http://localhost:8080/api/auth/me
```

Confirme `204` no logout e `401` em `/me`. Para reiniciar o fluxo, obtenha
novamente cookie e token CSRF.

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

Escrita exige uma sessão de recepcionista e token CSRF, conforme a seção de
autenticação. Consultas são públicas. A documentação básica está no Swagger UI.

### Validação manual do CRUD

Com a aplicação conectada ao PostgreSQL, consulte os tipos iniciais:

```bash
curl -i http://localhost:8080/api/room-types
```

Cadastre um tipo e anote o `id` retornado:

```bash
curl -i -X POST -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" http://localhost:8080/api/room-types \
  -H 'Content-Type: application/json' \
  -d '{"name":"Premium","description":"Acomodação premium."}'
```

Nos comandos seguintes, substitua `<id>` pelo ID cadastrado:

```bash
curl -i 'http://localhost:8080/api/room-types/<id>'

curl -i -X PUT -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" 'http://localhost:8080/api/room-types/<id>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Premium atualizado","description":"Descrição atualizada."}'

curl -i -X DELETE -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" 'http://localhost:8080/api/room-types/<id>'

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

## API de quartos

| Método | Rota | Resultado de sucesso |
| --- | --- | --- |
| GET | `/api/rooms` | `200`: lista de quartos, incluindo inativos, ordenada por número |
| GET | `/api/rooms/{id}` | `200`: quarto encontrado |
| POST | `/api/rooms` | `201`: quarto criado e cabeçalho `Location` |
| PUT | `/api/rooms/{id}` | `200`: quarto atualizado |
| DELETE | `/api/rooms/{id}` | `204`: exclusão sem corpo de resposta |

POST e PUT recebem os seguintes campos:

| Campo | Regra |
| --- | --- |
| `name` | Opcional, até 255 caracteres |
| `number` | Inteiro positivo obrigatório e único |
| `typeId` | ID positivo obrigatório de um tipo existente |
| `description` | Opcional, até 255 caracteres |
| `capacity` | Inteiro positivo obrigatório |
| `pricePerNight` | Obrigatório, de zero a 99999999.99, até duas casas decimais |
| `active` | Booleano obrigatório: `true` ou `false` |

PUT substitui os dados editáveis: os campos obrigatórios devem ser enviados
novamente; omitir `name` ou `description` remove seu valor anterior. `id` e
`createdAt` são gerados pelo servidor e preservados na atualização.

A resposta contém `id`, `name`, `number`, `type`, `description`, `capacity`,
`pricePerNight`, `active` e `createdAt`. `type` contém `id`, `name` e
`description` do tipo associado. As entidades JPA não são expostas diretamente.

Dados inválidos retornam `400`; quarto ou tipo inexistente retorna `404`;
número de quarto já utilizado retorna `409`. Na atualização, manter o número
do próprio quarto é permitido. A restrição única no banco também protege
contra gravações simultâneas com o mesmo número.

Desativar um quarto é uma atualização com `active: false`; DELETE exclui
fisicamente o registro somente quando não existem reservas vinculadas, inclusive
canceladas. A redução de capacidade é recusada quando não comporta os hóspedes
de reservas pendentes ou aprovadas. Escrita exige sessão de recepcionista
e token CSRF; consultas são públicas.

### Validação manual dos quartos

1. Execute a aplicação com PostgreSQL e consulte `GET /api/room-types`.
2. Use um ID existente no campo `typeId` dos exemplos abaixo, substituindo `1`.
3. Cadastre um quarto com um número ainda livre e anote seu ID:

```bash
curl -i -X POST -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" http://localhost:8080/api/rooms \
  -H 'Content-Type: application/json' \
  -d '{"name":"Quarto 101","number":101,"typeId":1,"description":"Vista para o jardim.","capacity":2,"pricePerNight":180.50,"active":true}'

curl -i http://localhost:8080/api/rooms
```

Confirme `201` na criação, `Location` apontando para o quarto criado e `200`
na listagem. Substitua `<id>` pelo ID retornado:

```bash
curl -i 'http://localhost:8080/api/rooms/<id>'

curl -i -X PUT -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" 'http://localhost:8080/api/rooms/<id>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Quarto 101 atualizado","number":101,"typeId":1,"description":"Descrição atualizada.","capacity":3,"pricePerNight":200.00,"active":false}'
```

Confirme `200` nos dois casos, preservação de `id` e `createdAt` e atualização
dos demais campos. Antes de excluir, valide também:

- Repetir o POST com número `101` deve retornar `409`.
- Cadastrar outro quarto e atualizar seu número para `101` deve retornar `409`.
- Usar um `typeId` positivo inexistente deve retornar `404`.
- Número ou capacidade zero ou negativos, diária negativa, com mais de duas
  casas decimais ou acima do limite devem retornar `400`.
- Campos obrigatórios ausentes ou nulos e textos acima de 255 caracteres
  devem retornar `400`. `active: false` e diária zero são válidos.
- Excluir o tipo vinculado ao quarto deve retornar `409`, mesmo com o quarto inativo.
- Reiniciar a aplicação deve preservar o quarto e suas alterações.

Finalize excluindo o quarto:

```bash
curl -i -X DELETE -b /tmp/hotel-cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN" 'http://localhost:8080/api/rooms/<id>'
curl -i 'http://localhost:8080/api/rooms/<id>'
```

Confirme `204` na exclusão e `404` na consulta. PUT e DELETE com ID
inexistente também devem retornar `404`; ID não numérico e JSON malformado
devem retornar `400`.

## Reservas e disponibilidade

| Método | Rota | Permissão e resultado |
| --- | --- | --- |
| GET | `/api/rooms/{id}/availability?checkInDate=AAAA-MM-DD&checkOutDate=AAAA-MM-DD` | Público; `200` com `available` |
| POST | `/api/reservations` | Cliente; `201` com reserva `PENDING` e `Location` |
| GET | `/api/reservations` | Cliente vê suas reservas; recepcionista vê todas; `200` |
| GET | `/api/reservations/{id}` | Proprietário ou recepcionista; `200` |
| PUT | `/api/reservations/{id}/approve` | Recepcionista; `200` com status `APPROVED` |
| PUT | `/api/reservations/{id}/cancel` | Cliente proprietário; `200` com status `CANCELLED` |

Os endpoints de escrita exigem sessão e token CSRF. Aprovar e cancelar não
recebem corpo. A criação recebe:

```json
{
  "roomId": 1,
  "checkInDate": "2027-01-10",
  "checkOutDate": "2027-01-12",
  "guestCount": 2,
  "additionalServiceIds": []
}
```

Use um quarto real e datas futuras ao validar. `roomId`, datas e `guestCount`
são obrigatórios. IDs e quantidade de hóspedes devem ser positivos.
`additionalServiceIds` é opcional; ausente, nulo ou vazio significa sem serviços.
O cliente é obtido da sessão, e o status é definido pelo servidor. A resposta
contém `id`, `customerProfileId`, `roomId`, `checkInDate`, `checkOutDate`,
`guestCount`, `status`, `createdAt` e `additionalServiceIds`.

### Regras implementadas

- Entrada hoje ou em data futura; saída estritamente depois da entrada.
- Quantidade de hóspedes limitada à capacidade e quarto ativo.
- Reservas `PENDING` e `APPROVED` bloqueiam disponibilidade; `CANCELLED` não bloqueia.
- O período inclui a entrada e exclui a saída. Uma nova entrada no mesmo dia
  da saída anterior é permitida.
- Apenas `PENDING` pode ser aprovada. A aprovação revalida quarto, capacidade,
  data de entrada e sobreposição, desconsiderando a própria reserva.
- O cliente pode cancelar suas reservas pendentes ou aprovadas até o dia
  anterior à entrada, inclusive. No dia da entrada ou depois, recebe `409`.
- Aprovar novamente, cancelar novamente ou aprovar uma reserva cancelada retorna `409`.
- O calendário utiliza `HOTEL_TIME_ZONE`, com padrão `America/Recife`.
- Acesso a reserva de outro cliente retorna `404`, sem expor seus dados.
- Quartos com qualquer reserva, inclusive cancelada, não podem ser excluídos.
  A capacidade não pode ser reduzida abaixo do necessário às reservas bloqueantes.

A consulta pública de disponibilidade é informativa. A criação e a aprovação
utilizam transações e bloqueio pessimista do quarto (`PESSIMISTIC_WRITE`),
serializando operações concorrentes daquele quarto antes de verificar períodos.
A aprovação e o cancelamento também bloqueiam a reserva para proteger as
transições de status. Alterar ou excluir quartos usa o mesmo bloqueio de quarto.

Datas inválidas e capacidade excedida retornam `400`; entidades inexistentes
retornam `404`; indisponibilidade, quarto inativo, serviço inativo e transições
inválidas retornam `409`. Perfil sem permissão retorna `403` e ausência de
autenticação retorna `401`, observada a exigência de CSRF nas escritas.

`Reservation` referencia `CustomerProfile` e `Room` por `@ManyToOne`.
`AdditionalService` representa os serviços opcionais e se associa a reservas
por `@ManyToMany`, com a tabela `reservation_additional_service`. O Hibernate
prepara as tabelas ao iniciar. IDs de serviços inexistentes ou inativos são
recusados na criação. O CRUD de serviços ainda será implementado; até lá,
reservas podem ser feitas sem serviços ou com serviços cadastrados no banco.
Não há cobrança ou cálculo de valores de serviços nesta etapa.

Exclusão física, rejeição, alteração de datas e troca de serviços de uma reserva
não possuem endpoints nesta etapa; dependem das decisões de escopo sobre
histórico e alterações. Não existe remoção em cascata de clientes, quartos ou
serviços ao cancelar uma reserva.

### Validação manual de reservas

Prepare uma sessão de cliente e outra de recepcionista seguindo a seção de
autenticação; use arquivos de cookies separados e um token CSRF para cada sessão.
Com um quarto ativo e datas futuras:

1. Consulte a disponibilidade: deve retornar `available: true`.
2. Como cliente, envie POST com o JSON acima e anote o ID: deve retornar `201`
   e status `PENDING`. O cliente da resposta deve corresponder à sessão.
3. Consulte novamente a disponibilidade: `available: false`. Solicitar uma
   reserva sobreposta deve retornar `409`, mesmo antes da aprovação.
4. Como recepcionista, aprove a reserva: `200` e `APPROVED`. Repetir deve dar `409`.
5. Como outro cliente, confirme que a listagem não inclui a reserva e que
   consultar ou cancelar seu ID retorna `404`. Aprovação por cliente dá `403`.
6. Como proprietário, cancele até o dia anterior à entrada: `200`, `CANCELLED`.
   Confirme a disponibilidade liberada e solicite outra reserva no mesmo período.
7. Faça uma reserva com entrada amanhã: deve permitir cancelamento hoje.
   Faça outra com entrada hoje: o cancelamento deve retornar `409`.
8. Verifique saída igual ou anterior à entrada, entrada passada, capacidade
   excedida, quantidade zero, datas ausentes e IDs inválidos (`400`).
9. Verifique quarto inativo (`409`), IDs inexistentes (`404`), serviços inativos
   (`409`) e serviços inexistentes (`404`).
10. Verifique uma entrada no mesmo dia da saída de outra reserva: deve ser aceita.
11. Dispare duas solicitações para um quarto livre e o mesmo período em duas
    janelas: somente uma deve retornar `201`; a outra deve retornar `409`.
12. Confirme que excluir um quarto com reserva dá `409`, mesmo após cancelamento,
    e que reduzir sua capacidade abaixo de uma reserva bloqueante dá `409`.
13. Reinicie a aplicação, autentique novamente e confira a preservação das
    reservas, dos status e das associações de serviços.

O fluxo é validado manualmente contra PostgreSQL, sem infraestrutura de testes
automatizados ou banco em memória.
