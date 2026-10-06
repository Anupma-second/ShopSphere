# ---------- 1. Build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# download dependencies first (cached until pom.xml changes)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- 2. Run it on a small Java runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# don't run as root
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring

# tuned for small free-tier containers (512 MB RAM)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]