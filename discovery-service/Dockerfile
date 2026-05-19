FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/discovery-service-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
