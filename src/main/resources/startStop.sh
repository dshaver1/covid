pkill java

unzip /mnt/data/tmp/*.zip
mv /mnt/data/tmp/covid/*.jar /mnt/data/covid
cp /mnt/data/tmp/covid/static/* /mnt/data/apache/www/html/

rm -rf /mnt/data/tmp/covid

chmod +x /mnt/data/covid/*.jar
java -Xmx1500m -Xms1500m -jar -Dspring.profiles.active=cloud /mnt/data/covid/covid-1.0.0-SNAPSHOT.jar > /dev/null 2>&1 &
tail -f /mnt/data/covid/logs/spring.log
