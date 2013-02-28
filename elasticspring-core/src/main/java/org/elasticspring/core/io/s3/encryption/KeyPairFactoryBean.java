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

package org.elasticspring.core.io.s3.encryption;

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
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

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

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class KeyPairFactoryBean extends AbstractFactoryBean<KeyPair> {

	private final Resource privateKey;
	private final Resource publicKey;

	public KeyPairFactoryBean(Resource privateKey, Resource publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(this.privateKey != null && this.privateKey.exists(), "Private key resource does not exist.");
		Assert.isTrue(this.publicKey != null && this.publicKey.exists(), "Public key resource does not exist.");
		super.afterPropertiesSet();
	}

	@Override
	public Class<?> getObjectType() {
		return KeyPair.class;
	}

	@Override
	protected KeyPair createInstance() throws Exception {
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
			fileReader = new FileReader(this.privateKey.getFile());
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
			fileReader = new FileReader(this.publicKey.getFile());
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
