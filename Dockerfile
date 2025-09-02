FROM bellsoft/liberica-openjdk-alpine:latest
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 9090
VOLUME /tmp

ARG JAR_FILE=target/mobile-access-gateway-2.0.0-spring-boot.jar

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY ${JAR_FILE} /app.jar

ENTRYPOINT java -Xmx1G -jar /app.jar -Djavax.net.ssl.trustStore=cacerts -Djavax.net.ssl.trustStorePassword=changeit -Dspring.config.additional-location=optional:file:/config/application.yml


# export PROJECT_ID="$(gcloud config get-value project -q)"
# docker build -t eu.gcr.io/${PROJECT_ID}/mag:v016 .
# docker push eu.gcr.io/${PROJECT_ID}/mag:v016
# docker run -d --name mag  -p 9090:9090 --memory="5G" --cpus="1" eu.gcr.io/fhir-ch/mag:v016

# docker buildx build --tag "europe-west6-docker.pkg.dev/ahdis-ch/ahdis/mag-cara:v0.1.0" --push --platform=linux/amd64 -f Dockerfile .