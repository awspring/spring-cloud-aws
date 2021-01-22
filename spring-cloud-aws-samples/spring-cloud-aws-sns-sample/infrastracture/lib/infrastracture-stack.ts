import * as cdk from '@aws-cdk/core';
import * as sns from '@aws-cdk/aws-sns';
import * as sqs from '@aws-cdk/aws-sqs';
import * as subs from '@aws-cdk/aws-sns-subscriptions';

export class InfrastractureStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const topic = new sns.Topic(this, 'snsSpring', {
      displayName: 'Spring cloud AWS SNS samplenpm i @aws-cdk/aws-sns',
      topicName: 'snsSpring',
    });

    const queue = new sqs.Queue(this, 'spring-aws', { queueName: `${id}-spring-aws` });

    topic.addSubscription(new subs.SqsSubscription(queue));
  }
}
