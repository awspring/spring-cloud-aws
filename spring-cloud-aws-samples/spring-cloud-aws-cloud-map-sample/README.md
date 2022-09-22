# Spring Cloud AWS Cloud Map Sample

Here is a step by step guide to run the sample application.

----

## Prerequisites

* Active internet connection
* Java 8 or above
* AWS credentials configured in `~/.aws/credentials` or as environment variables (see [AWS SDK documentation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html))
* Docker installed

## Running the sample

* Build the sample application

```bash
mvn clean install
```

* Run a docker build to create a docker image using the `Dockerfile` defined in the root directory of this sample project:

```bash
docker build -t aws-samples-cloudmap .
```

* Create a ECS Cluster using AWS CLI:

```bash
aws ecs create-cluster --cluster-name cloudmap-sample
```

* Create a ECR repository using AWS CLI:

```bash
aws ecr create-repository --repository-name cloudmap-sample
```

* Login to ECR, tag the docker image and push it to ECR:

```bash
$(aws ecr get-login --no-include-email --region us-east-1)
docker tag aws-samples-cloudmap:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/cloudmap-sample:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/cloudmap-sample:latest
```

* Create a IAM role for ECS task execution:

```bash
aws iam create-role --role-name ecsTaskExecutionRole --assume-role-policy-document file://task-execution-assume-role.json
```

**task-execution-assume-role.json**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ecs-tasks.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

* Attach the following IAM policy to the role created in the previous step:

```bash
aws iam put-role-policy --role-name ecsTaskExecutionRole --policy-name ecsTaskExecutionRole --policy-document file://task-execution-role-policy.json
```

**task-execution-role-policy.json**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:DescribeRepositories"
            ],
            "Resource": "*"
        }
    ]
}
```

* Create a ECS Task role:

```bash
aws iam create-role --role-name ecsTaskRole --assume-role-policy-document file://task-assume-role.json
```

**task-assume-role.json**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ecs-tasks.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

* Attach the following IAM policy to the role created in the previous step:

```bash
aws iam put-role-policy --role-name ecsTaskRole --policy-name ecsTaskRole --policy-document file://task-role-policy.json
```

**task-role-policy.json**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "servicediscovery:CreateService",
                "servicediscovery:DeleteService",
                "servicediscovery:GetService",
                "servicediscovery:ListInstances",
                "servicediscovery:ListNamespaces",
                "servicediscovery:ListServices",
                "servicediscovery:RegisterInstance",
                "servicediscovery:DeregisterInstance",
                "servicediscovery:ListOperations",
                "servicediscovery:GetOperation"
            ],
            "Resource": "*"
        }
    ]
}
```

* Create a Task Definition using AWS CLI:

```bash
aws ecs register-task-definition --name cloudmap-sample-definition --cli-input-json file://task-definition.json
```

**Task definition file**

```json
{
    "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
    "containerDefinitions": [
        {
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/CloudMap",
                    "awslogs-region": "us-east-1",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "cpu": 0,
            "image": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/cloudmap-sample:latest",
            "name": "mainContainer"
        }
    ],
    "memory": "7168",
    "taskRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskRole",
    "family": "CloudMap",
    "requiresCompatibilities": [
        "FARGATE"
    ],
    "networkMode": "awsvpc",
    "runtimePlatform": {
        "operatingSystemFamily": "LINUX"
    },
    "cpu": "2048"
}
```

* Run the task using AWS CLI:

```bash
aws ecs run-task --cluster cloudmap-sample --task-definition cloudmap-sample-definition --launch-type FARGATE --network-configuration "awsvpcConfiguration={subnets=[<SUBNET_ID>],securityGroups=[<SECURITY_GROUP_ID>],assignPublicIp=ENABLED}"
```

## Verify Cloud Map service registration in AWS console

* Login to AWS console and navigate to Cloud Map service. You should see a namespace (`a-namespace`) and a service (`a-service`) created. Under the service, you should see a instance registered with the IP address of the container running in ECS.

## Clean up

* Stop the task using AWS CLI:

```bash
aws ecs stop-task --cluster cloudmap-sample --task <TASK_ID>
```

* Delete the task definition using AWS CLI:

```bash
aws ecs deregister-task-definition --task-definition cloudmap-sample-definition
```

* Delete the ECS cluster using AWS CLI:

```bash
aws ecs delete-cluster --cluster cloudmap-sample
```

* Detach the associated policies

```bash
aws iam detach-role-policy --role-name ecsTaskExecutionRole --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/ecsTaskExecutionRole
aws iam detach-role-policy --role-name ecsTaskRole --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

* Delete the IAM role for ECS task execution using AWS CLI:

```bash
aws iam delete-role --role-name ecsTaskExecutionRole
aws iam delete-role --role-name ecsTaskRole
```

* Delete ECR repository using AWS CLI:

```bash
aws ecr delete-repository --repository-name cloudmap-sample --force
```
