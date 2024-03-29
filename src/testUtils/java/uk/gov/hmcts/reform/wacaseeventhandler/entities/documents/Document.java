package uk.gov.hmcts.reform.wacaseeventhandler.entities.documents;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static java.util.Objects.requireNonNull;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@EqualsAndHashCode
@ToString
public class Document {

    private String documentUrl;
    private String documentBinaryUrl;
    private String documentFilename;

    private Document() {
        // noop -- for deserializer
    }

    public Document(
        String documentUrl,
        String documentBinaryUrl,
        String documentFilename
    ) {
        requireNonNull(documentUrl);
        requireNonNull(documentBinaryUrl);
        requireNonNull(documentFilename);

        this.documentUrl = documentUrl;
        this.documentBinaryUrl = documentBinaryUrl;
        this.documentFilename = documentFilename;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public String getDocumentBinaryUrl() {
        return documentBinaryUrl;
    }

    public String getDocumentFilename() {
        return documentFilename;
    }
}
