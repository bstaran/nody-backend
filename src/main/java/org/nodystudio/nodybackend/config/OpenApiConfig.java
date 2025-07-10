package org.nodystudio.nodybackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 설정을 위한 Configuration 클래스
 */
@Configuration
public class OpenApiConfig {

  /**
   * OpenAPI 설정을 정의합니다.
   *
   * @return OpenAPI 설정
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Nody Backend API")
            .version("0.0.0")
            .description("Nody 애플리케이션의 백엔드 API 문서입니다."))
        .servers(List.of(
            new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버"),
            new Server()
                .url("")
                .description("운영 서버")))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 토큰을 사용한 인증")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}