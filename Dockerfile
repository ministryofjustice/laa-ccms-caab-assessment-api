FROM ghcr.io/ministryofjustice/laa-base-container-images/java:25-corretto-alpine

COPY assessment-service/build/libs/assessment-service-*.jar /app/assessment-service.jar

USER 1001:1001
EXPOSE 8080
CMD ["java", "-jar", "/app/assessment-service.jar"]
