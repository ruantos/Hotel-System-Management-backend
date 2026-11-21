# Sistema de Gerenciamento Hoteleiro

Backend acadêmico em Java 21, Spring Boot 4.1, Spring Data JPA e PostgreSQL.
Oferece gerenciamento de quartos, características, contas e reservas, com
sessão HTTP, controle de acesso e documentação OpenAPI.

## Contexto para implementar o front-end

A API possui 36 operações, incluindo solicitação e redefinição de senha.
Os contratos, DTOs, permissões e erros estão no Swagger e em `/v3/api-docs`.
A integração utiliza sessão HTTP e CSRF; o navegador pode consumir a API por
um proxy no Next.js para preservar os cookies no deploy da Vercel.

A seção [Recuperação de senha](#recuperação-de-senha) descreve a habilitação,
os payloads e os cuidados com envio de e-mail. As credenciais de demonstração
permanecem no `.env.local` privado e não são versionadas.

## Executar

1. Instale Java 21.
2. Copie `.env.example` para `.env` na raiz e configure a conexão JDBC:

```dotenv
DB_URL=jdbc:postgresql://HOST/BANCO?user=USUARIO&password=SENHA&sslmode=require
```

Use valores sem aspas. Caracteres especiais nas credenciais devem ser
codificados para URL. O arquivo `.env` é ignorado pelo Git e carregado quando
a aplicação é executada a partir da raiz.

3. Crie o banco remoto e conceda ao usuário permissão para criar e alterar tabelas.
4. Execute:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

No Windows, utilize `mvnw.cmd`. A aplicação fica em `http://localhost:8080`.
`ddl-auto=update` cria tabelas ausentes e aplica alterações suportadas pelo
Hibernate. Os tipos `Normal` e `Suíte` são inseridos quando não existem.
Os dados permanecem no PostgreSQL após reiniciar; as sessões exigem novo login.

## Docker e deploy no Render

O `Dockerfile` compila com o Maven Wrapper e Java 21 em uma etapa e copia somente
o JAR executável para a imagem final com Java 21 JRE. O processo executa com
usuário sem privilégios de root. O `.dockerignore` exclui credenciais, metadados
do Git, caches e arquivos locais do contexto de construção.

Para executar localmente com Docker instalado:

```bash
docker build -t hotel-management .
docker run --rm -p 8080:8080 --env-file .env hotel-management
```

O `.env` é fornecido ao container em execução e não fica dentro da imagem.
A porta segue `PORT`, com padrão `8080`; `SERVER_PORT` também pode ser usado
como configuração direta do Spring Boot.

No Render, crie um Web Service, conecte este repositório, escolha Docker e
selecione o plano Free. Mantenha o caminho do Dockerfile como `./Dockerfile`
e o comando Docker padrão. Não é necessário instalar Docker localmente para
o Render construir a imagem.

Configure em Environment:

| Variável | Valor |
| --- | --- |
| `DB_URL` | Sua connection string JDBC do Neon, com SSL |
| `SESSION_COOKIE_SECURE` | `true` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` |
| `BOOTSTRAP_RECEPTIONIST_ENABLED` | `false`, quando a conta já existe no mesmo banco |

O Render fornece `PORT` automaticamente. Para a checagem de saúde, use
`/api/room-types`, que é uma consulta pública. Após o deploy, acesse
`https://SEU-SERVICO.onrender.com/swagger-ui.html`.

No plano gratuito, a aplicação pode ser suspensa por inatividade. O próximo
acesso pode levar mais tempo e exigir novo login, pois sessões ficam em memória.
Os dados permanecem no Neon.

## Primeiro recepcionista

Para o primeiro acesso administrativo, configure no `.env`:

```dotenv
BOOTSTRAP_RECEPTIONIST_ENABLED=true
BOOTSTRAP_RECEPTIONIST_NAME=Recepção
BOOTSTRAP_RECEPTIONIST_EMAIL=recepcao@example.com
BOOTSTRAP_RECEPTIONIST_PASSWORD=ESCOLHA_UMA_SENHA_FORTE
```

A senha deve ter entre 8 e 128 caracteres. A inicialização cria uma conta ativa
somente quando não há nenhum recepcionista. Não promove contas de cliente já
existentes, não sobrescreve senhas e não duplica a conta nos reinícios.
Depois do primeiro cadastro, desative o bootstrap e remova a senha do `.env`.

Os próximos recepcionistas são cadastrados por um recepcionista autenticado
em `POST /api/users/receptionists`, com `name`, `email` e `password`.
A rota pública de cadastro cria exclusivamente clientes.

## Swagger e autenticação

Abra [Swagger UI](http://localhost:8080/swagger-ui.html).
A especificação está em [OpenAPI JSON](http://localhost:8080/v3/api-docs).

A documentação inclui todas as rotas, formulários, DTOs, validações, permissões
por perfil, códigos de resposta e erros Problem Details.

1. Cadastre um cliente em `POST /api/auth/register`, ou utilize a conta inicial
   de recepção configurada localmente.
2. Execute `POST /api/auth/login` no Swagger, preenchendo o formulário com
   `email` e `password`. Uma resposta `204` confirma o login.
3. Consulte `GET /api/auth/me` e execute as operações permitidas ao seu perfil.
4. Execute `POST /api/auth/logout` para sair.

O navegador gerencia o cookie de sessão. A página do Swagger obtém um token CSRF
antes de cada escrita e o inclui automaticamente na requisição. Não é necessário
preencher o botão Authorize manualmente. A página utiliza os assets locais do
Swagger UI e não depende de CDN.

### Outros clientes HTTP

Consulte `GET /api/auth/csrf`, preserve os cookies e envie o valor `token` no
cabeçalho informado por `headerName` (`X-CSRF-TOKEN`) em POST, PUT e DELETE.
Isso também se aplica a cadastro, login e logout. Obtenha um novo token após
login, logout ou encerramento da sessão.

```bash
curl -i -c /tmp/hotel-cookies.txt http://localhost:8080/api/auth/csrf
```

Copie o token recebido para `CSRF_TOKEN` e faça login:

```bash
CSRF_TOKEN='TOKEN_RETORNADO'
curl -i -b /tmp/hotel-cookies.txt -c /tmp/hotel-cookies.txt \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  --data-urlencode 'email=recepcao@example.com' \
  --data-urlencode 'password=SUA_SENHA' \
  http://localhost:8080/api/auth/login
```

A sessão usa cookie HttpOnly, SameSite=Lax e expiração após 30 minutos de
inatividade. Em HTTPS, configure `SESSION_COOKIE_SECURE=true`. A proteção CSRF
continua ativa. Atualizar conta ou senha revoga as sessões anteriores; exclusão,
inativação ou mudança de perfil também impedem reutilizar uma sessão antiga.
Login incorreto, conta inexistente ou inativa retornam a mesma resposta `401`.
Senhas são armazenadas como PBKDF2-HMAC-SHA256, salt de 16 bytes e 600.000
iterações; senha e hash não aparecem nas respostas da API.

## Rotas

| Método | Rota | Acesso e resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Público; token CSRF, `200` |
| POST | `/api/auth/register` | Público; cadastro de cliente, `201` |
| POST | `/api/auth/forgot-password` | Público com CSRF; solicita link por e-mail, `202` |
| POST | `/api/auth/reset-password` | Público com CSRF; redefine por token, `204` |
| POST | `/api/auth/login` | Público; formulário de login, `204` |
| POST | `/api/auth/logout` | Encerra sessão, `204` |
| GET | `/api/auth/me` | Usuário autenticado, `200` |
| GET | `/api/users/me` | Própria conta, `200` |
| PUT | `/api/users/me` | Atualiza própria conta e encerra sessões, `200` |
| PUT | `/api/users/me/password` | Altera senha e encerra sessões, `204` |
| GET | `/api/users` | Recepcionista; lista usuários, `200` |
| GET | `/api/users/{id}` | Recepcionista; consulta usuário, `200` |
| DELETE | `/api/users/{id}` | Recepcionista; exclui usuário sem reservas, `204` |
| POST | `/api/users/receptionists` | Recepcionista; cadastra colega, `201` |
| GET/POST | `/api/room-types` | Consulta pública; cadastro por recepcionista |
| GET/PUT/DELETE | `/api/room-types/{id}` | Consulta pública; escrita por recepcionista |
| GET/POST | `/api/amenities` | Características: consulta pública; cadastro por recepcionista |
| GET/PUT/DELETE | `/api/amenities/{id}` | Consulta pública; escrita por recepcionista |
| GET/POST | `/api/rooms` | Consulta pública; cadastro por recepcionista |
| GET/PUT/DELETE | `/api/rooms/{id}` | Consulta pública; escrita por recepcionista |
| GET | `/api/rooms/{id}/availability` | Disponibilidade pública, `200` |
| POST | `/api/reservations` | Cliente; solicita reserva, `201` |
| GET | `/api/reservations` | Cliente vê suas reservas; recepcionista vê todas, `200` |
| GET | `/api/reservations/{id}` | Proprietário ou recepcionista, `200` |
| PUT | `/api/reservations/{id}/dates` | Proprietário ou recepcionista; altera datas, `200` |
| PUT | `/api/reservations/{id}/approve` | Recepcionista; aprova, `200` |
| PUT | `/api/reservations/{id}/cancel` | Cliente proprietário; cancela, `200` |

Nos CRUDs de quartos, tipos e características, POST retorna `201` e `Location`,
GET e PUT retornam `200` e DELETE retorna `204`. As listas são ordenadas por ID,
exceto quartos, ordenados pelo número. As listas de quartos incluem inativos.

## Contas

O cadastro de cliente recebe `name`, `email`, `password`, `phone` e `document`.
Nome e e-mail são obrigatórios e limitados a 255 caracteres. A senha é obrigatória,
não pode conter apenas espaços e deve ter de 8 a 128 caracteres. Telefone e
documento são opcionais e limitados a 255 caracteres. O e-mail é normalizado
para minúsculas, sem espaços nas extremidades, e é único. Usuário e perfil
são criados na mesma transação. O servidor atribui `CLIENT` e `active: true`.

PUT `/api/users/me` recebe `name`, `email`, `phone` e `document`. Os campos
obrigatórios devem ser reenviados; omitir telefone ou documento remove seu valor
anterior. Recepcionistas não possuem perfil de cliente e não recebem esses dois
campos. Perfil de acesso, ID e estado ativo não são editáveis por esse contrato.
PUT `/api/users/me/password` recebe `currentPassword` e `newPassword`.
Após sucesso em qualquer atualização, autentique novamente.

Somente recepcionistas listam usuários, consultam contas de terceiros ou excluem
usuários. A exclusão é física e recusa contas com qualquer reserva, mesmo
cancelada. O perfil vinculado é removido junto à conta sem reservas. Não é
permitido excluir a própria conta de recepção ou o último recepcionista ativo.

## Recuperação de senha

Configure `PASSWORD_RESET_ENABLED=true`, a URL da página `/redefinir-senha`,
o remetente e as credenciais SMTP, conforme `.env.example`.
Sem habilitar, as rotas retornam
`503`; o restante da aplicação continua funcionando.

`POST /api/auth/forgot-password` recebe `{ "email": "cliente@example.com" }`
e retorna `202` com confirmação genérica. O envio acontece em segundo plano.
`POST /api/auth/reset-password` recebe `token` e `newPassword` do formulário,
retorna `204` e revoga sessões. Ambas exigem CSRF, mesmo sem login.
O link é de uso único, expira em 15 minutos por padrão e não é retornado pela API.
Falhas de SMTP são registradas sem expor o token; `202` não garante entrega.
Limites de tentativa retornam `429` e `Retry-After` em segundos.
No Render gratuito, as portas SMTP 25/465/587 são bloqueadas. Usar um provedor
com porta alternativa compatível, como 2525 com STARTTLS.

Para investigar um pedido aceito sem recebimento, consulte os logs do backend.
Eles indicam processamento, conta inexistente/inativa, intervalo de reenvio,
início do envio e aceite pelo servidor SMTP. Em falhas, `tipos` identifica as
exceções e `codigosSMTP` informa códigos de rejeição disponíveis, sem registrar
e-mail, senha, corpo da mensagem ou link de recuperação. `MailAuthenticationException`
ou código `535` apontam para autenticação; `UnknownHostException`, `ConnectException`
e `SocketTimeoutException` apontam para DNS, conexão e timeout. Na Brevo, `450`
pode indicar que o envio transacional da conta ainda não foi ativado; confira o
status em Transacional e a [documentação de diagnóstico do provedor](https://help.brevo.com/hc/en-us/articles/115000188150-Troubleshooting-Issues-with-Brevo-SMTP).
O aceite SMTP não confirma entrega na caixa de entrada: a etapa seguinte deve
ser conferida nos logs transacionais do provedor.

## Quartos e características: N:N

`Amenity` representa uma característica da acomodação, como Wi-Fi,
ar-condicionado ou varanda, com `id`, `name` e `description`. O nome é obrigatório;
os textos são limitados a 255 caracteres. O CRUD usa `/api/amenities`.

Um quarto pode ter várias características e uma característica pode aparecer
em vários quartos. `Room.amenities` utiliza `@ManyToMany` e a tabela `room_amenity`,
com chaves estrangeiras para as duas entidades. Não há exclusão em cascata das
características ao excluir um quarto. Uma característica vinculada não pode ser
excluída; remova primeiro sua associação nos quartos.

Exemplo de criação/atualização de quarto, usando IDs existentes:

```json
{
  "name": "Quarto 101",
  "number": 101,
  "typeId": 1,
  "description": "Vista para o jardim",
  "capacity": 2,
  "pricePerNight": 180.50,
  "active": true,
  "amenityIds": [1, 2]
}
```

Número, tipo, capacidade, diária e ativo são obrigatórios. O número é positivo e
único; a capacidade é positiva; a diária aceita de zero a 99999999.99, com até
duas casas decimais. Nome e descrição são opcionais e limitados a 255 caracteres.
`amenityIds` é opcional; omitido, nulo ou vazio significa nenhuma característica,
inclusive no PUT. Os IDs devem existir. A resposta inclui `amenities` com ID,
nome e descrição de cada característica. `id` e `createdAt` são do servidor.

Tipos têm nome obrigatório e descrição opcional, ambos limitados a 255 caracteres.
Tipos vinculados a quartos e quartos vinculados a reservas não podem ser excluídos.
A redução de capacidade não pode contrariar reservas pendentes ou aprovadas.
Desativar um quarto impede novas solicitações, alterações de datas e aprovações.

## Reservas

```json
{
  "roomId": 1,
  "checkInDate": "2027-01-10",
  "checkOutDate": "2027-01-12",
  "guestCount": 2
}
```

Use IDs reais e datas futuras na validação. A disponibilidade pública recebe
`checkInDate` e `checkOutDate` como parâmetros de consulta, em formato `AAAA-MM-DD`.

- Entrada hoje ou futura; saída estritamente posterior; hóspedes positivos e
  dentro da capacidade do quarto ativo.
- O cliente é obtido da sessão. A reserva nasce `PENDING`.
- `PENDING` e `APPROVED` bloqueiam o período; `CANCELLED` libera o período.
- O período inclui a entrada e exclui a saída. Uma saída e outra entrada no
  mesmo dia são compatíveis.
- Somente recepcionistas aprovam reservas, sempre revalidando disponibilidade,
  capacidade, quarto ativo e data de entrada.
- O proprietário pode cancelar até o dia anterior à entrada, inclusive.
  No dia da entrada ou depois, o cancelamento é recusado.
- Proprietário e recepcionista podem alterar datas de reservas pendentes ou
  aprovadas, somente antes do dia da entrada atual. O corpo recebe apenas
  `checkInDate` e `checkOutDate`. O novo período precisa estar disponível.
- Uma alteração efetiva de datas retorna a reserva para `PENDING`, exigindo
  nova aprovação. Reenviar as mesmas datas preserva o status.
- Reservas canceladas não são alteradas nem aprovadas. Repetir cancelamento
  ou aprovação retorna conflito.
- Datas usam `HOTEL_TIME_ZONE`, com padrão `America/Recife`.
- Não há exclusão física de reservas; o histórico é preservado.

Solicitação, aprovação e alteração de datas usam transações e bloqueio
pessimista do quarto para serializar operações concorrentes. Transições também
bloqueiam a reserva. A consulta de disponibilidade é informativa; a gravação
sempre verifica novamente. Alterar ou excluir quarto usa o mesmo bloqueio.

## Respostas de erro

- `400`: campos, datas, IDs ou parâmetros inválidos; senha atual incorreta.
- `401`: sessão ausente, expirada ou revogada; login inválido.
- `403`: perfil sem permissão ou token CSRF ausente/inválido.
- `404`: entidade inexistente ou reserva que pertence a outro cliente.
- `409`: duplicidade, período ocupado, quarto inativo, vínculo impeditivo ou
  transição de status/prazo não permitida.
- `429`: limite de tentativas de recuperação ou fila de envio cheia; observar `Retry-After`.
- `503`: recuperação de senha desabilitada no ambiente.

Erros usam `application/problem+json`, com `type`, `title`, `status` e `detail`.
Validações de campos incluem `errors`. Dados internos do banco não são expostos.

## Arquitetura e cardinalidades

Organização por camadas: `controller`, `dto`, `entity`, `repository`, `service`,
`enums`, `exception`, `config` e `security`. Regras de negócio ficam nos serviços.

- `User` e `CustomerProfile`: 1:1, chave estrangeira obrigatória e única no perfil.
- `RoomType` e `Room`: 1:N.
- `CustomerProfile` e `Reservation`: 1:N.
- `Room` e `Reservation`: 1:N.
- `Room` e `Amenity`: N:N, com tabela de associação.

As seis entidades JPA são User, CustomerProfile, RoomType, Room, Reservation e
Amenity. Enums e DTOs não contam como entidades.

## Validação e entrega

A compilação limpa passou. A validação manual no Neon confirmou:

| Fluxo | Resultado verificado |
| --- | --- |
| Bootstrap | Primeiro recepcionista criado, ID preservado e sem duplicação após reinício |
| Cadastro de recepcionistas | Recepcionista recebe `201`; cliente recebe `403` |
| Edição de conta e senha | Dados atualizados, senha anterior recusada e sessões anteriores revogadas |
| Trocas simultâneas de senha | Uma concluiu com `204`; a segunda foi recusada com `400` |
| Exclusão de contas | Exclusiva de recepcionista; recusa reservas vinculadas e autoexclusão; sessão da conta excluída recebe `401` |
| N:N | Características compartilhadas entre quartos, remoção de vínculos, exclusão protegida e persistência após reinício |
| Mudança de datas | Período ocupado recusado sem alterar a reserva; novo período livre aceito e status retornado a `PENDING` |
| Concorrência nas datas | Duas mudanças para o mesmo período retornaram `200` e `409` |
| Prazos | Alteração no dia da entrada recusada; cancelamento na véspera aceito |
| Swagger/OpenAPI | 36 operações descritas, incluindo as duas de recuperação; contratos e erros presentes, recursos locais servidos com `200` |
| CSRF do Swagger | JavaScript executado contra a API: login, logout e novo login com renovação automática do token |

O acesso ao navegador automatizado estava indisponível; não houve inspeção
visual da interface. Os recursos HTTP, o JavaScript e os fluxos descritos acima
foram verificados diretamente. Todos os dados temporários usados na validação
foram removidos.

A verificação é por compilação e execução manual no PostgreSQL remoto, conforme
o escopo acadêmico. Não há banco em memória nem infraestrutura de testes
automatizados. O histórico Git deve identificar a participação dos integrantes.

### Validação da recuperação de senha em 20/11/2026

O empacotamento Maven com Java 21 passou. Os fluxos HTTP foram verificados em
um esquema temporário isolado no PostgreSQL configurado, com captura SMTP local:
resposta genérica, CSRF, validações, hash/expiração, intervalo de reenvio,
substituição e consumo único do token, revogação de sessões, alteração de
conta/senha e consumo concorrente (`204`/`400`). Também foram conferidos `429`
com Retry-After, sua exposição no CORS, modo desabilitado (`503`) e rollback
após falha simulada de SMTP, preservando o link anterior.

O processo e o esquema temporário foram removidos, sem alterar registros
preexistentes. Não houve envio externo de e-mail. Entrega por um provedor SMTP
real e funcionamento no deploy dependem da configuração do ambiente.

A integração com frontend está adiada. Não fazem parte desta entrega pagamentos,
reembolsos, notificações gerais, check-in/check-out operacionais,
rejeição ou exclusão física de reservas e cálculos financeiros de hospedagem.
O contrato de alteração de reserva abrange as datas; quarto e hóspedes são
preservados. A quantidade de entidades atende a equipes de até seis integrantes.

Para publicação, configure as variáveis no ambiente de hospedagem. CORS permite
por padrão `http://localhost:5173` e `http://localhost:3000`; altere
`CORS_ALLOWED_ORIGINS` quando necessário. O cookie SameSite=Lax atende ao
localhost em portas distintas; uso entre sites distintos exige rever sua política
na futura integração. A atualização automática do esquema não realiza renomeações
ou migrações de dados.
