FROM openjdk:8
COPY ./build/libs/zenodo-submit-*.jar /usr/src/app.jar
WORKDIR /usr/src/
ENTRYPOINT ["java", "-jar" , "app.jar"]