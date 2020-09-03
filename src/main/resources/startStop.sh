sudo pkill java

sudo unzip /mnt/data/tmp/*.zip -d /mnt/data/tmp/
sudo mv /mnt/data/tmp/covid/*.jar /mnt/data/covid
sudo cp -r /mnt/data/tmp/covid/static/* /mnt/data/apache/www/html/

sudo rm -rf /mnt/data/tmp/covid

sudo chmod +x /mnt/data/covid/*.jar
sudo su - covid -c java -Xmx1500m -Xms1500m -jar -Dspring.profiles.active=cloud /mnt/data/covid/covid-1.0.0-SNAPSHOT.jar > /dev/null 2>&1 &
tail -f /mnt/data/covid/logs/spring.log
