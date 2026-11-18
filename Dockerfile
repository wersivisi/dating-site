FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sed -i 's/\r$//' mvnw && sh mvnw -B package -Pportable -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/swipely-portable.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV APP_TEST_DATA_ENABLED=false
ENV APP_PORTABLE_ENABLED=false
ENV SERVER_FORWARD_HEADERS_STRATEGY=framework
EXPOSE 10000
CMD ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-10000}"]
