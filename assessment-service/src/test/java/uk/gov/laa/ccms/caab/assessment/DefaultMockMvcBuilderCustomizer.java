package uk.gov.laa.ccms.caab.assessment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Applies a default Authorization header to all new mockMvc instances.
 */
@Component
public class DefaultMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

    final String authHeader;
    final String authorizedClients;

    @Autowired
    public DefaultMockMvcBuilderCustomizer(
            @Value("${laa.ccms.springboot.starter.auth.authentication-header}") String authHeader,
            @Value("${laa.ccms.springboot.starter.auth.authorized-clients}") String authorizedClients) {
        this.authHeader = authHeader;
        this.authorizedClients = authorizedClients;
    }

    /**
     * Read a configured access token and apply the Authorization header to all requests made against
     * a MockMvc instance.
     *
     * @param builder the {@code MockMvc} builder to customize
     */
    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        String accessToken;
        try {
            accessToken = new ObjectMapper().readTree(authorizedClients).at("/0/token").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to retrieve configured access token.", e);
        }

        RequestBuilder defaultRequestBuilder = MockMvcRequestBuilders.get("/")
                .header(authHeader, accessToken);
        builder.defaultRequest(defaultRequestBuilder);
    }

}
