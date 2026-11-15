package pl.piomin.services.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.piomin.services.exception.ApiExceptionHandler.Problem;

import java.util.List;

/**
 * The query service's part of the contract. The gateway merges it with the query and reference services' parts
 * into {@code contract/openapi.json}; {@code info} there comes from the gateway.
 */
@Configuration
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI openApi(BuildProperties build) {
        return new OpenAPI().info(new Info().title("Order Management API - queries").version(build.getVersion()))
                .servers(List.of(new Server().url("/")));
    }

    /**
     * Problem Details responses by convention: 400 on lists (an invalid filter or sort), 404 on {@code {id}} paths.
     * {@code @ApiResponse} on the methods would drop the generated success response.
     */
    @Bean
    OpenApiCustomizer problemResponses() {
        return openApi -> {
            openApi.getComponents().getSchemas().putAll(ModelConverters.getInstance(true).readAll(Problem.class));
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (method != HttpMethod.GET) {
                    return;
                }
                if (path.contains("{id}")) {
                    addProblem(operation, "404", "Not found");
                } else {
                    addProblem(operation, "400", "Invalid filter (field errors in `errors`) or sort");
                }
            }));
        };
    }

    private static void addProblem(Operation operation, String status, String description) {
        Schema<?> problem = new Schema<>().$ref("#/components/schemas/Problem");
        operation.getResponses().addApiResponse(status, new ApiResponse().description(description)
                .content(new Content().addMediaType("application/problem+json", new MediaType().schema(problem))));
    }
}
