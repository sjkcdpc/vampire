FROM gaosi-gaosiedu.customerindex.alauda.cn:5000/tomcat:latest
ADD ./sed.sh /sed.sh
RUN chmod +x /sed.sh
ADD target/vampire /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
ENTRYPOINT ["/sed.sh"]
CMD ["catalina.sh","run"]