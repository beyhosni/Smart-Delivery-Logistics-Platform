FROM openjdk:23-jdk-slim
WORKDIR /app
COPY target/gateway-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
