FROM ubuntu:16.04

MAINTAINER VAN YURY

# Обвновление списка пакетов
RUN apt-get -y update

#
# Установка postgresql
#
ENV PGVER 9.5
RUN apt-get install -y postgresql-$PGVER

# Run the rest of the commands as the ``postgres`` user created by the ``postgres-$PGVER`` package when it was ``apt-get installed``
USER postgres

# Create a PostgreSQL role named ``docker`` with ``docker`` as the password and
# then create a database `docker` owned by the ``docker`` role.
RUN /etc/init.d/postgresql start && \
    psql --command "ALTER USER postgres WITH PASSWORD 'postgres';" && \
    /etc/init.d/postgresql stop

# Adjust PostgreSQL configuration so that remote connections to the
# database are possible.
RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/$PGVER/main/pg_hba.conf

# And add ``listen_addresses`` to ``/etc/postgresql/$PGVER/main/postgresql.conf``
RUN echo "listen_addresses='*'" >> /etc/postgresql/$PGVER/main/postgresql.conf

# Expose the PostgreSQL port
EXPOSE 5432

# Add VOLUMEs to allow backup of config, logs and databases
VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]


#
# Сборка проекта
#

# Установка JDK
RUN apt-get install -y openjdk-8-jdk-headless

# Установка mavev
RUN apt-get install -y maven

# Копируем исходный код в Docker-контейнер
ENV WORK /opt
ADD . $WORK/java-spring/


# Собираем и устанавливаем пакет
WORKDIR $WORK/java-spring
RUN mvn package


# Объявлем порт сервера
EXPOSE 5000

#
# Запускаем PostgreSQL и сервер
#

USER postgres

CMD service postgresql start && \
    psql --command "UPDATE pg_database SET datistemplate = FALSE WHERE datname = 'template1';" && \
    psql --command "DROP DATABASE template1;" && \
    psql --command "CREATE DATABASE template1 WITH TEMPLATE = template0 ENCODING = 'UNICODE';" && \
    psql --command "UPDATE pg_database SET datistemplate = TRUE WHERE datname = 'template1';" && \
    psql --command "\c template1" && \
    psql --command "VACUUM FREEZE;" && \
    \
     psql --command "CREATE DATABASE forumdb WITH ENCODING 'UTF8';" && \
    psql -f $WORK/java-spring/ForumDB.sql forumdb postgres && \
    java -jar $WORK/java-spring/target/DBServer-1.0-SNAPSHOT.jar
