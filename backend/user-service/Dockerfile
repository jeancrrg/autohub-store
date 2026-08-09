FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

COPY target/user-service.jar app.jar

EXPOSE 8002

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
