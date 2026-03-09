FROM eclipse-temurin:17-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates bash \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://github.com/sbt/sbt/releases/download/v1.10.5/sbt-1.10.5.tgz -o /tmp/sbt.tgz \
    && tar -xzf /tmp/sbt.tgz -C /opt \
    && ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt \
    && rm /tmp/sbt.tgz

WORKDIR /app

COPY build.sbt ./
COPY project ./project
RUN sbt update

COPY src ./src

EXPOSE 8080

CMD ["sbt", "run"]
