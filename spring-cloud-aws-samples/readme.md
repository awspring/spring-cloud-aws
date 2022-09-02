# Spring Cloud AWS Samples

Samples are prepared to run on Localstack - a local equivalent of AWS. 

To start Localstack locally:

```
$ docker-compose up
```

## Infrastructure

Samples that have `infrastructure` directory inside use AWS CDK to create the infrastructure components to run the sample.

In `infrastructure` directory:

```
mvn package
cdklocal bootstrap 
cdklocal deploy
```

**Note** - `cdklocal bootstrap` has to be executed only once.

## How to run?

Samples are regular Spring Boot applications. The best way to run them is to run the main `@SpringBootApplication` annotated class directly from an IDE.
