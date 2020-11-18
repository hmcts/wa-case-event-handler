ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.2

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/wa-case-event-handler.jar /opt/app/

EXPOSE 8088
CMD [ "wa-case-event-handler.jar" ]
