package uk.gov.hmcts.reform.wacaseeventhandler.clients;

public interface FeatureToggler {

    boolean getValue(String key, Boolean defaultValue);

}
