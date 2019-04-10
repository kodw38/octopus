package com.octopus.utils.safety.impl;

import com.octopus.utils.safety.IEncrypt;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;


public class DefaultHash256Encrypt implements IEncrypt {
	private static transient Log log = LogFactory.getLog(DefaultHash256Encrypt.class);
	private Properties properties;
	private int saltSize = 32; //默认的的大小

	public DefaultHash256Encrypt() {
	}

	public DefaultHash256Encrypt(Properties p) {
		this.properties = p;
	}

	public String encrypt(String plain, String salt) throws Exception {
		if (StringUtils.isBlank(plain)) {
			throw new IllegalArgumentException(
					"The text for encrypt should not be blank.");
		}

		if (StringUtils.isBlank(salt)) {
			throw new IllegalArgumentException(
					"The salt for encrypt should not be blank.");
		}

		String result = null;
		byte[] bytes = null;
		String hashAlgorithm = getStringValue("hash.secure.algorithm",
				this.properties);
		String encoding = getStringValue("hash.secure.encoding", this.properties);
		byte[] decodeHex = Hex.decodeHex(salt.toCharArray());
		try {
			MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
			md.update(decodeHex);
			bytes = md.digest(plain.getBytes(encoding));
			result = new String(Hex.encodeHex(bytes));
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException("Can't find hash algorithm "
					+ hashAlgorithm, e);
		} catch (UnsupportedEncodingException ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException("Can't find encoding for " + encoding,
					ex);
		}

		return result;
	}

	private String getStringValue(String key, Properties properties) {
		String value = (String) properties.get(key);
		if (StringUtils.isNotBlank(value)) {
			value = value.trim();
		}
		return value;
	}

	public String getSalt() {
		SecureRandom random = new SecureRandom();
		String saltSizeString = getStringValue("hash.salt.size", properties);
		int saltSizeInt =0;
		if (StringUtils.isNotBlank(saltSizeString)) {
			try {
				saltSizeInt = Integer.parseInt(saltSizeString);
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("default.salt.size config error,can not support "
							+ saltSizeString + ",will use the default value.");
				}
			}
		} 
		if(saltSize>0){
			this.saltSize = saltSizeInt;
		}
		byte[] salt = new byte[this.saltSize];
		random.nextBytes(salt);
		return new String(Hex.encodeHex(salt));
	}

	public String decrypt(String encrypt) throws Exception {
		throw new Exception("Not Support decrypt(String encrypt).");
	}

	public String decrypt(String plain, String salt) throws Exception {
		throw new Exception("Not Support decrypt(String plain, String salt).");
	}
	
	public String encrypt(String plain) throws Exception {
		throw new Exception("Not Support encrypt(String plain).");
	}
}
