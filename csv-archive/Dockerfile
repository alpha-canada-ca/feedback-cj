FROM bash:4.4
WORKDIR /opt/app
RUN adduser -h /opt/app -S -D myuser
RUN apk add --no-cache --update mongodb-tools coreutils
COPY ./csv-archive.sh .
USER myuser
ENTRYPOINT ["bash", "csv-archive.sh"]