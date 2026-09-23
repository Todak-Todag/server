FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE

RUN apk add --no-cache curl

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 19005

ENTRYPOINT ["java", "-jar", "app.jar"]