echo 'make sure language is installed and starting application'
apt-get install -yyy tesseract-ocr-${OCR_LANGUAGE}
echo 'make sure permissions on folders are set correctly'
chown -R paperspace:paperspace /data
su paperspace -c 'java -jar /app.jar'
