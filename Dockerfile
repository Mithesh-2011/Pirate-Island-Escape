FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p out && javac -d out src/*.java
COPY web ./web

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out", "Main"]
