FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY src ./src
COPY pom.xml .
RUN mvn -q -DskipTests package
COPY web ./web

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/game-server.jar ./game-server.jar
COPY --from=build /app/web ./web
COPY Screenshot_2026-11-25_at_6.07.46_PM-removebg-preview.png ./
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "game-server.jar"]
