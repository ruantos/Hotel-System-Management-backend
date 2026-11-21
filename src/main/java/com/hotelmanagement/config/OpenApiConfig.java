package com.hotelmanagement.config;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hotelOpenApi() {
        return new OpenAPI().info(new Info().title("Hotel Management API").version("1.0.0")
                .description("""
                        API de quartos, contas e reservas. Datas usam o calendário America/Recife por padrão.

                        **Como testar:** cadastre um cliente ou use a conta de recepção configurada localmente.
                        Execute POST /api/auth/login com e-mail e senha no formulário. O navegador mantém a
                        sessão pelo cookie HttpOnly JSESSIONID. GET /api/auth/me identifica a conta atual.
                        POST /api/auth/logout encerra a sessão. Não é necessário preencher Authorize manualmente.

                        Esta interface obtém e envia CSRF automaticamente antes de cada escrita, inclusive
                        cadastro, login e logout. Em outros clientes HTTP, consulte GET /api/auth/csrf e envie
                        o token no cabeçalho indicado, preservando os cookies. Renove-o após login/logout.

                        CLIENT acessa somente suas reservas e sua conta. RECEPTIONIST gerencia quartos,
                        características, tipos, usuários e aprova reservas. Apenas RECEPTIONIST exclui usuários.
                        Alterar a própria conta ou senha encerra suas sessões e exige novo login.

                        Recuperação de senha: POST /api/auth/forgot-password aceita e-mail e retorna 202
                        genérico; o link é enviado por SMTP em segundo plano quando a funcionalidade está
                        habilitada. POST /api/auth/reset-password recebe token e newPassword e retorna 204.
                        Ambos são públicos e exigem CSRF. Tokens expiram, são de uso único e não aparecem
                        nas respostas. A redefinição revoga sessões e não autentica automaticamente.

                        Reservas PENDING e APPROVED bloqueiam o período [entrada, saída). Cancelamento e
                        mudança de datas só são permitidos antes do dia da entrada atual. Alterar datas de
                        uma reserva aprovada exige nova aprovação e passa seu status para PENDING.
                        """))
                .components(new Components().addSecuritySchemes("session", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                        .description("Cookie gerenciado pelo navegador após POST /api/auth/login."))
                        .addSchemas("ApiProblem", new ObjectSchema()
                                .addProperty("type", new StringSchema().example("about:blank"))
                                .addProperty("title", new StringSchema().example("Conflict"))
                                .addProperty("status", new IntegerSchema().example(409))
                                .addProperty("detail", new StringSchema().example("O quarto não está disponível no período informado."))
                                .addProperty("instance", new StringSchema().example("/api/reservations"))
                                .addProperty("errors", new ObjectSchema().additionalProperties(new StringSchema()))));
    }

    @Bean
    public OpenApiCustomizer completeApiDocumentation() {
        return api -> {
            Schema<Object> loginForm = new ObjectSchema();
            loginForm.addProperty("email", new StringSchema().format("email").example("cliente@example.com"));
            loginForm.addProperty("password", new StringSchema().format("password").writeOnly(true));
            loginForm.setRequired(List.of("email", "password"));
            RequestBody loginBody = new RequestBody().required(true).content(new Content()
                    .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(loginForm)));
            api.path("/api/auth/login", new PathItem().post(new Operation()
                    .operationId("login")
                    .requestBody(loginBody)
                    .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Sessão iniciada; cookie JSESSIONID emitido."))
                            .addApiResponse("401", problem("Credenciais inválidas ou conta inativa."))
                            .addApiResponse("403", problem("Token CSRF ausente ou inválido.")))));
            api.path("/api/auth/logout", new PathItem().post(new Operation().operationId("logout")
                    .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Sessão encerrada e cookie removido."))
                            .addApiResponse("403", problem("Token CSRF ausente ou inválido.")))));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) ->
                    document(path, method, operation)));
        };
    }

    private void document(String path, PathItem.HttpMethod method, Operation operation) {
        boolean read = method == PathItem.HttpMethod.GET;
        boolean filterEndpoint = path.equals("/api/auth/login") || path.equals("/api/auth/logout");
        boolean forgotPassword = path.equals("/api/auth/forgot-password");
        boolean resetPassword = path.equals("/api/auth/reset-password");
        boolean passwordResetEndpoint = forgotPassword || resetPassword;
        boolean publicEndpoint = path.equals("/api/auth/csrf") || path.equals("/api/auth/register")
                || passwordResetEndpoint || filterEndpoint || (read && (path.startsWith("/api/rooms")
                || path.startsWith("/api/room-types") || path.startsWith("/api/amenities")));
        String tag = path.startsWith("/api/auth") ? "Autenticação"
                : path.startsWith("/api/users") ? "Contas"
                : path.startsWith("/api/reservations") ? "Reservas"
                : path.startsWith("/api/room-types") ? "Tipos de quarto"
                : path.startsWith("/api/amenities") ? "Características de quartos" : "Quartos";
        operation.setTags(List.of(tag));
        operation.setSummary(summary(path, method, tag));
        String permission = publicEndpoint ? "Acesso público."
                : path.startsWith("/api/users/me") || path.equals("/api/auth/me") ? "Conta autenticada."
                : path.startsWith("/api/users") || (!read && !path.startsWith("/api/reservations"))
                        || path.endsWith("/approve") ? "Exclusivo de RECEPTIONIST."
                : path.equals("/api/reservations") && !read || path.endsWith("/cancel")
                        ? "Exclusivo de CLIENT; somente reservas próprias."
                        : "CLIENT acessa reservas próprias; RECEPTIONIST acessa todas.";
        operation.setDescription(permission + " " + details(path)
                + (read ? "" : " Escrita protegida por CSRF; esta interface envia o token automaticamente."));
        operation.setSecurity(publicEndpoint ? new ArrayList<>() : List.of(new SecurityRequirement().addList("session")));
        if (filterEndpoint) {
            return;
        }
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        String success = forgotPassword ? "202"
                : resetPassword || method == PathItem.HttpMethod.DELETE || path.endsWith("/password") ? "204"
                : method == PathItem.HttpMethod.POST ? "201" : "200";
        ApiResponse successResponse = responses.remove("200");
        if (successResponse == null) {
            successResponse = new ApiResponse();
        }
        successResponse.setDescription(forgotPassword ? "Solicitação aceita para processamento; resposta genérica sem revelar se a conta existe."
                : success.equals("201") ? "Registro criado."
                : success.equals("204") ? "Operação concluída sem corpo de resposta." : "Operação concluída.");
        if (success.equals("204")) {
            successResponse.setContent(null);
        }
        if (success.equals("201") && !path.equals("/api/auth/register")) {
            successResponse.addHeaderObject("Location", new Header().description("URL do registro criado.")
                    .schema(new StringSchema().format("uri")));
        }
        responses.addApiResponse(success, successResponse);
        if (operation.getRequestBody() != null || operation.getParameters() != null && !operation.getParameters().isEmpty()) {
            responses.addApiResponse("400", problem("Campos, datas ou parâmetros inválidos; erros de campos aparecem em errors."));
        }
        if (!publicEndpoint) {
            responses.addApiResponse("401", problem("Sessão ausente, expirada ou revogada."));
        }
        if (!read || !publicEndpoint) {
            responses.addApiResponse("403", problem("Perfil sem permissão ou token CSRF ausente/inválido."));
        }
        if (path.contains("{id}") || !read && (path.equals("/api/rooms") || path.equals("/api/reservations"))) {
            responses.addApiResponse("404", problem("Registro inexistente ou reserva não pertencente ao cliente."));
        }
        if (!read && !path.endsWith("/password") && !passwordResetEndpoint) {
            responses.addApiResponse("409", problem("Conflito de disponibilidade, duplicidade, vínculo existente ou transição não permitida."));
        }
        if (passwordResetEndpoint) {
            responses.addApiResponse("429", problem("Muitas tentativas ou fila de envio cheia; aguarde Retry-After segundos.")
                    .addHeaderObject("Retry-After", new Header().description("Segundos até uma nova tentativa.")
                            .schema(new IntegerSchema())));
            responses.addApiResponse("503", problem("Recuperação de senha desabilitada no ambiente."));
        }
    }

    private String summary(String path, PathItem.HttpMethod method, String tag) {
        return switch (path) {
            case "/api/auth/csrf" -> "Obter token CSRF";
            case "/api/auth/login" -> "Iniciar sessão";
            case "/api/auth/logout" -> "Encerrar sessão";
            case "/api/auth/register" -> "Cadastrar cliente";
            case "/api/auth/forgot-password" -> "Solicitar recuperação de senha";
            case "/api/auth/reset-password" -> "Redefinir senha por token";
            case "/api/auth/me" -> "Consultar usuário autenticado";
            case "/api/users/me" -> method == PathItem.HttpMethod.GET ? "Consultar minha conta" : "Atualizar minha conta";
            case "/api/users/me/password" -> "Alterar minha senha";
            case "/api/users/receptionists" -> "Cadastrar recepcionista";
            case "/api/reservations/{id}/dates" -> "Alterar datas da reserva";
            case "/api/reservations/{id}/approve" -> "Aprovar reserva";
            case "/api/reservations/{id}/cancel" -> "Cancelar reserva";
            case "/api/rooms/{id}/availability" -> "Consultar disponibilidade por período";
            default -> switch (method) {
                case GET -> (path.contains("{id}") ? "Consultar por ID: " : "Listar: ") + tag;
                case POST -> "Cadastrar: " + tag;
                case PUT -> "Atualizar: " + tag;
                case DELETE -> "Excluir: " + tag;
                default -> tag;
            };
        };
    }

    private String details(String path) {
        if (path.equals("/api/auth/forgot-password")) {
            return "Recebe JSON com email. Retorna 202 com message idêntica para conta ativa, inativa, inexistente ou reenvio no intervalo mínimo. O envio SMTP é assíncrono; 202 não confirma a entrega do e-mail. Por padrão, token válido por 15 minutos e reenvio após 60 segundos. Limite de 20 solicitações por IP em 15 minutos. Funcionalidade depende de configuração no servidor.";
        }
        if (path.equals("/api/auth/reset-password")) {
            return "Recebe JSON com token e newPassword (8 a 128 caracteres). Token opaco do link, de uso único; inválido, usado ou expirado retorna 400. Redefinição revoga sessões e encerra a sessão atual, sem login automático. Obtenha novo CSRF antes de entrar. Limite de 30 tentativas por IP em 15 minutos.";
        }
        if (path.endsWith("/dates")) {
            return "Datas ISO AAAA-MM-DD. Apenas antes da entrada atual; períodos livres são revalidados com bloqueio do quarto. Datas alteradas tornam a reserva PENDING; datas idênticas preservam o status.";
        }
        if (path.endsWith("/cancel")) {
            return "Permitido até o dia anterior à entrada, inclusive, no fuso do hotel. Preserva o registro e libera o período.";
        }
        if (path.endsWith("/approve")) {
            return "Aprova somente PENDING, revalidando data, capacidade, quarto ativo e disponibilidade.";
        }
        if (path.equals("/api/users/me") || path.endsWith("/password")) {
            return "Atualizações exigem novo login e revogam as sessões anteriores. PUT de conta substitui nome, e-mail e dados opcionais do cliente; não altera perfil de acesso.";
        }
        if (path.startsWith("/api/users")) {
            return "Dados públicos da conta, sem senha ou hash. Exclusão recusa usuário com reservas, a própria conta de recepção e o último recepcionista ativo. Novos recepcionistas são ativos e não recebem perfil de cliente.";
        }
        if (path.startsWith("/api/reservations")) {
            return "O cliente é obtido da sessão. Reservas PENDING e APPROVED bloqueiam [entrada, saída). Entrada hoje ou futura, saída posterior e hóspedes dentro da capacidade. Consultas são ordenadas por ID.";
        }
        if (path.endsWith("/availability")) {
            return "Datas ISO AAAA-MM-DD. Resposta informativa; a disponibilidade definitiva é verificada na gravação. Saída e nova entrada no mesmo dia são compatíveis.";
        }
        if (path.startsWith("/api/rooms")) {
            return "Número único, capacidade positiva e diária não negativa (até 8 inteiros e 2 decimais). amenityIds associa características existentes; omitir no PUT remove os vínculos. Quartos com reservas não podem ser excluídos. Listagem ordenada por número.";
        }
        if (path.startsWith("/api/room-types") || path.startsWith("/api/amenities")) {
            return "Nome obrigatório e textos limitados a 255 caracteres. PUT substitui os campos. Exclusão recusa registros vinculados a quartos. Listagem ordenada por ID.";
        }
        if (path.equals("/api/auth/login")) {
            return "Formulário email/password. Retorna 204, altera o ID da sessão e emite cookie HttpOnly. Conta inexistente, inativa ou senha incorreta retornam a mesma resposta 401.";
        }
        if (path.equals("/api/auth/register")) {
            return "Cria CLIENT ativo e perfil na mesma transação. E-mail normalizado, único; senha de 8 a 128 caracteres armazenada como hash. Telefone e documento opcionais.";
        }
        if (path.equals("/api/auth/csrf")) {
            return "Retorna headerName e token vinculados à sessão. Preserve cookies e renove o token após login/logout. A interface faz isso automaticamente.";
        }
        return "Senha e hash nunca são retornados. A sessão utiliza cookie HttpOnly e expira após 30 minutos de inatividade.";
    }

    private ApiResponse problem(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
