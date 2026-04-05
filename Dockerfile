FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# 의존성 레이어 캐시: 소스 변경 시 재다운로드 방지
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]