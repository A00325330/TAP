FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/container-manager-0.0.1-SNAPSHOT.jar /app/container-manager.jar
EXPOSE 8081
CMD ["java", "-jar", "container-manager.jar"]
