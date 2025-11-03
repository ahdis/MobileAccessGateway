## ############################# ##
## FIRST STAGE: THE BUILD SYSTEM
## ############################# ##
FROM maven:3-amazoncorretto-25 AS builder

# Prepare the system
RUN yum install -y binutils
COPY pom.xml .
COPY src ./src

# Build small JRE image
RUN ["jlink", \
      "--compress=2", \
      "--add-modules", "java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.jfr,jdk.management,jdk.unsupported", \
      "--no-header-files", \
      "--no-man-pages", \
      "--strip-debug", \
      "--verbose", \
      "--output", "/mag-jre"]

# Build the Java app
RUN mvn --batch-mode -DskipTests -DfinalName=mag clean package


## ############################# ##
## SECOND STAGE: THE FINAL IMAGE
## ############################# ##
FROM bellsoft/alpaquita-linux-base:stream-glibc
COPY --from=builder /mag-jre /opt/jdk

ENV PATH=$PATH:/opt/jdk/bin
EXPOSE 9090/tcp
LABEL org.opencontainers.image.url="https://github.com/ahdis/MobileAccessGateway"
LABEL org.opencontainers.image.source="https://github.com/ahdis/MobileAccessGateway"
LABEL org.opencontainers.image.title="MobileAccessGateway"
LABEL org.opencontainers.image.vendor="ahdis ag"
LABEL org.opencontainers.image.documentation="https://ahdis.github.io/MobileAccessGateway/"

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'
RUN apk add libstdc++ && apk --purge del

RUN addgroup -S mag && adduser -S mag -G mag
USER mag:mag

# Use our own folder
RUN mkdir -p /home/mag
WORKDIR /home/mag
ENV HOME=/home/mag
COPY --from=builder target/mag.jar .

ENTRYPOINT [ \
  "java", \
  "-Xmx1G", \
  "-jar", "mag.jar", \
  "-Dspring.config.additional-location=file:/home/mag/config/application.yml" \
]

# To publish a version manually:
# docker buildx build --tag "europe-west6-docker.pkg.dev/ahdis-ch/ahdis/mag-cara:v2.0.4" --push --platform=linux/amd64 -f Dockerfile .