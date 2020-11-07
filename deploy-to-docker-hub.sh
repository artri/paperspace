#!/usr/bin/env bash

cd api && ./gradlew build && docker build . -t dedicatedcode/paperspace:latest
cd ..
cd search && docker build . -t dedicatedcode/paperspace-search:latest
cd ..

docker push dedicatedcode/paperspace:latest
docker push dedicatedcode/paperspace-search:latest
docker push dedicatedcode/paperspace-feeder:latest