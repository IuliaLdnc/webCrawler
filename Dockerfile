FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/MySearchEngine-1.0-SNAPSHOT.jar app.jar
COPY libs/ libs/
COPY resources /app/resources
COPY application.yml /app/application.yml
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

