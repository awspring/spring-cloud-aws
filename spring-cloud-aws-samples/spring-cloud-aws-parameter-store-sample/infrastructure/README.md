# Spring Cloud AWS Parameter Store Sample App Infrastructure

Infrastructure code to run **Spring Cloud AWS Parameter Store Sample App** based on [AWS CDK](https://aws.amazon.com/cdk/)

## How to install

* `npm install`

## How to run

* `cdk synth`
* `cdk deploy`

Once you don't need the infrastructure anymore, it can be destroyed by calling:

* `cdk destroy`.

You will need following policies to run cdk and Sample app:

* `{
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": [
                  "ssm:PutParameter",
                  "ssm:DeleteParameter",
                  "ssm:GetParametersByPath",
                  "ssm:AddTagsToResource",
                  "cloudformation:DescribeStackEvents",
                  "cloudformation:GetTemplate",
                  "cloudformation:DeleteStack",
                  "cloudformation:CreateChangeSet",
                  "cloudformation:DescribeChangeSet",
                  "cloudformation:ExecuteChangeSet",
                  "cloudformation:DescribeStacks"
              ],
              "Resource": "yourArn"
          }
      ]
  }`
