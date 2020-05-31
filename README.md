# paper{s}pace

a small web application to manage all your offline documents. 
Provides a searchable storage for your documents and reminds you of upcoming tasks.
 
## Introduction
what can it do:
- provides a searchable storage for your documents 
- easy to install and use
- stores automatically documents or tasks placed in specific folders
- tasks have a due date associated, and you will be reminded before the date is approaching by mail

what is it **not** (and probably never be)
- not a full sized business application
- means:
  - no user management
  - no settings in the ui
  
## Deployment

the easiest solution is to adjust the provided docker-compose file and deploy the whole stack.
It is also possible to build booth applications and deploy them on a server or locally.

### docker
 
1. checkout the project
    ```shell script
    git clone git@gitlab.com:dedicatedcode/paperspace.git
    ```
2. navigate to the deployment folder
3. copy the provided **env-sample** to **.env**. This will contain all your passwords and usernames.
    ```shell script
    cp env-sample .env
    ```
4. adjust **.env** to your requirements.
5. adjust the **docker-compose.yml** file to your environment. Especially the ports section of the api. You can find all available configuration options in th section [docker configuration](#docker-configuration).
6. run the stack with docker compose or deploy it in your docker swarm
   ```shell script
    docker-compose up -d
    ``` 
   or for docker swarm
   ```shell script
    docker stack deploy --compose-file docker-compose.yml paperspace
    ```
    after the application is running you should be able to open the app through the port specified in the api service.
    For example if you mapped port 8080, then navigate to http://localhost:8080
    
There is also a minimal docker-compose file where only the options needed are set for reference.
    
### docker configuration
API Configuration Options

| key | mandatory | default value | description |
|-----|-----------|---------------|-------------|
| APPLICATION_HOST | y | http://localhost:8080 | the URL under which the application is reachable. Used to provide working links in emails. |
| DB_HOST | n | 'db' | The host of the database to connect to. |
| DB_PORT | n | '3306' | The port of your database. |
| DB_TABLE | y | '' | Name of your database. |
| DB_USER | y | '' | User used to connect to the database. |
| DB_PASSWORD | y | '' | Password to connect to your database. |
| SEARCH_HOST | n | 'search' | Hostname of the solr instance. |
| SEARCH_PORT | n | '8983' | Port of the solr instance. |
| STORAGE_PATH | n | '/binary' | where do we store the uploaded binary files. This is also the folder you should backup. |
| ENABLE_MAIL | n | 'false' | Is sending emails enabled. If enabled all the other properties starting handling mails should be set.|
| MAIL_TO_ADDRESS | n | '' | Who receives notifications about new documents or upcoming tasks?|
| MAIL_FROM_ADDRESS | n | '' | In which name should the system send out emails. |
| MAIL_ATTACH_DOCUMENTS | n | 'false' | Should the system also attach the uploaded document to the email. |
| MAILING_HOST | n | '' | The host of your mailing provider. |
| MAILING_PORT | n | 587 | The port of your mailing provider. |
| MAILING_PROTOCOL | n | 'smtp' | The protocoll the app should use. |
| MAILING_SMTP_AUTH | n | 'yes' | Should we login? |
| MAILING_SMTP_USE_STARTTLS | n | 'yes' | Should we use starttls? |
| MAILING_USERNAME | n | '' | Username provided by your mailing provider. |
| MAILING_PASSWORD | n | ''| Password used to login to your mailing provider.|

Feeder Configuration Options

| key | mandatory | default value | description |
|-----|-----------|---------------|-------------|
| API_URL | n | http://api:8080 | How to reach the api?|
| DOCUMENT_INPUT | n | /data/input/documents | The place to watch for new documents. |
| DOCUMENT_IGNORED | n | /data/ignored/documents | Where do we put ignored documents? |
| DOCUMENT_ERROR | n | /data/error/documents | In case of an error, where do we put the uploaded file?|
| DOCUMENT_PROCESSED | n | /data/processed/documents | The place to store processed documents. |
| DOCUMENT_BACKUP | n | false | Should we move the new document to the processed document folder. If false, the document will be deleted. |
| TASK_INPUT | n | /data/input/tasks | The place to watch for new tasks. | 
| TASK_IGNORED | n | /data/ignored/tasks | Where do we put ignored tasks? | 
| TASK_ERROR | n | /data/errors/tasks | In case of an error, where do we put the uploaded file?| 
| TASK_PROCESSED | n | /data/processed/tasks | The place to store processed tasks. |
| TASK_BACKUP | n | false |  Should we move the new tasks to the processed tasks folder. If false, the task document will be deleted.  |
| OCR_LANGUAGE | n | deu |  The language code used for OCR. |
