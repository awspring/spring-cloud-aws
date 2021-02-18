import * as cdk from '@aws-cdk/core';
import * as sns from '@aws-cdk/aws-sns';
import * as sqs from '@aws-cdk/aws-sqs';
import * as subs from '@aws-cdk/aws-sns-subscriptions';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const topic = new sns.Topic(this, 'snsSpring', {
      displayName: 'Spring cloud AWS SNS sample',
      topicName: 'snsSpring',
    });

    const queue = new sqs.Queue(this, 'spring-aws', { queueName: `${id}-spring-aws` });

    topic.addSubscription(new subs.SqsSubscription(queue));
    //URL from NGROK goes here
    topic.addSubscription(new subs.UrlSubscription('http://1745bc39a0ae.ngrok.io/testTopic'));
  }
}
