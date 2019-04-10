package com.octopus.utils.safety;

public interface IEncrypt {
	public String encrypt(String plain) throws Exception;

	public String decrypt(String plain) throws Exception;

	public String encrypt(String plain, String salt) throws Exception;

	public String decrypt(String plain, String salt) throws Exception;

	public String getSalt() throws Exception;
}
