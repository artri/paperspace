#!/usr/bin/env bash

cd api && ./gradlew build && sudo docker build . -t dedicatedcode/paperspace:latest && sudo docker push dedicatedcode/paperspace:latest
cd ..
cd search && sudo docker build . -t dedicatedcode/paperspace-search:latest && sudo docker push dedicatedcode/paperspace-search:latest
cd ..