package com.octopus.utils.safety;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Date;

public class Security {

  public static void main( String[] args ) throws Exception{
    Security INSTANCE = new Security();
    String tt = "13838820885|TESTUSER|TT|" + new Date().getTime();
    System.out.println( INSTANCE.getEnCode( tt ) );
    System.out.println( INSTANCE.getUnCode( INSTANCE.getEnCode( tt ) ) );
    RC2 r = new RC2();
      String s = r.encrypt("9ol.(OL>");
      System.out.println(s);
  }

  public String getRealStr( String code ) throws Exception{
    BASE64Decoder decoder = new BASE64Decoder();
    byte[] b = decoder.decodeBuffer( code );
    code = new String( b );
    String[] temp = null;
    temp = code.split( "&" );
    byte[] byt = new byte[temp.length];

    for( int i = 0; i < byt.length; i++ ){
      byt[i] = (byte) Integer.parseInt( temp[i] );
    }

    byte[] key = new byte[] { 23, 14, 81, 19, 21, 61, 61, 79 };
    byte[] res = decrypt( byt, key );

    byte[] s = new byte[res.length / 2];
    for( int i = 0; i < s.length; i++ ){
      s[i] = res[i * 2];
    }
    System.out.println( "====================================" );
    System.out.println( "URL��ꑣ�ԭʼ���a=" + code );
    System.out.println( "URL��ꑣ����ܺ�ľ��a=" + new String( s ) );
    System.out.println( "====================================" );
    return new String( s );
  }

  public String getEnCode( String str ) throws Exception{
    byte[] temp = null;
    try{
      temp = str.getBytes( "utf-8" );
    }catch( UnsupportedEncodingException e ){
    }
    byte[] code = new byte[temp.length * 2];

    for( int i = 0; i < temp.length; i++ ){
      int radom = (int) (1 + Math.random() * (10 - 1 + 1));

      code[i + i] = temp[i];
      code[i + i + 1] = (byte) radom;
    }
    byte[] key = new byte[] { 23, 14, 81, 19, 21, 61, 61, 79 };

    byte[] sec = encrypt( code, key );

    StringBuffer t = new StringBuffer();
    for( int i = 0; i < sec.length; i++ ){
      t.append( sec[i] + "&" );
    }

    String result = t.toString();

    result = new sun.misc.BASE64Encoder().encode( result.getBytes() );
    result = result.replaceAll( "\r\n", "" );
    return result.replaceAll( "\n", "" );
  }

  public boolean UnCodeCheck( String code ) throws Exception{
    try{
      System.out.println( "====================================" );
      System.out.println( "URL��ꑣ�ԭʼ���a=" + code );
      BASE64Decoder decoder = new BASE64Decoder();
      byte[] b = decoder.decodeBuffer( code );
      code = new String( b );
      String[] temp = null;
      temp = code.split( "&" );
      byte[] byt = new byte[temp.length];

      for( int i = 0; i < byt.length; i++ ){
        byt[i] = (byte) Integer.parseInt( temp[i] );
      }

      byte[] key = new byte[] { 23, 14, 81, 19, 21, 61, 61, 79 };
      byte[] res = decrypt( byt, key );

      byte[] s = new byte[res.length / 2];
      for( int i = 0; i < s.length; i++ ){
        s[i] = res[i * 2];
      }
      StringBuffer t = new StringBuffer();

      t.append( new String( s ) );

      String[] param = t.toString().split( "\\|" );

      System.out.println( "URL��ꑣ����ܺ�ľ��a=" + t.toString() );

/*
      String time = param[param.length - 1];
      Date date = new Date( Long.parseLong( time ) );
      Date big = new Date( new Date().getTime() + 60 * 1000 * 15 );
      Date small = new Date( new Date().getTime() - 60 * 1000 * 15 );
      if( big.after( date ) && small.before( date ) ){
        System.out.println( "URL��ꑣ���C�ܴa��B:true ͨ�^��C" );
        System.out.println( "====================================" );
        return true;
      }
*/
        return true;
    }catch( Throwable e ){
      System.out.println( "URL��ꑣ���C�ܴa��B:false ��Cʧ�������ܴ����Ϸ�" );
      System.out.println( "====================================" );
      return false;
    }
/*
    System.out.println( "URL��ꑣ���C�ܴa��B:false ��Cʧ�������ܴ��^��" );
    System.out.println( "====================================" );
    return false;
*/

  }

  public String getUnCode( String code ) throws Exception{

    BASE64Decoder decoder = new BASE64Decoder();
    byte[] b = decoder.decodeBuffer( code );
    code = new String( b );
    String[] temp = null;
    temp = code.split( "&" );
    byte[] byt = new byte[temp.length];

    for( int i = 0; i < byt.length; i++ ){
      byt[i] = (byte) Integer.parseInt( temp[i] );
    }

    byte[] key = new byte[] { 23, 14, 81, 19, 21, 61, 61, 79 };
    byte[] res = decrypt( byt, key );

    byte[] s = new byte[res.length / 2];
    for( int i = 0; i < s.length; i++ ){
      s[i] = res[i * 2];
    }

    return new String( s );
  }

  private final static String DES = "DES";

  public static byte[] encrypt( byte[] src, byte[] key ) throws Exception{

    // DES�㷨Ҫ����һ�������ε������Դ

    SecureRandom sr = new SecureRandom();

    // ��ԭʼ�ܳ���ݴ���DESKeySpec����

    DESKeySpec dks = new DESKeySpec( key );

    // ����һ���ܳ׹�����Ȼ�������DESKeySpecת����

    // һ��SecretKey����

    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( DES );

    SecretKey securekey = keyFactory.generateSecret( dks );

    // Cipher����ʵ����ɼ��ܲ���

    Cipher cipher = Cipher.getInstance( DES );

    // ���ܳ׳�ʼ��Cipher����

    cipher.init( Cipher.ENCRYPT_MODE, securekey, sr );

    // ���ڣ���ȡ��ݲ�����

    // ��ʽִ�м��ܲ���

    return cipher.doFinal( src );

  }

  /**
   * ����
   *
   * @param src ���Դ
   * @param key ��Կ�����ȱ�����8�ı���
   * @return ���ؽ��ܺ��ԭʼ���
   * @throws Exception
   */

  public static byte[] decrypt( byte[] src, byte[] key ) throws Exception{

    // DES�㷨Ҫ����һ�������ε������Դ

    SecureRandom sr = new SecureRandom();

    // ��ԭʼ�ܳ���ݴ���һ��DESKeySpec����

    DESKeySpec dks = new DESKeySpec( key );

    // ����һ���ܳ׹�����Ȼ�������DESKeySpec����ת����

    // һ��SecretKey����

    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( DES );

    SecretKey securekey = keyFactory.generateSecret( dks );

    // Cipher����ʵ����ɽ��ܲ���

    Cipher cipher = Cipher.getInstance( DES );

    // ���ܳ׳�ʼ��Cipher����

    cipher.init( Cipher.DECRYPT_MODE, securekey, sr );

    // ���ڣ���ȡ��ݲ�����

    // ��ʽִ�н��ܲ���

    return cipher.doFinal( src );

  }

    static MD5 m = new MD5();
    public static  String encryptMD5(String str){
        return m.toDigest(str);
    }
    static MD5_2 m2 = new MD5_2();
    public static String encryptMD5_2(String str){
        return m2.toDigest(str);
    }
    static RC2 rc2 = new RC2();
    public static  String encryptRC2(String str){
        try {
            return rc2.encrypt(str);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public static  String decryptRC2(String str){
        try {
            return rc2.decrypt(str);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}