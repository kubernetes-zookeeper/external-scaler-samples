FROM amazoncorretto:8 AS build

COPY . /opt/external-scaler-server/

WORKDIR /opt/external-scaler-server

RUN ./gradlew clean installDist --no-daemon

FROM amazoncorretto:8

COPY --from=build /opt/external-scaler-server/build/install/external-scaler-grpc/bin/external-scaler-server /opt/external-scaler-server/bin/

COPY --from=build /opt/external-scaler-server/build/install/external-scaler-grpc/lib/*.jar /opt/external-scaler-server/lib/

EXPOSE 50051/tcp

CMD ["/opt/external-scaler-server/bin/external-scaler-server"]
