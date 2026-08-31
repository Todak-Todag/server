FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 19005

ENTRYPOINT ["java", "-jar", "app.jar"]