FROM maven:3.8.1-openjdk-8
RUN apt-get clean
RUN apt-get update
RUN mkdir -p /app
ADD target/pagefeedback-cj-1.0.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
