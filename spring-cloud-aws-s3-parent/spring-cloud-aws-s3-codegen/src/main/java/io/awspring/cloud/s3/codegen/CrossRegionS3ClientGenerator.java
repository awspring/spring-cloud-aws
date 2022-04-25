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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Generates CrossRegionS3Client class from {@link CrossRegionS3ClientTemplate}.
 *
 * Generated methods wrap every bucket-specific {@link S3Client} method with
 * {@link CrossRegionS3ClientTemplate#executeInBucketRegion(String, Function)}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public final class CrossRegionS3ClientGenerator {

	private CrossRegionS3ClientGenerator() {

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new RuntimeException("Need 1 parameter: the JavaParser source checkout root directory.");
		}

		// load template class
		final Path source = Paths.get(args[0], "..", "spring-cloud-aws-s3-codegen", "src", "main", "java", "io",
				"awspring", "cloud", "s3", "codegen", "CrossRegionS3ClientTemplate.java");
		CompilationUnit compilationUnit = StaticJavaParser.parse(source);
		compilationUnit.setPackageDeclaration("io.awspring.cloud.s3.crossregion");
		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit
				.getClassByName("CrossRegionS3ClientTemplate")
				.orElseThrow(() -> new IllegalStateException("Class CrossRegionS3ClientTemplate not found"));

		// rename class and constructors
		classOrInterfaceDeclaration.setName("CrossRegionS3Client");
		classOrInterfaceDeclaration.getConstructors()
				.forEach(constructorDeclaration -> constructorDeclaration.setName("CrossRegionS3Client"));

		// add methods
		addOverriddenMethods(classOrInterfaceDeclaration);

		// generate target file
		final Path generatedJavaCcRoot = Paths.get(args[0], "..", "spring-cloud-aws-s3-cross-region-client", "src",
				"main", "java", "io", "awspring", "cloud", "s3", "crossregion", "CrossRegionS3Client.java");
		Files.write(generatedJavaCcRoot, Collections.singletonList(compilationUnit.toString()));
	}

	private static void addOverriddenMethods(ClassOrInterfaceDeclaration crossRegionS3Client) {
		TypeSolver typeSolver = new ClassLoaderTypeSolver(CrossRegionS3ClientGenerator.class.getClassLoader());
		ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = typeSolver
				.solveType(S3Client.class.getName());
		resolvedReferenceTypeDeclaration.getAllMethods().stream()
				.sorted(Comparator.comparing(MethodUsage::getSignature)).forEach(method -> {
					if (isRegionSpecific(method) && isCanonical(method)) {
						MethodDeclaration methodDeclaration = crossRegionS3Client.addMethod(method.getName(),
								Modifier.Keyword.PUBLIC);

						// collect & create method parameters
						List<String> parameterStrings = new ArrayList<>();
						for (int i = 0; i < method.getParamTypes().size(); i++) {
							methodDeclaration.addParameter(new Parameter(StaticJavaParser
									.parseClassOrInterfaceType(method.getParamTypes().get(i).describe()), "p" + i));
							parameterStrings.add("p" + i);
						}

						// handle generic types
						if (method.getDeclaration().getReturnType().isTypeVariable()) {
							methodDeclaration.addTypeParameter(method.getDeclaration().getReturnType().describe());
						}

						methodDeclaration.setType(StaticJavaParser
								.parseClassOrInterfaceType(method.getDeclaration().getReturnType().describe()));
						methodDeclaration.setBody(new BlockStmt()
								.addStatement("return executeInBucketRegion(p0.bucket(), s3Client -> s3Client."
										+ method.getName() + "(" + String.join(",", parameterStrings) + "));"));
						methodDeclaration.addMarkerAnnotation(Override.class);
						methodDeclaration.addThrownException(AwsServiceException.class);
						methodDeclaration.addThrownException(SdkClientException.class);
					}
				});
	}

	private static boolean isRegionSpecific(MethodUsage method) {
		return !Arrays.asList("listBuckets", "writeGetObjectResponse").contains(method.getName());
	}

	private static boolean isCanonical(MethodUsage method) {
		return method.getParamTypes().size() > 0 && method.getParamType(0).describe().endsWith("Request");
	}

}
