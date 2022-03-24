/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awspring.cloud.s3.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

public final class CrossRegionS3ClientGenerator {

	private CrossRegionS3ClientGenerator() {

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new RuntimeException("Need 1 parameter: the JavaParser source checkout root directory.");
		}

		CompilationUnit compilationUnit = new CompilationUnit();
		compilationUnit.setPackageDeclaration("io.awspring.cloud.s3");
		compilationUnit.setBlockComment(" * Copyright 2013-2022 the original author or authors.\n" + " *\n"
				+ " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
				+ " * you may not use this file except in compliance with the License.\n"
				+ " * You may obtain a copy of the License at\n" + " *\n"
				+ " *      https://www.apache.org/licenses/LICENSE-2.0\n" + " *\n"
				+ " * Unless required by applicable law or agreed to in writing, software\n"
				+ " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
				+ " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
				+ " * See the License for the specific language governing permissions and\n"
				+ " * limitations under the License.");
		compilationUnit.addImport(S3Client.class);
		compilationUnit.addImport(Function.class);
		compilationUnit.addImport(StringUtils.class);
		compilationUnit.addImport(SdkAutoCloseable.class);
		compilationUnit.addImport(S3Exception.class);
		compilationUnit.addImport(Region.class);
		compilationUnit.addImport(ConcurrentHashMap.class);
		compilationUnit.addImport(Map.class);
		compilationUnit.addImport(S3ClientBuilder.class);
		compilationUnit.addImport(ConcurrentLruCache.class);
		compilationUnit.addImport(LoggerFactory.class);
		compilationUnit.addImport(Logger.class);
		ClassOrInterfaceDeclaration crossRegionS3Client = compilationUnit.addClass("CrossRegionS3Client")
				.addImplementedType(S3Client.class.getName()).setPublic(true);

		crossRegionS3Client.addFieldWithInitializer("Logger", "LOGGER",
				StaticJavaParser.parseExpression("LoggerFactory.getLogger(CrossRegionS3Client.class)"),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		crossRegionS3Client.addFieldWithInitializer("int", "DEFAULT_BUCKET_CACHE_SIZE",
				StaticJavaParser.parseExpression("20"), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC,
				Modifier.Keyword.FINAL);
		crossRegionS3Client.addFieldWithInitializer("Map<Region, S3Client>", "clientCache",
				StaticJavaParser.parseExpression("new ConcurrentHashMap<>(Region.regions().size())"),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

		crossRegionS3Client.addField("S3Client", "defaultS3Client", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		crossRegionS3Client.addField("ConcurrentLruCache<String, S3Client>", "bucketCache", Modifier.Keyword.PRIVATE,
				Modifier.Keyword.FINAL);

		ConstructorDeclaration constructorDeclaration = crossRegionS3Client.addConstructor(Modifier.Keyword.PUBLIC);
		constructorDeclaration.addParameter("S3ClientBuilder", "clientBuilder");
		constructorDeclaration.setBody(new BlockStmt().addStatement("this(DEFAULT_BUCKET_CACHE_SIZE, clientBuilder);"));

		ConstructorDeclaration primaryConstructor = crossRegionS3Client.addConstructor(Modifier.Keyword.PUBLIC);
		primaryConstructor.addParameter("int", "bucketCacheSize");
		primaryConstructor.addParameter("S3ClientBuilder", "clientBuilder");
		primaryConstructor.setBody(new BlockStmt().addStatement("this.defaultS3Client = clientBuilder.build();")
				.addStatement("\t\tthis.bucketCache = new ConcurrentLruCache<>(bucketCacheSize, bucket -> {\n"
						+ "\t\t\tRegion region = resolveBucketRegion(bucket);\n"
						+ "\t\t\treturn clientCache.computeIfAbsent(region, r -> {\n"
						+ "\t\t\t\tLOGGER.debug(\"Creating new S3 client for region: {}\", r);\n"
						+ "\t\t\t\treturn clientBuilder.region(r).build();\n" + "\t\t\t});\n" + "\t\t});"));

		TypeSolver typeSolver = new ClassLoaderTypeSolver(CrossRegionS3ClientGenerator.class.getClassLoader());
		ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = typeSolver
				.solveType(S3Client.class.getName());
		resolvedReferenceTypeDeclaration.getAllMethods().stream().sorted(Comparator.comparing(MethodUsage::getName))
				.forEach(u -> {
					if (!u.getName().equals("listBuckets") && u.getParamTypes().size() == 1
							&& u.getParamType(0).describe().endsWith("Request")) {
						MethodDeclaration methodDeclaration = crossRegionS3Client.addMethod(u.getName(),
								Modifier.Keyword.PUBLIC);
						methodDeclaration.addParameter(
								new Parameter(new ClassOrInterfaceType(u.getParamType(0).describe()), "request"));
						methodDeclaration
								.setType(new ClassOrInterfaceType(u.getDeclaration().getReturnType().describe()));
						methodDeclaration.setBody(new BlockStmt()
								.addStatement("return executeInBucketRegion(request.bucket(), s3Client -> s3Client."
										+ u.getName() + "(request));"));
						methodDeclaration.addMarkerAnnotation(Override.class);
						methodDeclaration.addThrownException(AwsServiceException.class);
						methodDeclaration.addThrownException(SdkClientException.class);
					}
				});

		MethodDeclaration listBuckets = crossRegionS3Client.addMethod("listBuckets", Modifier.Keyword.PUBLIC);
		listBuckets.addMarkerAnnotation(Override.class);
		listBuckets.addThrownException(AwsServiceException.class);
		listBuckets.addThrownException(SdkClientException.class);
		listBuckets.addParameter(ListBucketsRequest.class, "request");
		listBuckets.setType(ListBucketsResponse.class);
		listBuckets.setBody(new BlockStmt().addStatement("return defaultS3Client.listBuckets(request);"));

		MethodDeclaration executeInBucketRegion = crossRegionS3Client.addMethod("executeInBucketRegion",
				Modifier.Keyword.PRIVATE);
		executeInBucketRegion.addParameter("String", "bucket");
		executeInBucketRegion.addParameter("Function<S3Client, Result>", "fn");
		executeInBucketRegion.addTypeParameter("Result");
		executeInBucketRegion.setType("Result");

		executeInBucketRegion.setBody(new BlockStmt().addStatement("try {\n"
				+ "\t\t\tif (bucketCache.contains(bucket)) {\n" + "\t\t\t\treturn fn.apply(bucketCache.get(bucket));\n"
				+ "\t\t\t} else {\n" + "\t\t\t\treturn fn.apply(defaultS3Client);\n" + "\t\t\t}\n"
				+ "\t\t} catch (S3Exception e) {\n" + "\t\t\tif (LOGGER.isTraceEnabled()) {\n"
				+ "\t\t\t\tLOGGER.trace(\"Exception when requesting S3: {}\", e.awsErrorDetails().errorCode(), e);\n"
				+ "\t\t\t} else {\n"
				+ "\t\t\t\tLOGGER.debug(\"Exception when requesting S3 for bucket: {}: [{}] {}\", bucket, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());\n"
				+ "\t\t\t}\n"
				+ "\t\t\t// \"PermanentRedirect\" means that the bucket is in different region than the defaultS3Client is configured for\n"
				+ "\t\t\tif (\"PermanentRedirect\".equals(e.awsErrorDetails().errorCode())) {\n"
				+ "\t\t\t\treturn fn.apply(bucketCache.get(bucket));\n" + "\t\t\t} else {\n" + "\t\t\t\tthrow e;\n"
				+ "\t\t\t}\n" + "\t\t}"));

		MethodDeclaration resolveBucketRegion = crossRegionS3Client.addMethod("resolveBucketRegion",
				Modifier.Keyword.PRIVATE);
		resolveBucketRegion.addParameter("String", "bucket");
		resolveBucketRegion.setType(Region.class);

		resolveBucketRegion.setBody(new BlockStmt()
				.addStatement("LOGGER.debug(\"Resolving region for bucket {}\", bucket);")
				.addStatement(
						"\t\tString bucketLocation = defaultS3Client.getBucketLocation(request -> request.bucket(bucket)).locationConstraintAsString();\n")
				.addStatement(
						"\t\tRegion region = StringUtils.hasLength(bucketLocation) ? Region.of(bucketLocation) : Region.US_EAST_1;\n")
				.addStatement("\t\tLOGGER.debug(\"Region for bucket {} is {}\", bucket, region);\n")
				.addStatement("\t\treturn region;"));

		MethodDeclaration serviceName = crossRegionS3Client.addMethod("serviceName", Modifier.Keyword.PUBLIC);
		serviceName.setType(String.class);
		serviceName.addMarkerAnnotation(Override.class);
		serviceName.setBody(new BlockStmt().addStatement("return S3Client.SERVICE_NAME;"));

		MethodDeclaration close = crossRegionS3Client.addMethod("close", Modifier.Keyword.PUBLIC);
		close.addMarkerAnnotation(Override.class);
		close.setBody(new BlockStmt().addStatement("this.clientCache.values().forEach(SdkAutoCloseable::close);"));

		final Path generatedJavaCcRoot = Paths.get(args[0], "..", "spring-cloud-aws-s3", "src", "main", "java", "io",
				"awspring", "cloud", "s3", "CrossRegionS3Client.java");

		Files.write(generatedJavaCcRoot, Arrays.asList(compilationUnit.toString()));
	}

}
