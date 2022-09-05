# 🍃 Spring Cloud AWS

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/from-referrer/)

Simplifies using AWS managed services in a Spring and Spring Boot applications.

For a deep dive into the project, refer to the Spring Cloud AWS documentation:

| Version                   | Reference Docs                                                                                      | API Docs                                                                               |
|---------------------------|-----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Spring Cloud AWS 3.0.0-M2 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0-M2/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0-M2/apidocs/index.html) | 
| Spring Cloud AWS 2.4.2    | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.4.2/reference/html/index.html)    | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.4.2/apidocs/index.html)    | 
| Spring Cloud AWS 2.3.5    | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.3.5/reference/html/index.html)    | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.3.5/apidocs/index.html)    |

## Sponsors

Big thanks to [Localstack](https://localstack.cloud) for providing PRO licenses to the development team!

[![localstacklogo](https://user-images.githubusercontent.com/1357927/166442325-6a94bdcd-8da0-4c76-b04e-69819a490c1c.png)](https://localstack.cloud)

## Compatibility with Spring Project Versions

This project has dependency and transitive dependencies on Spring Projects. The table below outlines the versions of Spring Cloud, Spring Boot and Spring Framework versions that are compatible with certain Spring Cloud AWS version.

| Spring Cloud AWS          | Spring Cloud                                                                                                          | Spring Boot  | Spring Framework | AWS Java SDK |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------|--------------|------------------|--------------|
| 2.3.x (maintenance mode)  | [2020.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2020.0-Release-Notes) (3.0/Illford) | 2.4.x, 2.5.x | 5.3.x            | 1.x          |
| 2.4.x (maintenance mode)  | [2021.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2021.0-Release-Notes) (3.1/Jubilee) | 2.6.x, 2.7.x        | 5.3.x            | 1.x          |
| 3.0.x (under development) | [2021.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2021.0-Release-Notes) (3.1/Jubilee) | 2.6.x, 2.7.x        | 5.3.x            | 2.x          |

## Supported AWS integrations

| AWS Service     | Spring Cloud AWS 2.x | Spring Cloud AWS 3.x                                                                                                                        |
|-----------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| S3              | ✅                    | ✅                                                                                                                                           |
| SNS             | ✅                    | ✅                                                                                                                                           |
| SES             | ✅                    | ✅                                                                                                                                           |
| Parameter Store | ✅                    | ✅                                                                                                                                           |
| Secrets Manager | ✅                    | ✅                                                                                                                                           |
| SQS             | ✅                    | ✅                                                                                                                                           |
| RDS             | ✅                    | TODO [#322](https://github.com/awspring/spring-cloud-aws/issues/322)                                                                        |
| EC2             | ✅                    | ❌                                                                                                                                           |
| ElastiCache     | ✅                    | ❌                                                                                                                                           |
| CloudFormation  | ✅                    | ❌                                                                                                                                           |
| CloudWatch      | ✅                    | ✅                                                                                                                                           |
| Cognito         | ✅                    | [Covered by Spring Boot](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0-SNAPSHOT/reference/html/index.html#migration-from-2-x-to-3-x) |
| DynamoDB        | ❌                    | ✅                                                                                                                                           |

Note, that Spring provides support for other AWS services in following projects:

- [Spring Cloud Stream Binder AWS Kinesis](https://github.com/spring-cloud/spring-cloud-stream-binder-aws-kinesis)
- [Spring Cloud Config Server](https://github.com/spring-cloud/spring-cloud-config) supports AWS Parameter Store and Secrets Manager
- [Spring Integration for AWS](https://github.com/spring-projects/spring-integration-aws)

## Current Efforts

We are working on Spring Cloud AWS 3.0 - a major release that includes moving to AWS SDK v2 and re-thinking most of the integrations.

## Checking out and building

To check out the project and build it from source, do the following:

```
git clone https://github.com/awspring/spring-cloud-aws.git
cd spring-cloud-aws
./mvnw package
```

To build and install jars into your local Maven cache:

```
./mvnw install
```

For faster builds, we recommend using [Maven Daemon](https://github.com/apache/maven-mvnd) and using following commands:

Build:

```
make build
```

Clean:

```
make clean
```

Format code:

```
make format
```

## Building documentation

Documentation can be built by activating the `docs` profile in the maven build.

```
make docs
```

It generates:

- reference documentation in `docs/target/generated-docs/`
- API docs in `target/site/`

## Getting in touch

- [Discussions on Github](https://github.com/awspring/spring-cloud-aws/discussions) - the best way to discuss anything Spring Cloud AWS related

Or reach out directly to individual team members:

- Maciej Walkowiak [Twitter](https://twitter.com/maciejwalkowiak)
- Matej Nedic [Twitter](https://twitter.com/MatejNedic1)
- Tomaz Fernandes [Twitter](https://twitter.com/tomazfernandes_)
