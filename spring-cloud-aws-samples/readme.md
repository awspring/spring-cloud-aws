# Spring Cloud AWS Samples

Samples are prepared to run on LocalStack - a local equivalent of AWS. 

To start LocalStack locally:

```
$ docker-compose up
```

## Infrastructure

Samples use AWS CDK to create the infrastructure components to run the sample. To deploy infrastructure, you need to install CDK and [CDK local](https://github.com/localstack/aws-cdk-local):

```
$ npm install -g aws-cdk-local aws-cdk
```

Then, in `infrastructure` directory:

```
$ mvn package
$ cdklocal bootstrap 
$ cdklocal deploy
```

## How to run?

Samples are regular Spring Boot applications. The best way to run them is to run the main `@SpringBootApplication` annotated class directly from an IDE.

## How to destroy infrastructure?

Infrastructure is destroyed once LocalStack container shuts down. If you want to destroy infrastructure manually, run:

```
$ cdklocal destroy
```

## How to run against real AWS?

To run samples against real AWS, update `spring.cloud.aws` properties in sample's `application.properties` to reflect your AWS configuration or delete these properties completely to use defaults.
