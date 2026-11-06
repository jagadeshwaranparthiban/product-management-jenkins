#BUILD

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests package \
    && cp target/*.jar /workspace/app.jar

#RUNTIME

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/app.jar app.jar

# Set ownership
RUN chown appuser:appuser app.jar

# Run as non-root user
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
