# 1단계: 애플리케이션 빌드
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY . .
RUN ./gradlew bootJar -x test --no-daemon


# 2단계: RDS 인증서 준비
FROM eclipse-temurin:25-jdk AS certs

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl openssl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 기본 Java 인증서 저장소를 복사
RUN cp "$JAVA_HOME/lib/security/cacerts" /tmp/rds-cacerts

# AWS 공식 서울 리전 인증서 번들 다운로드
RUN curl --fail --show-error --silent \
    https://truststore.pki.rds.amazonaws.com/ap-northeast-2/ap-northeast-2-bundle.pem \
    -o /tmp/rds-bundle.pem

# PEM 번들에서 서울 리전 RSA2048 G1 루트 CA만 선택해 등록
RUN awk '/-----BEGIN CERTIFICATE-----/ {n++} n > 0 {print > ("/tmp/rds-" n ".pem")}' \
      /tmp/rds-bundle.pem \
    && count=0 \
    && for cert in /tmp/rds-*.pem; do \
         if openssl x509 -in "$cert" -noout -subject \
             | grep -q 'Amazon RDS ap-northeast-2 Root CA RSA2048 G1'; then \
           keytool -importcert -noprompt -trustcacerts \
             -alias nyummy-rds-ca \
             -file "$cert" \
             -keystore /tmp/rds-cacerts \
             -storepass changeit \
           && count=$((count + 1)); \
         fi; \
       done \
    && test "$count" -eq 1 \
    && keytool -list -keystore /tmp/rds-cacerts \
         -storepass changeit -alias nyummy-rds-ca


# 3단계: 실행
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN adduser --system --group spring

COPY --from=build /app/build/libs/app.jar app.jar

# 기존 Java CA 목록을 보존하면서 RDS CA를 추가한 저장소
COPY --from=certs /tmp/rds-cacerts /app/rds-cacerts

RUN chown spring:spring app.jar rds-cacerts

USER spring

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djavax.net.ssl.trustStore=/app/rds-cacerts", \
    "-Djavax.net.ssl.trustStorePassword=changeit", \
    "-jar", "app.jar"]