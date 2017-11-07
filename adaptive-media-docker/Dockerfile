FROM liferay/portal-tomcat-mysql:master-50870e19ab8a0f536c7f0d66d3368ebefaa3a9d5
MAINTAINER Sergio González <sergio.gonzalez@liferay.com>

RUN rm $LIFERAY_HOME/osgi/modules/com.liferay.adaptive.media*.jar
COPY *.jar $LIFERAY_HOME/osgi/modules/
COPY supervisord-tomcat.conf /etc/supervisor/conf.d/supervisord-tomcat.conf
COPY portal-ext.properties /liferay/portal-ext.properties