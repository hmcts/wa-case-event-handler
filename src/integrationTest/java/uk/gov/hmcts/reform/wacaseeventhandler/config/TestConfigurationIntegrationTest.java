package uk.gov.hmcts.reform.wacaseeventhandler.config;

public class TestConfigurationIntegrationTest {

    private TestConfigurationIntegrationTest() {
    }

    /**
     * Maximum time in seconds to wait for Assertions to all pass.
     */
    public static final Integer MAX_WAIT = 60;

    /**
     * Time in seconds to poll the system to repeat the Assertions.
     */
    public static final Integer POLL_INT = 1;

}
