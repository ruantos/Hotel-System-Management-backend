FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+UseSerialGC"

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
