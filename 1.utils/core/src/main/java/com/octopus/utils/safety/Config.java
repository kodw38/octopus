package com.octopus.utils.safety;

import com.octopus.utils.safety.impl.DefaultHash256Encrypt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;


public class Config {
	private static transient Log log = LogFactory.getLog(Config.class);
	private static volatile Config instance = null;
	Properties config;

	public static Config getInstance() {
		if (instance == null) {
				if (instance == null) {
					instance = new Config();
				}

		}
		return instance;
	}

	private Config() {
		String filepath = "encryption.properties";
		InputStream input = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(filepath);
		config = new Properties();
		try {
			config.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				input = null;
			}
		}
	}

	public IEncrypt getDefaultEncrypt() {
		return getSomeEncrypt("default");
	}

	public IEncrypt getEncrypt(String type) {
		return getSomeEncrypt(type);
	}
	
	IEncrypt getSomeEncrypt(String type) {
		Properties p = getEncryptCofnig(type);
		if (null != p) {
			String impl = "secure.encrypt.impl";
			IEncrypt ret = null;
			if (p.containsKey(impl)) {
				try {
					ret = (IEncrypt) Class.forName(p.getProperty(impl))
							.getConstructor(Properties.class).newInstance(p);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				ret = new DefaultHash256Encrypt(p);
			}
			return ret;
		}
		return null;
	}

	Properties getEncryptCofnig(String type) {
		if (null != config) {
			Iterator its = config.keySet().iterator();
			String item;
			Properties ret = new Properties();
			while (its.hasNext()) {
				item = (String) its.next();
				if (item.startsWith(type + ".")) {
					ret.put(item.substring(type.length() + 1), config.get(item));
				}
			}
			return ret;
		}
		return null;
	}

}
