FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY build/libs/*.jar app.jar
COPY models/ /app/models/
RUN mkdir -p /vault
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-Xmx2g", \
    "-Dspring.profiles.active=docker", "-jar", "app.jar"]
