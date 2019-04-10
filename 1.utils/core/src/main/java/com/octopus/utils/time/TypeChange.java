package com.octopus.utils.time;

/**
 * <p>Title: Asiainfo Portal System</p>
 * <p>Description: </p>
 * <p>����ת������������</p>
 * <p>�����ṩ�˸�java����ת���ȹ��ܡ�</p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: Asiainfo Technologies (China),Inc.HangZhou</p>
 * @author Asiainfo PSO/yuanjq
 * @version 1.0
 */
public class TypeChange {
    /**
     * ͨ���ַ�ת������Ӧ��DOUBLE�������ء�
     * @param strValue String ��ת�����ַ�
     * @return double ת����ɵ�DOUBLE
     * */
    public static double getStrToDouble(String strValue) {
        if (null == strValue) {
            return 0;
        }
        double dValue = 0;
        try {
            dValue = Double.parseDouble(strValue.trim());
        } catch (Exception ex) {
            dValue = 0;
        }
        return dValue;
    }

    /**
     * ͨ���ַ�ת������Ӧ�����ͣ������ء�
     * FrameWorkʹ��
     * @param strValue String ��ת�����ַ�
     * @return int ת����ɵ�����
     * */
    public static int getStrToInt(String strValue) {
        int iValue = 0;
        if (null == strValue) {
            return iValue;
        }
        try {
            iValue = new Integer(strValue.trim()).intValue();
        } catch (Exception ex) {
            iValue = 0;
        }
        return iValue;
    }

    /**
     * ͨ���ַ�ת������Ӧ�Ķ����ͣ������ء�
     * FrameWorkʹ��
     * @param strValue String ��ת�����ַ�
     * @return short ת����ɵĶ�����
     * */
    public static short getStrToShort(String strValue) {
        short iValue = 0;
        if (null == strValue) {
            return iValue;
        }
        try {
            iValue = new Short(strValue.trim()).shortValue();
        } catch (Exception ex) {
            iValue = 0;
        }
        return iValue;
    }

    /**
     * ͨ���ַ�ת������Ӧ�ĳ����ͣ������ء�
     * @param strValue String ��ת�����ַ�
     * @return long ת����ɵĳ�����
     * */
    public static long getStrToLong(String strValue) {
        if (null == strValue) {
            return 0;
        }
        long lValue = 0;
        try {
            lValue = new Long(strValue.trim()).longValue();
        } catch (Exception ex) {
            lValue = 0;
        }
        return lValue;
    }

    /**
     * ��intתΪString
     * @param i int
     * @return String
     */
    public static String getIntToStr(int i) {
        return new Integer(i).toString();
    }

    /**
     * ��doubleתΪString
     * @param d double
     * @return String
     */
    public static String getDoubleToStr(double d) {
        return new Double(d).toString();
    }

    /**
     * ��longתΪString
     * @param l long
     * @return String
     */
    public static String getLongToStr(long l) {
        return new Long(l).toString();
    }
    
    /**
     * ��longתΪint
     * @param l long
     * @return int
     */
    public static int getLongToInt(long l) {
        return new Long(l).intValue();
    }

    /**
     * ��shortתΪString
     * @param n short
     * @return String
     */
    public static String getShortToStr(short n) {
        return new Short(n).toString();
    }

    /**
     * ��Object����ת��String����
     * @param seqObj Object[]
     * @return String[]
     */
    public static String[] getObjArrayToStringArray(Object[] seqObj) {
        String[] seqStr = new String[seqObj.length];
        for (int i = 0; i < seqObj.length; i++) {
            seqStr[i] = seqObj[i].toString();
        }
        return seqStr;
    }
}
