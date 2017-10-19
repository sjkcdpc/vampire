#!/bin/sh
set -e

#zook=`env | grep zoo | grep 2181 | grep tcp | awk -F'/' '{print $NF}'`
#sed -i 's#zookeeper.aixuexi.com:2181#'${zook}'#g' /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/config.properties
#add host
echo "10.80.85.15 mysql.aixuexi.com" >> /etc/hosts
echo "10.45.138.45 redis.cluster.aixuexi.com" >> /etc/hosts
echo "10.45.138.45 redis.aixuexi.com" >> /etc/hosts
echo "10.45.138.45 ehcache.aixuexi.com" >> /etc/hosts
echo "10.45.138.45 mongo.aixuexi.com" >> /etc/hosts
echo "10.45.138.45 memcached.aixuexi.com" >> /etc/hosts
exec "$@"
