FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy everything (except what .dockerignore excludes)
COPY . .

# Give execute permission to mvnw (IMPORTANT)
RUN chmod +x mvnw

# Build the jar inside the container
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

# Use the exact jar name (NO wildcard)
CMD ["java", "-jar", "target/generation-0.0.1-SNAPSHOT.jar"]
