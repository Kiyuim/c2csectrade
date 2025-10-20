# Runtime-only Dockerfile: copy prebuilt jar and run with wait-for-deps

# Accept JDK version as a build argument (default 17)
ARG JDK_VERSION=17

# Runtime stage
FROM openjdk:${JDK_VERSION}-jdk-slim
ARG JDK_VERSION
WORKDIR /app

# Install curl, netcat and font libraries for captcha generation
RUN apt-get update && \
    apt-get install -y curl netcat-openbsd fontconfig libfreetype6 && \
    rm -rf /var/lib/apt/lists/*

# Copy the built jar from local target directory
COPY target/c2c-platform-0.0.1-SNAPSHOT.jar app.jar

# Copy wait script
COPY wait-for-deps.sh /wait-for-deps.sh
RUN chmod +x /wait-for-deps.sh

EXPOSE 8080
ENTRYPOINT ["/wait-for-deps.sh"]
