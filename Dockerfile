FROM bde2020/spark-submit:3.0.1-hadoop3.2

LABEL maintainer="Rezwanul"

ARG SBT_VERSION
ENV SBT_VERSION=${SBT_VERSION:-1.4.1}

RUN wget -O - https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | gunzip | tar -x -C /usr/local

ENV PATH /usr/local/sbt/bin:${PATH}

WORKDIR /app

# Pre-install base libraries
ADD build.sbt /app/
ADD project/plugins.sbt /app/project/
RUN sbt update

COPY template.sh /

ENV SPARK_APPLICATION_MAIN_CLASS com.marvel.MarvelApp

# Copy the build.sbt first, for separate dependency resolving and downloading
COPY build.sbt /app/
COPY project /app/project
RUN sbt update

# Copy the source code and build the application
COPY . /app
RUN sbt clean assembly

CMD ["/template.sh"]
