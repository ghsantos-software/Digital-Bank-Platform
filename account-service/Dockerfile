FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/account-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
