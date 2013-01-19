# ElasticSpring

ElasticSpring is a cloud application framework for the [Amazon Webservice Plattform] [AWS]. ElasticSpring enables developers to
use popular Amazon Webservices with the [Spring Framework] [Spring]. With ElasticSpring, developers can re-use their existing
knowledge, code and components build with the Spring Framework programming model and build applications using the
Amazon Webservices. While continuing to use the programming model, developers can take advantage of the scaliablity
of the service provided by the Amazon cloud platform.


# Checking out and Building

To check out the project and build from source, do the following:

    git clone https://github.com/aemruli/elasticspring.git
    cd elasticspring
    ./gradlew build

If you encounter out of memory errors during the build, increase available heap and permgen for Gradle:

    GRADLE_OPTS='-XX:MaxPermSize=1024m -Xmx1024m'

To build and install jars into your local Maven cache:

    ./gradlew install

To build api Javadoc (results will be in `build/api`):

    ./gradlew api

To build reference documentation (results will be in `build/reference`):

    ./gradlew reference

# Using IntelliJ IDEA

ElastiSpring development is done with [IntelliJ IDEA] [IntelliJ]. All configurations files (which do not contain environment
specific paths) are kept in the repository to ensure that every developer gets the same settings (Copyright header,
Formatter, Inspections, Encoding etc.). These files are located in the root and .idea folder.

 You can open the project through File --> Open... (select the folder which you checked out before)


# Running integration tests
ElasticSpring contains a test-suite which runs integration tests to ensure compatibility with the Amazon Webservices.
In order to run the integration tests, the build process have to create different ressources on the Amazon Webservice
platform (Amazon EC2 instances, Amazon RDS instances, Amazon S3 Buckets, Amazon SQS Qeues). Creating these ressource
takes time and costs money, because every instance creation is charged with a one hour usage. Therefore ElasticSpring
does not execute the integration tests by default.

In order to execute the integration tests you have to create two configuration files that configure the necessary
parameters to build the environment.

Please create a new file named access.properties this file must contain two properties named accessKey and secretKey.
These two properties are account/user specific and should never be shared to anybody. Two retrieve this setting you have
to open your account inside the AWS console and retreive them through the [Security Crendetials Page]
[AWS-Security-Credentials].
*Note:* In general we recommend that you use an [Amazon IAM] [Amazon-IAM] user instead of the account itself.

An example file will look like this

	accessKey=ilaugsjdlkahgsdlaksdhg
	secretKey=aöksjdhöadjs,höalsdhjköalsdjhasd+

Also you have to create another file named mail.properties which will provide the sender and recipient mail address to
test the [Amazon Simple E-Mail Service] [Amazon-SES]. These two addresses must be verified for the Amazon SES Service.

An example file will have the following contents

	senderAddress=foo@bar.com
	recipientAddress=baz@buz.com

After creating both files and storing them outside the project (or inside the project, they are ignored in git)
you have to provide the configuration directory while running the build. Providing these configuration setting will
automatically execute the integration tests.

To build with the integration tests you must execute

	./gradlew build -Dels.config.dir=/Users/foo/config/dir
 	(on windows you will also need a leading slash before the drive letter e.g. /C:/users/foo/config/dir)

The integration test will create a [Amazon Webservices CloudFormation] [Amazon-CloudFormation] Stack and execute the
tests. The stack is destroyed after executing the tests (either successful or failed) to ensure that there are no
non-necessary costs.

### Costs of integration tests
The costs for one integration test run should not be more then 0.13 $ (excl. VAT).


# Developing using Amazon Webservices
During development it might be time-consuming to run the integration tests regularly. In order to create a stack only
once, and reuse them for the tests run, you have to create the stack manually using the template found in
/elasticspring-integration-test/src/test/resources. You will need to create the stack with the name
"IntegrationTestStack" to ensure that the integration tests will re-use the stack.

# Getting in touch
Individual team members can be found on different social media channels

Agim Emruli ([Twitter](http://twitter.com/aemruli) / [LinkedIn](http://de.linkedin.com/in/agimemruli/))
Alain Sahli ([Twitter](http://twitter.com/alainsahli) / [LinkedIn](http://ch.linkedin.com/in/asahli) )

[AWS]: http://aws.amazon.com/
[Spring]: http://www.springsource.org
[IntelliJ]: http://www.jetbrains.com/idea/
[AWS-Security-Credentials]: https://portal.aws.amazon.com/gp/aws/securityCredentials
[Amazon-IAM]: https://aws.amazon.com/iam/
[Amazon-SES]: https://aws.amazon.com/ses/
[Amazon-CloudFormation]: https://aws.amazon.com/de/cloudformation/