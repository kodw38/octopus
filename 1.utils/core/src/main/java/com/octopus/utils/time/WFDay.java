package com.octopus.utils.time;

import java.util.Calendar;

/**
 * <p>
 * ����ʱ�޽ӿ�
 * </p>
 * @author �¾�
 * @Date 2010-06-09
 * <p>
 * 2010-6-10 ½���� 
 * 				����calculate�ӿ�,���ڼ��㹤��ʱ��
 * </p>
 */
public interface WFDay {
	
	/**
	 * ������ʱ��
	 * 
	 * @param cTime
	 * @return
	 * @throws Exception
	 */
	public 	boolean	adjust(WFTime cTime) throws Exception;	
	
	/**
	 * ����ʱ��
	 * 
	 * @param cTime
	 * @param endDate ��������
	 * @return
	 * @throws Exception
	 */
	public boolean calculate(WFTime cTime, Calendar endDate) throws Exception;
}
