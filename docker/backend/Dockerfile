FROM openjdk:17-slim

WORKDIR /app

COPY paper-revision-backend/target/*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
