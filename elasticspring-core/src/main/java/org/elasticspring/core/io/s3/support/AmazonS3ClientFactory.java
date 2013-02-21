/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.io.pem.PemObject;
import org.elasticspring.core.region.ServiceEndpoint;
import org.springframework.core.io.Resource;

import javax.crypto.SecretKey;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that returns the corresponding {@link AmazonS3Client} based
 * on the {@link org.elasticspring.core.io.s3.S3ServiceEndpoint}. The {@link AmazonS3Client} are cached so that
 * at most one instance is create per {@link org.elasticspring.core.io.s3.S3ServiceEndpoint}.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AmazonS3ClientFactory {

	private final AWSCredentialsProvider credentials;
	private final ConcurrentHashMap<ServiceEndpoint, AmazonS3Client> clientsForRegion = new ConcurrentHashMap<ServiceEndpoint, AmazonS3Client>();
	private boolean anonymous;
	private KeyPair keyPairRef;
	private SecretKey symmetricKeyRef;
	private Resource privateKeyResource;
	private Resource publicKeyResource;
	private Resource symmetricKeyResource;

	public AmazonS3ClientFactory(AWSCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	public boolean isAnonymous() {
		return this.anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public KeyPair getKeyPairRef() {
		return this.keyPairRef;
	}

	public void setKeyPairRef(KeyPair keyPairRef) {
		this.keyPairRef = keyPairRef;
	}

	public SecretKey getSymmetricKeyRef() {
		return this.symmetricKeyRef;
	}

	public void setSymmetricKeyRef(SecretKey symmetricKeyRef) {
		this.symmetricKeyRef = symmetricKeyRef;
	}

	public Resource getPrivateKeyResource() {
		return this.privateKeyResource;
	}

	public void setPrivateKeyResource(Resource privateKeyResource) {
		this.privateKeyResource = privateKeyResource;
	}

	public Resource getPublicKeyResource() {
		return this.publicKeyResource;
	}

	public void setPublicKeyResource(Resource publicKeyResource) {
		this.publicKeyResource = publicKeyResource;
	}

	public Resource getSymmetricKeyResource() {
		return this.symmetricKeyResource;
	}

	public void setSymmetricKeyResource(Resource symmetricKeyResource) {
		this.symmetricKeyResource = symmetricKeyResource;
	}

	/**
	 * Method that returns the corresponding {@link AmazonS3} client based
	 * on the {@link org.elasticspring.core.region.ServiceEndpoint}.
	 *
	 * @param s3ServiceEndpoint
	 * 		the {@link org.elasticspring.core.region.ServiceEndpoint} that the client must access.
	 * @return the corresponding {@link AmazonS3} client.
	 */
	public AmazonS3 getClientForServiceEndpoint(ServiceEndpoint s3ServiceEndpoint) {
		AmazonS3Client cachedAmazonS3Client = this.clientsForRegion.get(s3ServiceEndpoint);
		if (cachedAmazonS3Client != null) {
			return cachedAmazonS3Client;
		} else {
			if (isEncryptionClient()) {
				return createAmazonS3EncryptionClient(s3ServiceEndpoint);
			} else {
				AmazonS3Client amazonS3Client = new AmazonS3Client(this.credentials.getCredentials());
				amazonS3Client.setEndpoint(s3ServiceEndpoint.getEndpoint());
				AmazonS3Client previousValue = this.clientsForRegion.putIfAbsent(s3ServiceEndpoint, amazonS3Client);
				return previousValue == null ? amazonS3Client : previousValue;
			}
		}
	}

	private AmazonS3 createAmazonS3EncryptionClient(ServiceEndpoint s3ServiceEndpoint) {
		EncryptionMaterials encryptionMaterials = null;
		if (this.keyPairRef != null) {
			encryptionMaterials = new EncryptionMaterials(this.keyPairRef);
		}

		if (this.privateKeyResource != null && this.privateKeyResource.exists()) {
			try {
				KeyPair keyPair = createKeyPair();
				encryptionMaterials = new EncryptionMaterials(keyPair);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (this.symmetricKeyRef != null) {
			encryptionMaterials = new EncryptionMaterials(this.symmetricKeyRef);
		}

		// TODO read key resource and create a SecretKey

		AmazonS3EncryptionClient amazonS3EncryptionClient;
		if (this.anonymous) {
			amazonS3EncryptionClient = new AmazonS3EncryptionClient(encryptionMaterials);
		} else {
			amazonS3EncryptionClient = new AmazonS3EncryptionClient(this.credentials.getCredentials(), encryptionMaterials);
		}

		AmazonS3Client previousValue = this.clientsForRegion.putIfAbsent(s3ServiceEndpoint, amazonS3EncryptionClient);
		return previousValue == null ? amazonS3EncryptionClient : previousValue;
	}

	private boolean isEncryptionClient() {
		return this.anonymous || this.keyPairRef != null || (this.privateKeyResource != null && this.privateKeyResource.exists()) ||
				this.symmetricKeyRef != null || (this.symmetricKeyResource != null && this.symmetricKeyResource.exists());
	}

	private KeyPair createKeyPair() {
		if (!this.publicKeyResource.exists() || !this.privateKeyResource.exists()) {
			// TODO throw an appropriate exception...
		}

		Security.addProvider(new BouncyCastleProvider());
		try {
			PublicKey publicKey = createPublicKey();
			PrivateKey privateKey = createPrivateKey();
			return new KeyPair(publicKey, privateKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private PrivateKey createPrivateKey() throws IOException {
		FileReader fileReader = null;
		PEMReader pemReader = null;
		ASN1InputStream asn1InputStream = null;
		try {
			fileReader = new FileReader(this.getPrivateKeyResource().getFile());
			pemReader = new PEMReader(fileReader);
			PemObject pemObject = pemReader.readPemObject();
			asn1InputStream = new ASN1InputStream(pemObject.getContent());
			PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, new DERNull()), asn1InputStream.readObject());
			RSAKeyParameters rsaPrivateParams = (RSAKeyParameters) PrivateKeyFactory.createKey(privateKeyInfo);
			RSAPrivateKeySpec rsaPrivateSpec = new RSAPrivateKeySpec(rsaPrivateParams.getModulus(), rsaPrivateParams.getExponent());
			return KeyFactory.getInstance("RSA").generatePrivate(rsaPrivateSpec);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} finally {
			if (pemReader != null) {
				pemReader.close();
			}
			if (fileReader != null) {
				fileReader.close();
			}
			if (asn1InputStream != null) {
				asn1InputStream.close();
			}
		}
		return null;
	}

	private PublicKey createPublicKey() throws IOException {
		FileReader fileReader = null;
		PEMReader pemReader = null;
		try {
			fileReader = new FileReader(this.getPublicKeyResource().getFile());
			pemReader = new PEMReader(fileReader);
			PemObject pemObject = pemReader.readPemObject();
			SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence) ASN1Object.fromByteArray(pemObject.getContent()));

			RSAKeyParameters rsaPublicParams = (RSAKeyParameters) PublicKeyFactory.createKey(subjectPublicKeyInfo);
			RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(rsaPublicParams.getModulus(), rsaPublicParams.getExponent());
			return KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} finally {
			if (pemReader != null) {
				pemReader.close();
			}
			if (fileReader != null) {
				fileReader.close();
			}
		}
		return null;
	}
}