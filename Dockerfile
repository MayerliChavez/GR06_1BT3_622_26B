FROM maven:3.9.11-eclipse-temurin-21

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mkdir -p /app/data \
    && mvn -B -DskipTests package

EXPOSE 8080

CMD ["mvn", "-B", "-Dpomora.host=0.0.0.0", "-Dpomora.port=8080", "jetty:run"]
