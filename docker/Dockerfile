FROM maven:3.8.6-openjdk-8-slim
RUN apt-get clean
RUN apt-get update
RUN mkdir -p /app
COPY target/pagefeedback-cj-1.0.0-SNAPSHOT.jar /app/app.jar
ENV JAVA_OPTS="-Xmx2g"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
