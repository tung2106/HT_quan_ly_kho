FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
# Chạy unit test khi đóng gói; test PostgreSQL được chạy riêng trước triển khai.
RUN mvn -B -ntp package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -Duser.timezone=Asia/Ho_Chi_Minh"
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
