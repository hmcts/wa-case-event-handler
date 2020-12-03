package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CamundaClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    @Autowired
    private CamundaClient camundaClient;

    @Override
    public boolean canHandle() {
        Map<String, DmnValue> variables = new HashMap<>();
        DmnStringValue dmnStringValue = new DmnStringValue("submitAppeal");
        variables.put("eventId", dmnStringValue);

        EvaluateDmnRequest requestParameters = new EvaluateDmnRequest(variables);

        camundaClient.getTask("Bearer", "df", "df", requestParameters);
        return false;
    }

    @Override
    public void handle() {

    }
}
