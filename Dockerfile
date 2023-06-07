FROM openjdk:17-jdk-slim-buster
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} BatchApplication.jar
ENTRYPOINT ["java","-jar","/BatchApplication.jar"]