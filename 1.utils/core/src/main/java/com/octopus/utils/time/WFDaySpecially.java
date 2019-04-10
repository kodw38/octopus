package com.octopus.utils.time;

import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * ��������ʱ�޽ӿ�(��5.1�ڼ��յ���)
 * </p>
 * @author �¾�
 * @Date 2010-06-09
 * <p>
 * 2010-6-10 ½���� 
 * 				����calculate�ӿ�,���ڼ��㹤��ʱ��
 * </p>
 */
public class WFDaySpecially implements WFDay{
	private	Date			date=null;
	private WFDayWorking	rTimeWorking=null;
	
	public WFDaySpecially(Date date,WFDayWorking rTimeWorking){
		this.date=date;
		this.rTimeWorking=rTimeWorking;
	}
	
	public boolean adjust(WFTime wfTime) throws Exception {
		if (this.date!=null && this.rTimeWorking!=null && wfTime.isSpecialDate(this.date)){
			return this.rTimeWorking.adjust(wfTime);
		}else{
			return false;
		}
	}

	public boolean calculate(WFTime wfTime, Calendar endDate) throws Exception {
		if (this.date!=null && this.rTimeWorking!=null && wfTime.isSpecialDate(this.date)){
			return this.rTimeWorking.calculate(wfTime, endDate);
		}else{
			return false;
		}
	}
}
