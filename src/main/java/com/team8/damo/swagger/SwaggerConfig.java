package com.team8.damo.swagger;

import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.swagger.annotation.ApiErrorResponse;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.SneakyThrows;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String jwt = "Bearer Token";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
        Components components = new Components().addSecuritySchemes(jwt, new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
            //.scheme("cookie")
            //.bearerFormat("JWT")
        );

        Info info = new Info()
            .title("Damo API")
            .description("Damo API 입니다.")
            .version("1.0.0");

        return new OpenAPI()
            .info(info)
            .addSecurityItem(securityRequirement)
            .components(components);
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return ((operation, handlerMethod) -> {
            ApiErrorResponses apiErrorResponses = handlerMethod.getMethodAnnotation(ApiErrorResponses.class);

            if (apiErrorResponses != null) {
                generateErrorResponseExample(operation, apiErrorResponses.value());
            } else {
                ApiErrorResponse apiErrorResponse = handlerMethod.getMethodAnnotation(ApiErrorResponse.class);

                if (apiErrorResponse != null) {
                    generateErrorResponseExample(operation, apiErrorResponse.value());
                }
            }

            addResponseBodyWrapperSchemaExample(operation, BaseResponse.class, "data");
            return operation;
        });
    }

    private void addResponseBodyWrapperSchemaExample(Operation operation, Class<?> type, String wrapFieldName) {
        final ApiResponses responses = operation.getResponses();

        responses.forEach((statusCode, apiResponse) -> {
            if (apiResponse != null) {
                Content content = apiResponse.getContent();
                if (content != null) {
                    content.keySet()
                      .forEach(mediaTypeKey -> {
                          final MediaType mediaType = content.get(mediaTypeKey);
                          mediaType.schema(wrapSchema(Integer.parseInt(statusCode), mediaType.getSchema(), type, wrapFieldName));
                      });
                }
            }
        });
    }

    @SneakyThrows
    private <T> Schema<T> wrapSchema(int statusCode, Schema<?> originalSchema, Class<T> type, String wrapFieldName) {
        final Schema<T> wrapperSchema = new Schema<>();
        final HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        wrapperSchema.addProperty("httpStatus", new Schema<>().type("String").example(httpStatus.getReasonPhrase()));
        wrapperSchema.addProperty("data", originalSchema);
        wrapperSchema.addProperty("errorMessage", new Schema<>().type("boolean").example(null));

        return wrapperSchema;
    }

    private void generateErrorResponseExample(Operation operation, ErrorCode errorCode) {
        ApiResponses responses = operation.getResponses();

        ExampleHolder exampleHolder = ExampleHolder.builder()
          .holder(getExample(errorCode))
          .name(errorCode.toString())
          .code(errorCode.getHttpStatus().value())
          .message(errorCode.getMessage())
          .status(errorCode.getHttpStatus())
          .build();

        addExamplesToResponses(responses, exampleHolder);
    }

    private void generateErrorResponseExample(Operation operation, ErrorCode[] errorCodes) {
        ApiResponses responses = operation.getResponses();

        Map<Integer, List<ExampleHolder>> statusExampleHolders = Arrays.stream(errorCodes)
          .map(
            errorCode -> ExampleHolder.builder()
              .holder(getExample(errorCode))
              .name(errorCode.toString())
              .code(errorCode.getHttpStatus().value())
              .message(errorCode.getMessage())
              .status(errorCode.getHttpStatus())
              .build()
          )
          .collect(Collectors.groupingBy(ExampleHolder::getCode));

        addExamplesToResponses(responses, statusExampleHolders);
    }

    private Example getExample(ErrorCode errorCode) {
        ErrorDto errorDto = ErrorDto.from(errorCode);
        Example example = new Example();
        example.setValue(errorDto);
        return example;
    }

    private void addExamplesToResponses(ApiResponses apiResponses, ExampleHolder exampleHolder) {
        Content content = new Content();
        MediaType mediaType = new MediaType();
        ApiResponse apiResponse = new ApiResponse();

        mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder());
        content.addMediaType("application/json", mediaType);
        apiResponse.setContent(content);
        apiResponse.setDescription(exampleHolder.getStatus().name());
        apiResponses.addApiResponse(String.valueOf(exampleHolder.getCode()), apiResponse);
    }

    private void addExamplesToResponses(ApiResponses apiResponses,
                                        Map<Integer, List<ExampleHolder>> statusExampleHolders) {
        statusExampleHolders.forEach(
          (status, v) -> {
              Content content = new Content();
              MediaType mediaType = new MediaType();
              ApiResponse apiResponse = new ApiResponse();

              v.forEach(
                exampleHolder -> {
                    mediaType.addExamples(
                      exampleHolder.getName(),
                      exampleHolder.getHolder()
                    );
                    apiResponse.setDescription(exampleHolder.getStatus().name());
                }
              );

              content.addMediaType("application/json", mediaType);
              apiResponse.setContent(content);

              apiResponses.addApiResponse(String.valueOf(status), apiResponse);
          }
        );
    }
}
