FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/user-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
