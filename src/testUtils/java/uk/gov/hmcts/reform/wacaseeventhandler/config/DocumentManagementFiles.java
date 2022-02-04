package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.documents.Document;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.documents.DocumentNames;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DocumentManagementUploader;
import uk.gov.hmcts.reform.wacaseeventhandler.utils.BinaryResourceLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.SERVICE_AUTHORIZATION;

@Component
public class DocumentManagementFiles {

    private final Map<DocumentNames, Document> documents = new ConcurrentHashMap<>();
    private Collection<Resource> documentResources;
    @Autowired
    private AuthorizationProvider authorizationProvider;
    @Autowired
    private DocumentManagementUploader documentManagementUploader;

    public void prepare() throws IOException {
        documentResources =
            BinaryResourceLoader
                .load("/documents/*")
                .values();
    }

    public Document uploadDocumentAs(DocumentNames document, TestAuthenticationCredentials credentials) {

        Optional<Resource> maybeResource = documentResources.stream()
            .filter(res -> {
                String filename = formatFileName(res.getFilename());

                return filename.equals(document.toString());
            }).findFirst();

        if (maybeResource.isPresent()) {

            Resource documentResource = maybeResource.get();

            String filename = documentResource.getFilename().toUpperCase();

            String contentType;

            if (filename.endsWith(".PDF")) {
                contentType = "application/pdf";

            } else if (filename.endsWith(".DOC")) {
                contentType = "application/msword";

            } else if (filename.endsWith(".DOCX")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            } else {
                throw new RuntimeException("Missing content type mapping for document: " + filename);
            }

            String userToken = credentials.getHeaders().getValue(AUTHORIZATION);
            String serviceToken = credentials.getHeaders().getValue(SERVICE_AUTHORIZATION);
            UserInfo userInfo = authorizationProvider.getUserInfo(userToken);

            return documentManagementUploader.upload(
                documentResource,
                contentType,
                userToken,
                serviceToken,
                userInfo
            );
        } else {
            throw new IllegalStateException(
                String.format("Resource for document '{}' not found", document));
        }
    }

    public Document getDocumentAs(DocumentNames document, TestAuthenticationCredentials credentials) {
        return documents.computeIfAbsent(
            document,
            doc -> uploadDocumentAs(document, credentials)
        );
    }

    private String formatFileName(String fileName) {
        return fileName
            .replace(".", "_")
            .replace("-", "_")
            .toUpperCase();
    }
}
