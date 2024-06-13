# wa-case-event-handler

[![Build Status](https://travis-ci.org/hmcts/wa-case-event-handler.svg?branch=master)](https://travis-ci.org/hmcts/wa-case-event-handler)

## Notes

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

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
  - Check if minikube IP is set as environment variable.
      ```
      echo $OPEN_ID_IDAM_URL
      ```
    You should see the ip and port as output, eg: http://192.168.64.14:30196.
    If you do not see, then from your wa-kube-enviroment map environment variables
      ```
      source .env
      ```
- You can either run as Java Application from run configurations or
    ```bash
      ./gradlew clean bootRun
    ```
- In order to test if the application is up, you can call its health endpoint:

    ```bash
      curl http://localhost:8090/health
    ```

  You should get a response similar to this:

    ```
      {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
    ```

- To run all functional tests or single test you can run as Junit, make sure the env is set
    ```
        OPEN_ID_IDAM_URL=http://'minikubeIP:port'
    ```
  Note: Make sure the BPMN and DMN are deployed onto Camunda locally. Workflow and Task Configuration services should be running

- To run all tests including junit, integration and functional.
  NOTE: This service is dependent on wa-workflow-api and wa-task-configuration service , so make sure it is running locally when running FTs.

  You can run the command
   ```
       ./gradlew test integration functional
   ```
  or
  ```
  ./gradlew tests
  ```

### Configuration for functional test
- To run functional tests, application should connect to the ASB. Make sure you have ASB subscription and provide
  correct values for these environment variables before you start the application.
  ```
  export AZURE_SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://ccd-servicebus-demo.servicebus.windows.net/;SharedAccessKeyName=SendAndRecieveCCDMessage;SharedAccessKey=<Access_Key>;EntityPath=wa-case-event-handler-topic-sessions-ft"
  export AZURE_SERVICE_BUS_TOPIC_NAME=wa-case-event-handler-topic-sessions-ft
  export AZURE_SERVICE_BUS_CCD_CASE_EVENTS_SUBSCRIPTION_NAME=<subscription_name>
  export AZURE_SERVICE_BUS_MESSAGE_AUTHOR=<author_name>
  ```

- To run application please provide these values
  ```
  export AZURE_SERVICE_BUS_DLQ_FEATURE_TOGGLE=true
  ```
- To run functional tests, make sure Application is up and running.
- please provide these values
- ```
  export AZURE_SERVICE_BUS_DLQ_FEATURE_TOGGLE=false
  ```
- Functional tests send messages to CaseEventHandlerTestingController, we are not using ASB for messaging.
  However, MessageReadinessConsumer peek into the ASB DLQ to make sure DLQ is empty before setting any message READY to
  be processed. So we still need to connect the application to the ASB.
  Functional test context doesn't need any of the ASB configuration as tests do not connect to the ASB

- Example command to run Application
  ```
  AZURE_SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://ccd-servicebus-demo.servicebus.windows.net/;SharedAccessKeyName=SendAndRecieveCCDMessage;SharedAccessKey=<Access_Key> \
  AZURE_SERVICE_BUS_DLQ_FEATURE_TOGGLE=true \
  ./gradlew clean bootrun
  ```
- Example command to run Functional Tests
  ```
  AZURE_SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://ccd-servicebus-demo.servicebus.windows.net/;SharedAccessKeyName=SendAndRecieveCCDMessage;SharedAccessKey=<Access_Key> \
  AZURE_SERVICE_BUS_DLQ_FEATURE_TOGGLE=false \
  ./gradlew clean functional
  ```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details


