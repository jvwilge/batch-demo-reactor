## Batch demo reactor

This is demo project to demonstrate `ConnectableFlux`

It's best to use this as a reference project

# setting up test data

First run `GenerateTestDataTest` , this will put test data in `/tmp`

Start elasticsearch with `./start-elasticsearch.sh` (Docker is needed)

Insert testdata with `./bulk-insert.sh`


# Running the load test

Start the application by running `BatchDemoApplication`

Download [JMeter](https://jmeter.apache.org/) and load `Test Plan.jmx` (this will read the testdata from `/tmp`)