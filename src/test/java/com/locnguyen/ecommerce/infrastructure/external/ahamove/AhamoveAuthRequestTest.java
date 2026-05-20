package com.locnguyen.ecommerce.infrastructure.external.ahamove;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locnguyen.ecommerce.infrastructure.external.ahamove.dto.AhamoveAuthRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AhamoveAuthRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesApiKeyUsingProviderExpectedSnakeCaseField() throws Exception {
        AhamoveAuthRequest request = AhamoveAuthRequest.builder()
                .mobile("84338710667")
                .apiKey("test-api-key")
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"mobile\":\"84338710667\"");
        assertThat(json).contains("\"api_key\":\"test-api-key\"");
        assertThat(json).doesNotContain("\"apiKey\"");
    }
}
