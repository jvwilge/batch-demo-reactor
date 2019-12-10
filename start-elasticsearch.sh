#!/bin/sh
# .2 cpu's results in a very low, unrealistic,  throughput
docker run -p 9200:9200 -p 9300:9300 --cpus=".3" -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch-oss:7.5.0