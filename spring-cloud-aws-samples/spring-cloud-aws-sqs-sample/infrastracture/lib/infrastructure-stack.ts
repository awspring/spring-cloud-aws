import * as cdk from '@aws-cdk/core';
import * as sqs from '@aws-cdk/aws-sqs';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

	  new sqs.Queue(this, 'spring-aws', { queueName: `${id}-spring-aws` });
      new sqs.Queue(this, 'aws-pojo', { queueName: `${id}-aws-pojo` });


  }
}
