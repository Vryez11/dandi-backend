# 1단계: 빌드
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 의존성 캐싱
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 전체 소스 복사 후 빌드
COPY . .
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN adduser --system --group spring

COPY --from=build /app/build/libs/app.jar app.jar
RUN chown spring:spring app.jar

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]