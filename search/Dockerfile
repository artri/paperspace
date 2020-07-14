FROM solr:8.5.0
USER root
RUN mkdir -p /data/solr/documents
RUN chown 8983:8983 /data/solr/documents
USER solr
ADD config /var/solr/data/cores/