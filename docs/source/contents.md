### Installation
#### Docker

For a dockerized installation please follow the [README](https://gitlab.com/dedicatedcode/paperspace#deployment) and one of the examples in
[examples](https://gitlab.com/dedicatedcode/paperspace/-/tree/master/deployment/examples)

#### Bare Metal Installation from Source
This installation assumes a system running Ubuntu 20.04 LTS. For other systems please adapt accordingly.

##### prerequisites
You need following applications installed on the host.
* git
* openjdk-11
* tesseract
* tesseract language package
* solr  

For successful ocr you have to choose in which language your documents are. The next steps assume german. Replace 
the language code *deu* with your language code.

To install tesseract-ocr, tesseract language file, git and openjdk execute:
```
sudo apt-get install tesseract-ocr tesseract-ocr-deu openjdk-11-jdk-headless git
```
######  clone repository
```
git clone https://gitlab.com/dedicatedcode/paperspace.git
```
###### Install and configure Solr

```shell script
cd /opt
sudo wget https://archive.apache.org/dist/lucene/solr/8.3.1/solr-8.3.1.tgz
sudo tar xzf solr-8.3.1.tgz solr-8.3.1/bin/install_solr_service.sh --strip-components=2
sudo bash ./install_solr_service.sh solr-8.3.1.tgz
```
create solr data directory referenced in search/config/conf/core.properties
You can change this to your preferred place.
```shell script
sudo mkdir -p /data/solr/
sudo chown solr:solr /data/solr
```
copy solr configuration 
```shell script
sudo cp -r paperspace/search/config/conf /var/solr/data/core_documents
```
restart solr
```shell script
sudo systemctl restart solr.service
```

###### install app 
1. create user for app
    ```shell script
    sudo useradd -M -s /bin/false paperspace
    ```
2. create installation folders
    ```shell script
    sudo mkdir /opt/paperspace-app
    ```
3. build api
    ```shell script
    cd <checkout-dir>
    cd api && mvn package && cd ..   
    ```
4. copy executables
   ```shell script
   sudo cp api/target/api.jar /opt/paperspace-app/app.jar
   ```
5. copy and adjust application.properties
    ```shell script
    sudo cp api/src/main/resources/application-docker.properties /opt/paperspace-app/application.properties
   
    sudo chown -R paperspace:paperspace /opt/paperspace-app
    ```
   After this you should have the opt folder populatet like this:
   ```shell script
    paperspace-app
    ├── api.jar
    └── application.properties
    ```
6. adjust paths, language and api host in feeder
    
    For example replace the key *feeder.api.host=${API_URL:http://api:8080}* with the actual address where the api 
    is reachable from the feeder. Let´s assume the api run on localhost:8080 then this would become *feeder.api.host=http://localhost:8080*.
    The important parts here are the api host, and the paths where the feeder can find new documents and where to put them afterwards. 
    Make sure all folders you put in here are actually owned or at least writable by the user you created in step  1. 
    You can find an explanation of every key in the [README](https://gitlab.com/dedicatedcode/paperspace#docker-configuration). 
    ```shell script
    sudo nano /opt/paperspace-feeder/application.properties
    ```
    Here are my versions of the application.properties 
    
    **/opt/paperspace-feeder/application.properties**
    ```properties
   feeder.api.host=http://localhost:8080
   feeder.documents.input=${DOCUMENT_INPUT:/data/input/documents}
   feeder.documents.ignored=${DOCUMENT_IGNORED:/data/ignored/documents}
   feeder.documents.error=${DOCUMENT_ERROR:/data/error/documents}
   feeder.documents.processed=${DOCUMENT_PROCESSED:/data/processed/documents}
   feeder.documents.moveToProcessed=${DOCUMENT_BACKUP:false}
   feeder.com.dedicatedcode.paperspace.feeder.tasks.input=${TASK_INPUT:/data/input/com.dedicatedcode.paperspace.feeder.tasks}
   feeder.com.dedicatedcode.paperspace.feeder.tasks.ignored=${TASK_IGNORED:/data/ignored/com.dedicatedcode.paperspace.feeder.tasks}
   feeder.com.dedicatedcode.paperspace.feeder.tasks.error=${TASK_ERROR:/data/error/com.dedicatedcode.paperspace.feeder.tasks}
   feeder.com.dedicatedcode.paperspace.feeder.tasks.processed=${TASK_PROCESSED:/data/processed/com.dedicatedcode.paperspace.feeder.tasks}
   feeder.com.dedicatedcode.paperspace.feeder.tasks.moveToProcessed=${TASK_BACKUP:false}
   feeder.ocr.datapath=/usr/share/tesseract-ocr/4.00/tessdata
   feeder.ocr.language=${OCR_LANGUAGE:deu}
    ```
    **/opt/paperspace-app/application.properties**
   ```properties
   spring.thymeleaf.cache=true
   app.host=http://192.168.122.103:8080
   database.host=localhost
   database.port=3306
   database.table=paperspace
   database.user=paperspace
   database.password=paperspace
   search.host=localhost
   search.port=8983
   spring.datasource.url=jdbc:mariadb://${database.host}:${database.port}/${database.table}?useSSL=false&useUnicode=true&characterEncoding=utf-8
   spring.datasource.username=${database.user}
   spring.datasource.password=${database.password}
   spring.data.solr.host=http://${search.host}:${search.port}/solr/documents
   storage.local.binary=${STORAGE_PATH:/binary}
   task.defaultDuePeriod=14
   email.enabled=${ENABLE_MAIL:false}
   email.target-address=${MAIL_TO_ADDRESS:}
   email.sender-address=${MAIL_FROM_ADDRESS:}
   email.attach_documents=${MAIL_ATTACH_DOCUMENTS:false}
   spring.mail.host=${MAILING_HOST:}
   spring.mail.port=${MAILING_PORT:587}
   spring.mail.protocol=${MAILING_PROTOCOL:smtp}
   spring.mail.test-connection=false
   spring.mail.properties.mail.smtp.auth=${MAILING_SMTP_AUTH:true}
   spring.mail.properties.mail.smtp.starttls.enable=${MAILING_SMTP_USE_STARTTLS:true}
   spring.mail.username=${MAILING_USERNAME:}
   spring.mail.password=${MAILING_PASSWORD:}
   ```
7. adjust config for api
    ```shell script
    sudo nano /opt/paperspace-app/application.properties
    ```
   An explanation of every key can also be found in the [README](https://gitlab.com/dedicatedcode/paperspace#docker-configuration).
   If you enable emailing please make sure to set the properties starting with mailing.
   
7. create folders app

   If you have adjusted the paths in the application.properties you have to make sure all paths are existing and writable to the feeder and the application. You can execute the following command to create the default paths.
   ```shell script
   sudo mkdir -p /binary
   sudo chown -R paperspace:paperspace /binary
   ```

8. create service files
   ```shell script
   sudo tee <<EOF /etc/systemd/system/paperspace-app.service >/dev/null
   [Unit]
   Description=paperspace-app
   After=syslog.target
   
   [Service]
   User=paperspace
   ExecStart=/opt/paperspace-app/app.jar
   SuccessExitStatus=143
    
   [Install]
   WantedBy=multi-user.target
   EOF
    ```
   
   reload systemd configurations
   ```shell script
   sudo systemctl daemon-reload
   ```

9. start services
   ```shell script
    sudo systemctl enable paperspace-app 
    sudo systemctl start paperspace-app
    ```
   
   If everything is set up the app should be now accessible over http://localhost:8080 and should you greet with an empty result.
   You can start throwing PDFs into the documents or task folder now.
