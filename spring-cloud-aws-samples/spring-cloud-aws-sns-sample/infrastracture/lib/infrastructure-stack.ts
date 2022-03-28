import * as cdk from '@aws-cdk/core';
import * as sns from '@aws-cdk/aws-sns';
import * as subs from '@aws-cdk/aws-sns-subscriptions';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const topic = new sns.Topic(this, 'snsSpring', {
      displayName: 'Spring cloud AWS SNS sample',
      topicName: 'snsSpring',
    });

    //URL from NGROK goes here
    topic.addSubscription(new subs.UrlSubscription('https://5d50-2a02-8109-8380-c8c-d911-1af7-8ab6-35e1.ngrok.io/testTopic'));
  }
}
