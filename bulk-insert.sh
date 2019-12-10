#!/bin/sh
curl -X POST "localhost:9200/_bulk?pretty" -H 'Content-Type: application/x-ndjson' --data-binary  @'/tmp/products.jsonl'

