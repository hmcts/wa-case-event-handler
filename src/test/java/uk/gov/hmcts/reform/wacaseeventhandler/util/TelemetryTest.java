package uk.gov.hmcts.reform.wacaseeventhandler.util;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationIdTelemetryInitializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelemetryTest {
    public Telemetry telemetryConfiguration() {

        Telemetry telemetry = new RequestTelemetry();

        TelemetryConfiguration
            .getActive()
            .getTelemetryInitializers().stream()
            .filter(WebOperationIdTelemetryInitializer.class::isInstance)
            .findFirst()
            .ifPresent(telemetryInitializer -> telemetryInitializer.initialize(telemetry));

        return telemetry;

    }

    @Test
    void should_get_azure_operation_id() {

        Assertions
            .assertNotNull(telemetryConfiguration()
            .getContext()
            .getOperation()
                .getId());

    }

}
