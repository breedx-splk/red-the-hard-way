#!/bin/bash

if [ "" == "${SPLUNK_ACCESS_TOKEN}" ] ; then
  echo "You must define SPLUNK_ACCESS_TOKEN env var."
  exit 1
fi

docker run -it --rm  \
  -v $(pwd)/collector.yaml:/etc/otelcol-contrib/config.yaml \
  -e SPLUNK_ACCESS_TOKEN=${SPLUNK_ACCESS_TOKEN} \
  otel/opentelemetry-collector-contrib:latest