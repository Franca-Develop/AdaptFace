package com.dev.adaptface.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AdaptFace API",
                version = "v1",
                description = "API do controle de acesso escolar por reconhecimento facial, RFID e código. "
                        + "Endpoints administrativos exigem JWT; o terminal usa uma chave de máquina.",
                contact = @Contact(name = "Equipe AdaptFace", url = "https://github.com/Franca-Develop/AdaptFace"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")),
        servers = {
                @Server(url = "http://localhost:8080", description = "Docker/local"),
                @Server(url = "/", description = "Servidor atual")
        },
        tags = {
                @Tag(name = "Autenticação", description = "Login e criação segura do primeiro administrador"),
                @Tag(name = "Cadastros", description = "Cadastro transacional de pessoa, matrícula e credenciais"),
                @Tag(name = "Usuários", description = "Gestão de alunos e funcionários"),
                @Tag(name = "Instituições", description = "Gestão das unidades de ensino"),
                @Tag(name = "Matrículas", description = "Matrículas e situação de acesso"),
                @Tag(name = "Credenciais", description = "Credenciais faciais, RFID e código"),
                @Tag(name = "Acesso", description = "Validação no terminal e auditoria de acessos")
        },
        externalDocs = @ExternalDocumentation(
                description = "Código-fonte e documentação do AdaptFace",
                url = "https://github.com/Franca-Develop/AdaptFace"))
@SecurityScheme(name = "bearerAuth", description = "JWT retornado pelo login administrativo",
        type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityScheme(name = "machineApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key", description = "Chave exclusiva do terminal de reconhecimento")
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class OpenApiConfig {
}
