FROM amazoncorretto:11
ARG JAR_FILE=target/spring-cloud-aws-cloud-map-sample-3.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
