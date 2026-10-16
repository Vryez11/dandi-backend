# syntax=docker/dockerfile:1
# Jansori(https://github.com/charsyam/jansori): prod RDS 쿼리 호출 수·실행 계획 조회 도구.
# 소스를 레포에 두지 않고 커밋을 고정해 받는다. 올릴 때는 JANSORI_COMMIT만 바꾼다.
FROM rust:1.94-slim-bookworm AS build

ARG JANSORI_COMMIT=eb6a64951d0ca91c34410e243ed7a30bbf8762d9

RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /src
RUN git init -q . \
 && git remote add origin https://github.com/charsyam/jansori.git \
 && git fetch -q --depth 1 origin "$JANSORI_COMMIT" \
 && git checkout -q FETCH_HEAD

# 웹 화면(web/index.html)은 include_str!로 바이너리에 들어간다.
RUN cargo build --release --locked

FROM debian:bookworm-slim

# TLS는 rustls라 OpenSSL은 필요 없고, RDS 인증서 검증용 루트 인증서만 둔다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --no-create-home jansori

COPY --from=build /src/target/release/jansori /usr/local/bin/jansori

USER jansori

# 컨테이너 안에서는 모든 인터페이스로 받고, 노출 범위는 호스트 포트 매핑(127.0.0.1)으로 제한한다.
# 인증이 없고 원본 SQL이 보이므로 ALB·Nginx에 연결하지 않는다.
ENV JANSORI_BIND=0.0.0.0:8080
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/jansori"]
