#!/usr/bin/env bash

echo "ensure database folder exists"
mkdir -p /storage/paperspace/database/

java -jar /app.jar