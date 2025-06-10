# Spring Cloud AWS

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/awspring/spring-cloud-aws/)

Spring Cloud AWS simplifies using AWS managed services in a Spring and Spring Boot applications.

For a deep dive into the project, refer to the Spring Cloud AWS documentation:

| Version                | Reference Docs                                                                                   | API Docs                                                                            |
|------------------------|--------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| Spring Cloud AWS 3.3.1 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.3.1/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.3.1/apidocs/index.html) | 
| Spring Cloud AWS 3.2.1 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.2.1/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.2.1/apidocs/index.html) | 
| Spring Cloud AWS 3.1.1 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.1.1/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.1.1/apidocs/index.html) | 
| Spring Cloud AWS 3.0.4 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.0.4/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/3.0.4/apidocs/index.html) | 
| Spring Cloud AWS 2.4.4 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.4.4/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.4.4/apidocs/index.html) | 
| Spring Cloud AWS 2.3.5 | [Reference Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.3.5/reference/html/index.html) | [API Docs](https://docs.awspring.io/spring-cloud-aws/docs/2.3.5/apidocs/index.html) |

## Sponsors

Big thanks to [LocalStack](https://localstack.cloud) for providing PRO licenses to the development team!

<a href="https://localstack.cloud"><img src="https://user-images.githubusercontent.com/47351025/215054012-f5af0761-0bd5-49c6-bd3e-c6b2a6844f53.png" height="100" /></a>

## Compatibility with Spring Project Versions

This project has dependency and transitive dependencies on Spring Projects. The table below outlines the versions of Spring Cloud, Spring Boot and Spring Framework versions that are compatible with certain Spring Cloud AWS version.

| Spring Cloud AWS            | Spring Cloud                                                                                                          | Spring Boot  | Spring Framework | AWS Java SDK |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------|--------------|------------------|--------------|
| 2.3.x (maintenance mode)  	 | [2020.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2020.0-Release-Notes) (3.0/Illford) | 2.4.x, 2.5.x | 5.3.x            | 1.x          |
| 2.4.x (maintenance mode)  	 | [2021.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2021.0-Release-Notes) (3.1/Jubilee) | 2.6.x, 2.7.x | 5.3.x            | 1.x          |
| 3.0.x                       | [2022.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2022.0-Release-Notes) (4.0/Kilburn) | 3.0.x, 3.1.x | 6.0.x            | 2.x          |
| 3.1.x                       | [2023.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2023.0-Release-Notes) (4.0/Kilburn) | 3.2.x        | 6.1.x            | 2.x          |
| 3.2.x                       | [2023.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2023.0-Release-Notes) (4.0/Kilburn) | 3.2.x, 3.3.x | 6.1.x            | 2.x          |
| 3.3.x                       | [2024.0.x](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2024.0-Release-Notes)               | 3.4.x        | 6.2.x            | 2.x          |

**Note**: 3.0.0-M2 is the last version compatible with Spring Boot 2.7.x and Spring Cloud 3.1. Starting from 3.0.0-M3, project has switched to Spring Boot 3.0.

## Supported AWS integrations

| AWS Service     | Spring Cloud AWS 2.x | Spring Cloud AWS 3.x                                                                                                                        |
|-----------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| S3              | ✅                    | ✅                                                                                                                                           |
| SNS             | ✅                    | ✅                                                                                                                                           |
| SES             | ✅                    | ✅                                                                                                                                           |
| Parameter Store | ✅                    | ✅                                                                                                                                           |
| Secrets Manager | ✅                    | ✅                                                                                                                                           |
| SQS             | ✅                    | ✅                                                                                                                                           |
| RDS             | ✅                    | ❌                                                                                                                                           |
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

## Getting in touch

- [Discussions on Github](https://github.com/awspring/spring-cloud-aws/discussions) - the best way to discuss anything Spring Cloud AWS related

Or reach out directly to individual team members:

- Maciej Walkowiak [Twitter](https://twitter.com/maciejwalkowiak)
- Matej Nedic [Twitter](https://twitter.com/MatejNedic1)
- Tomaz Fernandes [Twitter](https://twitter.com/tomazfernandes_)
