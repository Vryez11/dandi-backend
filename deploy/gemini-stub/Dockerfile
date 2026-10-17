# syntax=docker/dockerfile:1
# 부하 테스트 환경에서 Gemini 대신 응답하는 Stub 서버.
# 실제 Gemini는 비용·요청량 한도·응답 편차 때문에 before/after 비교가 흔들려, 지연만 흉내 낸다.
FROM wiremock/wiremock:3.13.1

# 응답과 지연 분포가 이미지에 들어가므로 "이미지 태그 = 분포 버전"이 된다.
# 측정 기록에는 어떤 태그로 쟀는지를 남긴다(before/after는 같은 태그로).
COPY mappings /home/wiremock/mappings

# --no-request-journal: 요청 본문(이미지 base64, 수백 KB)을 메모리에 쌓지 않는다. 켜 두면 부하 중 메모리가 샌다.
# --async-response-enabled: 지연 동안 요청 스레드를 잡지 않아 Stub 자신이 병목이 되지 않게 한다.
CMD ["--port", "8080", \
     "--no-request-journal", \
     "--async-response-enabled", "true", \
     "--async-response-threads", "50", \
     "--container-threads", "100", \
     "--disable-banner"]
