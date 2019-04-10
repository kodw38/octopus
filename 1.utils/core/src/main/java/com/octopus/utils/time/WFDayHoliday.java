package com.octopus.utils.time;

import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * �ڼ���ʱ�޽ӿ�
 * </p>
 * @author �¾�
 * @Date 2010-06-09
 * <p>
 * 2010-6-10 ½���� 
 * 				����calculate�ӿ�,���ڼ��㹤��ʱ��
 * </p>
 */
public class WFDayHoliday implements WFDay {
	private int 	day=-1;
	private Date	date=null;
	public WFDayHoliday(int day) {
		this.day=day;
	}
	public WFDayHoliday(Date	date){
		this.date=date;
	}
	
	public boolean adjust(WFTime wfTime) throws Exception {
		if ((this.day>=1 && this.day==wfTime.getDayOfWeek()) || (this.date!=null && wfTime.isSpecialDate(this.date))){
			wfTime.add(Calendar.SECOND,WFTime.SECONDS_OF_DAY- wfTime.getSecondOfDay());
			return true;
		}else{
			return false;
		}
	}
	
	public boolean calculate(WFTime wfTime, Calendar endDate) throws Exception {
		if ((this.day>=1 && this.day==wfTime.getDayOfWeek()) || (this.date!=null && wfTime.isSpecialDate(this.date))){
			wfTime.add(Calendar.SECOND,WFTime.SECONDS_OF_DAY- wfTime.getSecondOfDay());
			return true;
		}else{
			return false;
		}
	}
}
