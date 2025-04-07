# Use an official Maven image with OpenJDK 21 for building
FROM maven AS build

# Set the working directory
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the final image with OpenJDK 21
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app
# Copy the jar file from the build stage

COPY --from=build /app/target/productservice-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 9090
	
# Run the application
ENTRYPOINT ["java","-jar","app.jar"]