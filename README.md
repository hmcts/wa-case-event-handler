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
  Note: Make sure the BPMN and DMN are deployed onto Camunda locally.

- To run all tests including junit, integration and functional.
  NOTE: This service is dependant on wa-workflow-api service , so make sure it is running locally.

  You can run the command
   ```
       ./gradlew test integration functional
   ```
  or
  ```
  ./gradlew tests
  ```
## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details


