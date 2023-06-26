FROM public.ecr.aws/amazoncorretto/amazoncorretto:11 AS build

ENV TOMCAT_MAJOR=10

ENV TOMCAT_VERSION=10.1.9

ENV CATALINA_HOME=/usr/local/tomcat

ENV PATH=/usr/local/tomcat/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

RUN curl -fL -o "apache-tomcat.zip" https://dlcdn.apache.org/tomcat/tomcat-$TOMCAT_MAJOR/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.zip

RUN python -m zipfile -e apache-tomcat.zip /usr/local/

RUN /bin/mv /usr/local/apache-tomcat-${TOMCAT_VERSION} /usr/local/tomcat

COPY . /opt/external-scaler-web/

COPY ./bin ./lib ./init.d /opt/external-scaler-web/

WORKDIR /opt/external-scaler-web

RUN ./bin/gradle clean build --no-daemon
 
RUN /bin/mv /opt/external-scaler-web/build/libs/external-scaler-web.war /usr/local/tomcat/webapps/external-scaler-web.war

RUN chmod a+x /usr/local/tomcat/bin/*.sh

FROM public.ecr.aws/amazoncorretto/amazoncorretto:11
 
COPY --from=build /usr/local/tomcat /usr/local/tomcat

EXPOSE 8080/tcp

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
