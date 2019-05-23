package org.ironrhino.core.security.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class RSAUtilsTest {

	public static final String PUBLIC_KEY = 
			"-----BEGIN PUBLIC KEY-----\n"+
			"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCyQUq3Kj6pJfPkAACsoF4laMzq\n" + 
			"ZCej+2lIlW64ZKi1jVL8/xenrpCpnh2qBvP6y8Kx66WbwoS//8R8hKkRiHgRkEkx\n" + 
			"8WRLYxfJiTIi/gWC08XbPUmQq3otFFHFnJ6dDoggn2ts3AeIu8Ag2iFv821bqc2y\n" + 
			"QcOlUY7HFVOQfPcMoQIDAQAB\n"+
			"-----END PUBLIC KEY-----";
	public static final String PRIVATE_KEY = 
			"-----BEGIN PRIVATE KEY-----\n"+
			"MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALJBSrcqPqkl8+QA\n" + 
			"AKygXiVozOpkJ6P7aUiVbrhkqLWNUvz/F6eukKmeHaoG8/rLwrHrpZvChL//xHyE\n" + 
			"qRGIeBGQSTHxZEtjF8mJMiL+BYLTxds9SZCrei0UUcWcnp0OiCCfa2zcB4i7wCDa\n" + 
			"IW/zbVupzbJBw6VRjscVU5B89wyhAgMBAAECgYAdwuT0m+sGVr3XrWBvcf8GA+9i\n" + 
			"mwI7ULiNU9W+l5/LYCMg9n4+kti4WRvezXZiwy5ogk2OKfX8EHn/yC5qLPzOulSI\n" + 
			"JD3ipQtEdcctKAYKuiM4pCKxbKTS+4xhRLGVIdkTdemv1XIZTvMvQbku7beLHKaZ\n" + 
			"iwZ+ILdAGBWKcDMq2QJBAOfDN8/AZ8VcqFyuNEmYHpAs85ZjthY4atHeCqtZXWXY\n" + 
			"Khzne6gksVRoCNmRYY2uf5rM4gelI9SRuzzPuJpatusCQQDE5Y/9GEmN9s0cVXIp\n" + 
			"qRS2kR6B5pzhMdu0r8rToZLeZmAAx9f0h/QQEK8eo8mLiuahDF3UNHidwe3GIlzr\n" + 
			"4n+jAkEA5czFjDNFMYZcUflRUx+IPoCzzoxzwbiTAiDeB2SGnTMnHp3QGLq5Me4t\n" + 
			"yDzEs80wLXe1LstpqZ4OP4/fhP0pnQJAdo1SWS3ae+PyM/Euv+7STeqq18QnCWCf\n" + 
			"zPdbxHkwmUBC5bVuTgnd5h5lkqlDnQWRP77WcAL37OTrabUfBdhmyQJAVWhO72t3\n" + 
			"ISuLYaDhyQ98jnrJDhgYt1GCzwLrkkOlPhsOhfpX6NFgr3UrIZnAjKd9F3MCPmA1\n" + 
			"AnVrwY9YU+ZqoQ==\n" + 
			"-----END PRIVATE KEY-----";

	@Test
	public void testEncrypt() throws Exception {
		String data = "I am test";
		String encryptedData = RSAUtils.encrypt(data, PUBLIC_KEY);
		assertThat(encryptedData, is(not(data)));
		String decryptedData = RSAUtils.decrypt(encryptedData, PRIVATE_KEY);
		assertThat(decryptedData, is(data));
	}

	@Test
	public void testSign() throws Exception {
		String data = "I am test";
		String sign = RSAUtils.sign(data, PRIVATE_KEY);
		assertThat(RSAUtils.verify(data, PUBLIC_KEY, sign), is(true));
	}
}
