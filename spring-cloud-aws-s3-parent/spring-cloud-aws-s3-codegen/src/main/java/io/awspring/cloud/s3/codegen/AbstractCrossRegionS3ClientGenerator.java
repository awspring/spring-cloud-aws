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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Generates AbstractCrossRegionS3Client class from {@link AbstractCrossRegionS3ClientTemplate}.
 *
 * Generated methods wrap every bucket-specific {@link S3Client} method with
 * {@link AbstractCrossRegionS3ClientTemplate#executeInBucketRegion(String, Function)} and every bucket-independent
 * {@link S3Client} method with {@link AbstractCrossRegionS3ClientTemplate#executeInDefaultRegion(Function)}
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public final class AbstractCrossRegionS3ClientGenerator {

	private AbstractCrossRegionS3ClientGenerator() {

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Needs 1 parameter: the output directory");
		}

		// load template class
		CompilationUnit compilationUnit = StaticJavaParser
				.parseResource("io/awspring/cloud/s3/codegen/AbstractCrossRegionS3ClientTemplate.java");
		compilationUnit.setPackageDeclaration("io.awspring.cloud.s3.crossregion");
		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit
				.getClassByName("AbstractCrossRegionS3ClientTemplate")
				.orElseThrow(() -> new IllegalStateException("Class AbstractCrossRegionS3ClientTemplate not found"));

		// rename class and constructors
		classOrInterfaceDeclaration.setName("AbstractCrossRegionS3Client");
		classOrInterfaceDeclaration.getConstructors()
				.forEach(constructorDeclaration -> constructorDeclaration.setName("AbstractCrossRegionS3Client"));

		// add methods
		addOverriddenMethods(classOrInterfaceDeclaration);

		// generate target file
		String[] classPackage = classOrInterfaceDeclaration.getFullyQualifiedName()
				.orElseThrow(() -> new IllegalStateException("Couldn't get FQN from " + classOrInterfaceDeclaration))
				.split("\\.");
		classPackage[classPackage.length - 1] += ".java";
		final Path generatedJavaCcRoot = Paths.get(args[0], classPackage);
		Files.createDirectories(generatedJavaCcRoot.getParent());
		Files.write(generatedJavaCcRoot, Collections.singletonList(compilationUnit.toString()));
	}

	private static void addOverriddenMethods(ClassOrInterfaceDeclaration crossRegionS3Client) {
		TypeSolver typeSolver = new ClassLoaderTypeSolver(AbstractCrossRegionS3ClientGenerator.class.getClassLoader());
		ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = typeSolver
				.solveType(S3Client.class.getName());
		resolvedReferenceTypeDeclaration.getAllMethods().stream()
				.sorted(Comparator.comparing(MethodUsage::getSignature)).forEach(method -> {
					if (isCanonical(method)) {
						MethodDeclaration methodDeclaration = crossRegionS3Client.addMethod(method.getName(),
								Modifier.Keyword.PUBLIC);

						// collect & create method parameters
						for (int i = 0; i < method.getParamTypes().size(); i++) {
							methodDeclaration.addParameter(StaticJavaParser
									.parseClassOrInterfaceType(method.getParamTypes().get(i).describe()), "p" + i);
						}

						// handle generic types
						if (method.getDeclaration().getReturnType().isTypeVariable()) {
							methodDeclaration.addTypeParameter(method.getDeclaration().getReturnType().describe());
						}

						methodDeclaration.setType(StaticJavaParser
								.parseClassOrInterfaceType(method.getDeclaration().getReturnType().describe()));
						methodDeclaration.setBody(getMethodBody(method, methodDeclaration));
						methodDeclaration.addMarkerAnnotation(Override.class);
						methodDeclaration.addThrownException(AwsServiceException.class);
						methodDeclaration.addThrownException(SdkClientException.class);
					}
				});
	}

	private static BlockStmt getMethodBody(MethodUsage method, MethodDeclaration methodDeclaration) {
		BlockStmt body = new BlockStmt();
		String params = methodDeclaration.getParameters().stream().map(p -> p.getName().asString())
				.collect(Collectors.joining(","));
		if (isRegionSpecific(method)) {
			body.addStatement("return executeInBucketRegion(" + methodDeclaration.getParameter(0).getName().asString()
					+ ".bucket(), s3Client -> s3Client." + method.getName() + "(" + params + "));");
		}
		else {
			body.addStatement(
					"return executeInDefaultRegion(s3Client -> s3Client." + method.getName() + "(" + params + "));");
		}
		return body;
	}

	private static boolean isRegionSpecific(MethodUsage method) {
		return method.getDeclaration().getParam(0).getType().asReferenceType().getDeclaredMethods().stream()
				.anyMatch(mu -> mu.getName().equals("bucket"));
	}

	private static boolean isCanonical(MethodUsage method) {
		return !method.getParamTypes().isEmpty() && method.getParamType(0).describe().endsWith("Request");
	}

}
