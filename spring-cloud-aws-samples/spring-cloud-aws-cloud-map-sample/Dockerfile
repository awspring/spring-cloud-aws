FROM amazoncorretto:11
ARG JAR_FILE=target/spring-cloud-aws-cloud-map-sample-2.3.1.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
