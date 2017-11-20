#!/bin/sh
set -e

#zook=`env | grep zoo | grep 2181 | grep tcp | awk -F'/' '{print $NF}'`
#sed -i 's#zookeeper.aixuexi.com:2181#'${zook}'#g' /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/config.properties
#add host
echo "${IP} mysql.aixuexi.com" >> /etc/hosts
echo "${IP} redis.cluster.aixuexi.com" >> /etc/hosts
echo "${IP} redis.aixuexi.com" >> /etc/hosts
echo "${IP} ehcache.aixuexi.com" >> /etc/hosts
echo "${IP} mongo.aixuexi.com" >> /etc/hosts
echo "${IP} memcached.aixuexi.com" >> /etc/hosts
exec "$@"
