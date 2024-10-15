ARG APP_INSIGHTS_AGENT_VERSION=3.5.4

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/wa-case-event-handler.jar /opt/app/

EXPOSE 8088
CMD [ "wa-case-event-handler.jar" ]
