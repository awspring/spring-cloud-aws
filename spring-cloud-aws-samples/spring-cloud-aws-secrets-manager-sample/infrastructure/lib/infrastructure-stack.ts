import * as cdk from '@aws-cdk/core';
import * as secretsmanager from '@aws-cdk/aws-secretsmanager';

export class InfrastructureStack extends cdk.Stack {
	constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
		super(scope, id, props);

		new secretsmanager.Secret(this, 'Secret', {
			secretName: '/secrets/spring-cloud-aws-sample-app',
			generateSecretString: {
				secretStringTemplate: '{}',
				generateStringKey: 'password',
			}
		});
	}
}
