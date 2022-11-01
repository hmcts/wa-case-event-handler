package uk.gov.hmcts.reform.wacaseeventhandler.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.ccd.client.model.Classification;
import uk.gov.hmcts.reform.wacaseeventhandler.config.GivensBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.config.RestApiActions;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.RoleAssignment;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.RoleAssignmentResource;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.GrantType;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleType;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdamService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.RoleAssignmentServiceApi;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleType.CASE;
import static uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleType.ORGANISATION;

@Slf4j
public class Common {

    public static final DateTimeFormatter ROLE_ASSIGNMENT_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final String R2_ROLE_ASSIGNMENT_REQUEST =
        "requests/roleAssignment/r2/set-organisational-role-assignment-request.json";
    private static String DELETE_REQUEST = "{\n"
                                           + "    \"deleteReason\": \"clean up running process instances\",\n"
                                           + "    \"processInstanceIds\": [\n"
                                           + "    \"{PROCESS_ID}\"\n"
                                           + "    ],\n"
                                           + "    \"skipCustomListeners\": true,\n"
                                           + "    \"skipSubprocesses\": true,\n"
                                           + "    \"failIfNotExists\": false\n"
                                           + "    }";

    private final GivensBuilder given;
    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationProvider authorizationProvider;
    private final IdamService idamService;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Common(GivensBuilder given,
                  RestApiActions camundaApiActions,
                  RestApiActions restApiActions,
                  AuthorizationProvider authorizationProvider,
                  IdamService idamService,
                  RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.given = given;
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
        this.idamService = idamService;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public TestVariables createWaCase() {

        String caseId = given.createWaCcdCase();

        return new TestVariables(caseId, null, null);
    }

    public void setupCftOrganisationalRoleAssignment(Headers headers, String jurisdiction) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", jurisdiction
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers, jurisdiction);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");

        postRoleAssignment(
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

    }

    public void setupCftOrganisationalRoleAssignmentForWA(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers, "WA");
        createCaseAllocator(userInfo, headers, "WA");
        createStandardTribunalCaseworker(userInfo, headers, "WA", "WaCaseType");
    }

    private void createStandardTribunalCaseworker(UserInfo userInfo, Headers headers,
                                                  String jurisdiction, String caseType) {
        log.info("Creating Standard Tribunal caseworker organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(), "tribunal-caseworker",
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "caseType", caseType,
                "jurisdiction", jurisdiction
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createCaseAllocator(UserInfo userInfo, Headers headers, String jurisdiction) {
        log.info("Creating case allocator organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(), "case-allocator",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public TestVariables setupWaTaskAndRetrieveIds() {

        String caseId = given.createWaCcdCase();

        List<CamundaTask> response = given
            .createTaskWithCaseId(caseId, false, "WA", "WaCaseType")
            .and()
            .retrieveTaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public void clearAllRoleAssignments(Headers headers, String jurisdiction) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers, jurisdiction);
    }

    public void cleanUpTask(Headers authenticationHeaders, List<String> caseIds) {

        Set<String> processIds = new HashSet<>();

        caseIds
            .forEach(caseId -> processIds.addAll(getProcesses(authenticationHeaders, caseId)));

        processIds
            .forEach(processId -> deleteProcessInstance(authenticationHeaders, processId));

    }



    private void clearAllRoleAssignmentsForUser(String userId, Headers headers, String jurisdiction) {
        String userToken = headers.getValue(AUTHORIZATION);
        String serviceToken = headers.getValue(SERVICE_AUTHORIZATION);

        RoleAssignmentResource response = null;

        try {
            //Retrieve All role assignments
            response = roleAssignmentServiceApi.getRolesForUser(userId, userToken, serviceToken);
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.NOT_FOUND.value()) {
                log.info("No roles found, nothing to delete.");
            } else {
                ex.printStackTrace();
            }
        }

        if (response != null) {
            //Delete All role assignments
            List<RoleAssignment> organisationalRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> ORGANISATION.equals(assignment.getRoleType()))
                .collect(toList());

            List<RoleAssignment> caseRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> CASE.equals(assignment.getRoleType()))
                .collect(toList());

            //Check if there are 'orphaned' restricted roles
            if (organisationalRoleAssignments.isEmpty() && !caseRoleAssignments.isEmpty()) {
                log.info("Orphaned Restricted role assignments were found.");
                log.info("Creating a temporary role assignment to perform cleanup");
                //Create a temporary organisational role
                setupCftOrganisationalRoleAssignment(headers, jurisdiction);
                //Recursive
                clearAllRoleAssignments(headers, jurisdiction);
            }

            caseRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );

            organisationalRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );
        }
    }

    private void postRoleAssignment(String userToken,
                                    String s2sToken,
                                    UserInfo userInfo,
                                    String roleName,
                                    String attributes,
                                    String resourceFilename) {

        try {
            roleAssignmentServiceApi.createRoleAssignment(
                getBody(userInfo, roleName, resourceFilename, attributes),
                userToken,
                s2sToken
            );
        } catch (FeignException ex) {
            ex.printStackTrace();
        }
    }

    private void postRoleAssignment(String caseId,
                                    String bearerUserToken,
                                    String s2sToken,
                                    String actorId,
                                    String roleName,
                                    String attributes,
                                    String resourceFilename,
                                    String grantType,
                                    String roleCategory,
                                    String authorisations,
                                    String roleType,
                                    String classification,
                                    String process,
                                    String reference,
                                    boolean replaceExisting,
                                    Boolean readOnly,
                                    String notes,
                                    String beginTime,
                                    String endTime,
                                    String assignerId) {

        String body = getBody(caseId, actorId, roleName, resourceFilename, attributes, grantType, roleCategory,
            authorisations, roleType, classification, process, reference, replaceExisting,
            readOnly, notes, beginTime, endTime, assignerId);

        roleAssignmentServiceApi.createRoleAssignment(
            body,
            bearerUserToken,
            s2sToken
        );
    }

    private String getBody(final UserInfo userInfo,
                           final String roleName,
                           final String resourceFilename,
                           final String attributes) {
        String assignmentRequestBody = null;
        try {
            assignmentRequestBody = FileUtils.readFileToString(ResourceUtils.getFile(
                "classpath:" + resourceFilename), "UTF-8"
            );
            assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", userInfo.getUid());
            assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", userInfo.getUid());
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_NAME_PLACEHOLDER}", roleName);
            if (attributes != null) {
                assignmentRequestBody = assignmentRequestBody.replace("\"{ATTRIBUTES_PLACEHOLDER}\"", attributes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assignmentRequestBody;
    }

    private String getBody(final String caseId,
                           String actorId,
                           final String roleName,
                           final String resourceFilename,
                           final String attributes,
                           final String grantType,
                           String roleCategory,
                           String authorisations,
                           String roleType,
                           String classification,
                           String process,
                           String reference,
                           boolean replaceExisting,
                           Boolean readOnly,
                           String notes,
                           String beginTime,
                           String endTime,
                           String assignerId) {

        String assignmentRequestBody = null;

        try {
            assignmentRequestBody = FileUtils.readFileToString(ResourceUtils.getFile(
                "classpath:" + resourceFilename), "UTF-8"
            );
            assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", actorId);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_NAME_PLACEHOLDER}", roleName);
            assignmentRequestBody = assignmentRequestBody.replace("{GRANT_TYPE}", grantType);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_CATEGORY}", roleCategory);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_TYPE}", roleType);
            assignmentRequestBody = assignmentRequestBody.replace("{CLASSIFICATION}", classification);
            assignmentRequestBody = assignmentRequestBody.replace("{PROCESS}", process);
            assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", assignerId);

            assignmentRequestBody = assignmentRequestBody.replace(
                "\"replaceExisting\": \"{REPLACE_EXISTING}\"",
                String.format("\"replaceExisting\": %s", replaceExisting)
            );

            if (beginTime != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{BEGIN_TIME_PLACEHOLDER}",
                    beginTime
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"beginTime\": \"{BEGIN_TIME_PLACEHOLDER}\"", "");
            }

            if (endTime != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{END_TIME_PLACEHOLDER}",
                    endTime
                );
            } else {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{END_TIME_PLACEHOLDER}",
                    ZonedDateTime.now().plusHours(2).format(ROLE_ASSIGNMENT_DATA_TIME_FORMATTER)
                );
            }

            if (attributes != null) {
                assignmentRequestBody = assignmentRequestBody
                    .replace("\"{ATTRIBUTES_PLACEHOLDER}\"", attributes);
            }

            if (caseId != null) {
                assignmentRequestBody = assignmentRequestBody.replace("{CASE_ID_PLACEHOLDER}", caseId);
            }

            assignmentRequestBody = assignmentRequestBody.replace("{REFERENCE}", reference);


            if (notes != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "\"notes\": \"{NOTES}\"",
                    String.format("\"notes\": [%s]", notes)
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"notes\": \"{NOTES}\"", "");
            }

            if (readOnly != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "\"readOnly\": \"{READ_ONLY}\"",
                    String.format("\"readOnly\": %s", readOnly)
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"readOnly\": \"{READ_ONLY}\"", "");
            }

            if (authorisations != null) {
                assignmentRequestBody = assignmentRequestBody.replace("\"{AUTHORISATIONS}\"", authorisations);
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"authorisations\": \"{AUTHORISATIONS}\"", "");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return assignmentRequestBody;
    }


    private String toJsonString(Map<String, String> attributes) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    private String toJsonString(List<String> attributes) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    private Set<String> getProcesses(Headers authenticationHeaders, String caseId) {
        String filter = "/?variables=" + "caseId" + "_eq_" + caseId;
        List<String> processIds = camundaApiActions.get(
            "process-instance" + filter,
            authenticationHeaders
        ).then().extract().body().path("id");

        return Set.copyOf(processIds);
    }

    private void deleteProcessInstance(Headers authenticationHeaders, String processId) {
        String deleteRequest = DELETE_REQUEST.replace("{PROCESS_ID}", processId);

        try {
            camundaApiActions.post(
                "message",
                deleteRequest,
                authenticationHeaders
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
