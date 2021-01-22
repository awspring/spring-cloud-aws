import * as cdk from '@aws-cdk/core';
import * as ssm from '@aws-cdk/aws-ssm';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

	  new ssm.StringParameter(this, 'Parameter', {
		  parameterName: '/config/spring/message',
		  stringValue: 'Spring-cloud-aws value!'
	  });
  }
}
