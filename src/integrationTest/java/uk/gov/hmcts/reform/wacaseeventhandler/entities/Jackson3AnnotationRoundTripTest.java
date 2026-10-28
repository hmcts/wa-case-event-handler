package uk.gov.hmcts.reform.wacaseeventhandler.entities;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.documents.Document;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.Token;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson3AnnotationRoundTripTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void token_uses_snake_case_and_ignores_unknown_properties() {
        String json = jsonMapper.writeValueAsString(new Token("abc", "openid"));

        assertThat(json).contains("\"access_token\":\"abc\"");
        assertThat(json).contains("\"scope\":\"openid\"");
        assertThat(json).doesNotContain("accessToken");

        Token token = jsonMapper.readValue(
            "{\"access_token\":\"abc\",\"scope\":\"openid\",\"expires_in\":60}",
            Token.class
        );

        assertThat(token).isEqualTo(new Token("abc", "openid"));
    }

    @Test
    void document_uses_snake_case_field_names() {
        Document document = new Document("https://doc", "https://bin", "file.pdf");
        String json = jsonMapper.writeValueAsString(document);

        assertThat(json).contains("\"document_url\":\"https://doc\"");
        assertThat(json).contains("\"document_binary_url\":\"https://bin\"");
        assertThat(json).contains("\"document_filename\":\"file.pdf\"");
        assertThat(json).doesNotContain("documentUrl");
        assertThat(jsonMapper.readValue(json, Document.class)).isEqualTo(document);
    }
}
