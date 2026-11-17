window.onload = () => {
    window.ui = SwaggerUIBundle({
        url: "/v3/api-docs",
        dom_id: "#swagger-ui",
        deepLinking: true,
        validatorUrl: null,
        withCredentials: true,
        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
        layout: "StandaloneLayout",
        requestInterceptor: async (request) => {
            const url = new URL(request.url, window.location.origin);
            if (url.origin !== window.location.origin) {
                throw new Error("A documentação permite requisições apenas para esta aplicação.");
            }
            request.credentials = "same-origin";
            const method = (request.method || "GET").toUpperCase();
            if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
                const response = await fetch("/api/auth/csrf", {
                    credentials: "same-origin",
                    cache: "no-store"
                });
                if (!response.ok) {
                    throw new Error("Não foi possível obter o token CSRF. Atualize a página e tente novamente.");
                }
                const csrf = await response.json();
                request.headers = request.headers || {};
                request.headers[csrf.headerName] = csrf.token;
            }
            return request;
        }
    });
};
