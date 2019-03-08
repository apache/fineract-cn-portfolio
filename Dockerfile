FROM openjdk:8-jdk-alpine

ARG portfolio_port=2026

ENV server.max-http-header-size=16384 \
    cassandra.clusterName="Test Cluster" \
    portfolio.bookLateFeesAndInterestAsUser=imhotep \
    server.port=$portfolio_port

WORKDIR /tmp
COPY portfolio-service-boot-0.1.0-BUILD-SNAPSHOT.jar .

CMD ["java", "-jar", "portfolio-service-boot-0.1.0-BUILD-SNAPSHOT.jar"]
