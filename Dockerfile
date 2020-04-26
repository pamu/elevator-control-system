FROM openjdk:8

COPY ecs /work_dir/ecs
COPY build.sc /work_dir/
COPY mill /work_dir/
COPY .scalafmt.conf /work_dir/
COPY scalastyle_config.xml /work_dir/

WORKDIR /work_dir

RUN ./mill ecs.compile
RUN ./mill ecs.test