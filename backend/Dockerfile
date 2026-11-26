FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /source
COPY pom.xml .
COPY src/main src/main
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/*.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","app.jar"]
