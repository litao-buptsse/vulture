FROM docker.registry.clouddev.sogou:5000/library/sunshine-hadoop-tools:cdh5.7.0

ENV APPROOT /search/vulture
WORKDIR $APPROOT
ADD .tmp $APPROOT
