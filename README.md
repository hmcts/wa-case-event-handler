# wa-case-event-handler

[![Build Status](https://travis-ci.org/hmcts/wa-case-event-handler.svg?branch=master)](https://travis-ci.org/hmcts/wa-case-event-handler)

## Notes

Since Spring Boot 2.1, bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

- Prerequisite:
  - Check if all services are running in minikube, if not follow the README in
    https://github.com/hmcts/wa-kube-environment
  
- You can either run as Java Application from run configurations or
    ```bash
      ./gradlew clean bootRun
    ```
- In order to test if the application is up, you can call its health endpoint:

    ```bash
      curl http://localhost:8088/health
    ```

  You should get a response similar to this:

    ```
      {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
    ```

## Running tests

### Integration tests

There are no pre-requisitie steps to running junit and integration tests, they can be executed with the following command
```
   ./gradlew test integration
``` 

### Functional tests

#### Pre-requisites

Case Event Handler is dependent on the following services running locally (alongside minikube containers) when running
functional tests 

- WorkAllocation Services
    - [wa-workflow-api](https://github.com/hmcts/wa-workflow-api)
    - [wa-task-management-api](https://github.com/hmcts/wa-task-management-api)

- Immigration/Asylum Services
    - [ia-case-documents-api](https://github.com/hmcts/ia-case-documents-api)
    - [ia_case_api](https://github.com/hmcts/ia_case_api)
    - [ia-case-notifications-api](https://github.com/hmcts/ia-case-notifications-api)
    
Ensure the IA ccd case definitions are uploaded by installing yarn and running the script
    - checkout the [ia-ccd-definitions](https://github.com/hmcts/ia-ccd-definitions) repository
    - `cd ia-ccd-definitions`
    - `brew install yarn`
    - `yarn install`
    - `yarn upload-wa`
    
Ensure the BPMN and DMN are deployed onto Camunda locally
- These repos should be checked out and udpated to latest git revision, and respective environment variables set
 - `export WA_BPMNS_DMNS_PATH=/Users/hmcts/IdeaProjects/wa-standalone-task-bpmn`
 - `export IA_TASK_DMNS_BPMNS_PATH=/Users/hmcts/IdeaProjects/ia-task-configuration`
 - `export WA_TASK_DMNS_BPMNS_PATH=/Users/hmcts/IdeaProjects/wa-task-configuration-template`
 - These environment variables will then be used to upload the BPMN and DMNs to Camunda locally when
[setup.sh](https://github.com/hmcts/wa-kube-environment/blob/master/scripts/setup.sh) is run from `wa-kube-environment` repository

##### Azure setup

Your functional tests will require a topic and 2 subscriptions specific to you, plus a connection string to point to 
the azure asb instance on which these are hosted - you will need to create these resources
through the  Azure UI.

##### Run Case Event Handler locally

To run the case event handler locally for functional testing, the following Azure Asb environment variables are required.
This will ensure that Case Event Handler receives all messages from Azure ASB 

Either set them globally (by exporting them in a terminal window, or in your shell profile), or specify in the gradle 
command line used to run case event handler

 - `AZURE_SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://ccd-servicebus-ENV.servicebus.windows.net/;SharedAccessKeyName=SendAndListenSharedAccessKey;SharedAccessKey=sharedAccessKeyValue"`
 - `AZURE_SERVICE_BUS_TOPIC_NAME=wa-case-event-handler-topic`
 - `AZURE_SERVICE_BUS_CCD_CASE_EVENTS_SUBSCRIPTION_NAME=YOURNAME_dev_ccdcaseevent_local`
 - `AZURE_SERVICE_BUS_SUBSCRIPTION_NAME=YOURNAME__dev_local`
 - `AZURE_SERVICE_BUS_FEATURE_TOGGLE=true` 

#### Running functional tests

Functional tests publish messages to the Azure Service bus topic, so the following Azure environment 
variables are required    

 - `AZURE_SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://ccd-servicebus-ENV.servicebus.windows.net/;SharedAccessKeyName=SendAndListenSharedAccessKey;SharedAccessKey=sharedAccessKeyValue"`
 - `AZURE_SERVICE_BUS_TOPIC_NAME=wa-case-event-handler-topic`
 - `AZURE_SERVICE_BUS_FEATURE_TOGGLE=true`

All functional tests can be run using

`./gradlew cleanFunctional functional`

Specific test classes (e.g `MessageProcessorFunctionalTest`) can be run using

`./gradlew cleanFunctional functional --tests "uk.gov.hmcts.reform.wacaseeventhandler.MessageProcessorFunctionalTest"`

Specific tests within a class (e.g `MessageProcessorFunctionalTest.should_process_message_with_the_lowest_event_timestamp_for_that_case`) can be run using

`./gradlew cleanFunctional functional --tests "uk.gov.hmcts.reform.wacaseeventhandler.MessageProcessorFunctionalTest.should_process_message_with_the_lowest_event_timestamp_for_that_case"`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details


