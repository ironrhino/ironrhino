FROM java:8u111-jdk-alpine

#ENV SPRING_PROFILES_DEFAULT=dual

RUN mkdir /home/ironrhino

RUN adduser -D -h /home/ironrhino ironrhino

#VOLUME ["/home/ironrhino/$app/conf"]

#docker run --volume ~/$app/conf:/home/ironrhino/$app/conf --name $app $app

WORKDIR /home/ironrhino

ADD target/ROOT.war ROOT.war

RUN chown -R ironrhino:ironrhino /home/ironrhino

USER ironrhino

EXPOSE 8080

ENTRYPOINT ["java","-jar","ROOT.war"]