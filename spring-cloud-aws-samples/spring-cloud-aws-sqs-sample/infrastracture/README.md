# Spring Cloud AWS SQS Sample App Infrastructure

Infrastructure code to run **Spring Cloud AWS SQS Sample App** based on [AWS CDK](https://aws.amazon.com/cdk/)

## How to install

* `npm install`

## How to run

* `cdk synth`
* `cdk deploy`

Once you don't need the infrastructure anymore, it can be destroyed by calling:

* `cdk destroy`.

You will need following policies to run cdk and Sample app:
(Ideally this should be two different policies. One just for SQS and another for cloudFormations. For sake of sample and simplicity we will use one.)

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sqs:DeleteMessage",
                "sqs:GetQueueUrl",
                "sqs:ReceiveMessage",
                "sqs:SendMessage",
                "sqs:GetQueueAttributes",
                "sqs:DeleteQueue",
                "sqs:CreateQueue",
                "cloudformation:CreateChangeSet",
                "cloudformation:DescribeStacks",
                "cloudformation:DescribeStackEvents",
                "cloudformation:GetTemplate",
                "cloudformation:DeleteStack",
                "cloudformation:DescribeChangeSet",
                "cloudformation:ExecuteChangeSet"
            ],
            "Resource": "arn"
        }
    ]
}