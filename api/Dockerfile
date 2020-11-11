FROM ubuntu:20.04
ENV LANG C.UTF-8
RUN apt-get update && apt-get install -yyy openjdk-14-jre tesseract-ocr curl
ENV spring_profiles_active=docker

EXPOSE 8080

ARG JAR_FILE=build/libs/api.jar

ADD ${JAR_FILE} app.jar
ADD docker/start.sh start.sh
RUN chmod u+x start.sh
RUN useradd --uid 9876 --user-group --no-create-home --no-log-init --shell /bin/bash paperspace

ENTRYPOINT ["bash", "/start.sh"]