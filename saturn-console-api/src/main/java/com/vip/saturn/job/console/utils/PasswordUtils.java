package com.vip.saturn.job.console.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class PasswordUtils {

	public static final String HASH_METHOD_PLANTEXT = "plaintext";

	public static final String HASH_METHOD_PBKDF2 = "PBKDF2WithHmacSHA1";

	private static final Logger log = LoggerFactory.getLogger(PasswordUtils.class);

	private static final int ITERATIONS = 10 * 1000;

	private static final int SALT_LEN = 8;

	private static final int KEY_LEN = 256;

	/**
	 * 生成带盐的密码串，密码和盐使用'$'符号分隔。
	 * @param hashMethod 为JDK SecretKeyFactory支持的算法，如果算法不存在，会使用PBKDF2WithHmacSHA1
	 */
	public static String genPassword(String password, String hashMethod) throws Exception {
		byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(SALT_LEN);
		return hash(password, salt, hashMethod);
	}

	public static String genPassword(String password, byte[] salt, String hashMethod) throws Exception {
		return hash(password, salt, hashMethod) + "$" + Hex.encodeHexString(salt);
	}

	public static String hash(String password, byte[] salt, String hashMethod) throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory secretKeyFactory;
		try {
			secretKeyFactory = SecretKeyFactory.getInstance(hashMethod);
		} catch (NoSuchAlgorithmException e) {
			secretKeyFactory = SecretKeyFactory.getInstance(HASH_METHOD_PBKDF2);
		}

		SecretKey key = secretKeyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN));
		return Hex.encodeHexString(key.getEncoded());
	}

	public static boolean validate(String password, String passwordInDB, String hashMethod) {
		if (PasswordUtils.HASH_METHOD_PLANTEXT.equals(hashMethod)) {
			return password.equals(passwordInDB);
		}

		String[] saltAndPassword = passwordInDB.split("\\$");
		if (saltAndPassword.length != 2) {
			log.debug("malformed password in db");
			return false;
		}

		String hashOfRequestPassword = null;
		try {
			hashOfRequestPassword = hash(password, getSalt(saltAndPassword[1]), hashMethod);
		} catch (Exception e) {
			return false;
		}
		return hashOfRequestPassword.equals(new String(saltAndPassword[0]));
	}

	private static byte[] getSalt(String s) throws DecoderException {
		return Hex.decodeHex(s.toCharArray());
	}

}
