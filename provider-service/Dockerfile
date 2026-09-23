FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

ARG JAR_FILE

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
