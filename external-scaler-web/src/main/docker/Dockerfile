FROM tomcat:10.0.23-jdk11-corretto-al2 AS build

COPY . /opt/external-scaler-web/

WORKDIR /opt/external-scaler-web

RUN ./gradlew clean build --no-daemon

FROM tomcat:10.0.23-jdk11-corretto-al2

COPY  --from=build /opt/external-scaler-web/build/libs/external-scaler-web.war /usr/local/tomcat/webapps/external-scaler-web.war

EXPOSE 8080/tcp
