package uk.gov.laa.ccms.caab.assessment.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * The Spring OpenAPI Generator does not currently generate SecurityScheme definitions. It is required for Authorization
 * in the Swagger UI to behave properly.
 * See https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/spring.md#feature-set.
 * This can be removed when it has been implemented.
 */
@SecurityScheme(
        name = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "Authorization"
)
public class SwaggerConfiguration { }