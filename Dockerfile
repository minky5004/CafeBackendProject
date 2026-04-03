FROM ubuntu:latest
LABEL authors="minky"

ENTRYPOINT ["top", "-b"]