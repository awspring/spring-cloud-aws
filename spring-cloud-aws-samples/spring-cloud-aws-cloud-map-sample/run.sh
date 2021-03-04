mvn clean install -DskipTests=true
docker build -t aws-samples-cloudmap .
docker tag aws-samples-cloudmap:latest 775492342640.dkr.ecr.us-east-1.amazonaws.com/aws-samples-cloudmap:latest
docker push 775492342640.dkr.ecr.us-east-1.amazonaws.com/aws-samples-cloudmap:latest
