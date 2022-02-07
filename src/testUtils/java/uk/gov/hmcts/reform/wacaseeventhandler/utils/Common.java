package uk.gov.hmcts.reform.wacaseeventhandler.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.config.GivensBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.config.RestApiActions;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.RoleAssignment;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.RoleAssignmentResource;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdamService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.RoleAssignmentServiceApi;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleType.CASE;
import static uk.gov.hmcts.reform.wacaseeventhandler.entities.enums.RoleType.ORGANISATION;

@Slf4j
public class Common {

    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
    private final AuthorizationProvider authorizationProvider;
    private final IdamService idamService;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Common(GivensBuilder given,
                  RestApiActions camundaApiActions,
                  AuthorizationProvider authorizationProvider,
                  IdamService idamService,
                  RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.given = given;
        this.camundaApiActions = camundaApiActions;
        this.authorizationProvider = authorizationProvider;
        this.idamService = idamService;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }


    public TestVariables createCase() {

        String caseId = given.createCcdCase();

        return new TestVariables(caseId, null, null);
    }

    public void setupCftOrganisationalRoleAssignment(Headers headers) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", "IA"
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

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

    public void clearAllRoleAssignments(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
    }

    public void cleanUpTask(Headers authenticationHeaders, List<String> caseIds) {

        Set<String> processIds = new HashSet<>();

        caseIds
            .forEach(caseId -> processIds.addAll(getProcesses(authenticationHeaders, caseId)));

        processIds
            .forEach(processId -> deleteProcessInstance(authenticationHeaders, processId));

    }

    //todo: check here
    private void clearAllRoleAssignmentsForUser(String userId, Headers headers) {
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
                setupCftOrganisationalRoleAssignment(headers);
                //Recursive
                clearAllRoleAssignments(headers);
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

    private String toJsonString(Map<String, String> attributes) {
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
