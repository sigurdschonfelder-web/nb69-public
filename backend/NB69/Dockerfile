# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Kopiér det bygde jar-et (uansett navn) til app.jar
COPY --from=build /app/target/*.jar /app/app.jar

# Render gir oss PORT via env var; start Spring på den
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
