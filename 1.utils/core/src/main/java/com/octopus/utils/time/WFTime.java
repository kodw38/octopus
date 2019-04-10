package com.octopus.utils.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * ʱ�޼���
 * </p>
 * @author �¾�
 * <p>
 * 2010-06-10 ½����
 * 			 ����calculateLimitTime����,���ڼ�����ʼʱ��������ʱ��Ĺ���ʱ�� 
 * 			 ����compare����,���ڱȽϵ�ǰʱ����Ŀ��ʱ��
 * 			 ����addTimeOut����,��������ʱ��
 * 			 ����ǩ��,��adjust(ArrayList<WFDay> listDays) �޸�Ϊ getLimitTime(ArrayList<WFDay> listDays)
 * 			 ��ȡ����createLimitCfg(List<WFDay> listDays)
 * </p>
 * @Date 2010-06-09
 */
public class WFTime {
	public static final int	SECONDS_OF_DAY=86399;
	public static final int	MAX_RECURSIVE_TIME=100;
	
	private Calendar	date=null;
	private int			timeout;
	private int recursiveTime=0;
	private boolean isjudge = false;
	
	public WFTime(Date date,int	timeout){
		this.date=Calendar.getInstance();		
		this.date.setTime(date);
		this.timeout=timeout;
	}
	
	public WFTime(Date date){
		this.date=Calendar.getInstance();
		this.date.setTime(date);
		this.timeout=0;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public Calendar getDate() {
		return date;
	}
	
	public int	getDayOfWeek(){
		return this.date.get(Calendar.DAY_OF_WEEK);
	}
	public int getSecondOfDay(){
		return this.date.get(Calendar.HOUR_OF_DAY)*3600+this.date.get(Calendar.MINUTE)*60+this.date.get(Calendar.SECOND);
	}
	
	public int getSecondOfDay(Calendar date){
		return date.get(Calendar.HOUR_OF_DAY)*3600+date.get(Calendar.MINUTE)*60+date.get(Calendar.SECOND);
	}
	
	public Date	getTime(){
		return this.date.getTime();
	}
	public void add(int field,int amount){
		this.date.add(field, amount);
	}
	
	public int getYear(){
		return this.date.get(Calendar.YEAR);
	}
	public int getMonth(){
		return this.date.get(Calendar.MONTH);
	}
	public int getDayOfMonth(){
		return this.date.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * ��ȡ��������ʱ��
	 * 
	 * @param listDays ����������,��������,������������ ��
	 * @return ��������ʱ��
	 * @throws Exception
	 */
	public Date getLimitTime(List<WFDay> listDays) throws Exception{		
		List<WFDay> nListTimes = createLimitCfg(listDays);
		return timeLoop(this,nListTimes);		
	}

	/**
	 * ����ʱ�޳���
	 * 
	 * @param listDays ����������,��������,������������ ��
	 * @param endDate ���ʱ��
	 * @return ���ص�ǰʱ�������ʱ�� ֮��Ĺ�����Чʱ��
	 * @throws Exception
	 */
	public int calculateLimitTime(List<WFDay> listDays, Date endDate) throws Exception{
		List<WFDay> nListTimes = createLimitCfg(listDays);
		
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(endDate);
		
		return calculateLoop(this,nListTimes, endCalendar);
	}
	
	protected boolean isSpecialDate(Date vDate){
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(vDate);
		if (calendar.get(Calendar.DAY_OF_MONTH)==getDayOfMonth() && calendar.get(Calendar.MONTH)==getMonth() && calendar.get(Calendar.YEAR)==getYear()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * �Ƚ�ʱ���С,����
	 * 
	 * @param endDate
	 * @return
	 * endDate > ��ǰʱ��date return 1
	 * endDate = ��ǰʱ��date return 0
	 * endDate < ��ǰʱ��date return -1
	 */
	protected int compare(Calendar endDate) {
		long yearFromTo = date.get(Calendar.YEAR);
		long yearFromEnd = endDate.get(Calendar.YEAR);
		
		if(yearFromEnd > yearFromTo){
			return 1;
		}else if(yearFromEnd < yearFromTo){
			return -1;
		}else{
			//������
			long dayFromTo = date.get(Calendar.DAY_OF_YEAR);
			long dayFromEnd = endDate.get(Calendar.DAY_OF_YEAR);
			
			if(dayFromEnd > dayFromTo){
				return 1;
			}else if (dayFromEnd < dayFromTo){
				return -1;
			}else{
				return 0;
			}
		}
	}
	
	protected void adjust(int amount){
		if (amount>0){
			this.date.add(Calendar.SECOND, amount);
			this.timeout-=amount;
		}
	}
	
	/**
	 * ����ʱ��
	 * 
	 * @param amount
	 */
	protected void addTimeOut(int amount){
		if (amount>0){
			this.date.add(Calendar.SECOND, amount);
			timeout += amount;
		}
	}
	
	/**
	 * ����ʱ������
	 * 
	 * @param listDays
	 * @return
	 */
	private List<WFDay> createLimitCfg(List<WFDay> listDays) {
		WFDay working=null;
		List<WFDay> nListTimes=new ArrayList<WFDay>();
		for (int i=0;i<listDays.size();i++){
			WFDay rTime=listDays.get(i);
			if (rTime instanceof WFDaySpecially){
				nListTimes.add(0, rTime);
			}else if (rTime instanceof WFDayHoliday){
				nListTimes.add(rTime);
			}else if (rTime instanceof WFDayWorking){
				working=rTime;
			}
		}
		if (working!=null){
			nListTimes.add(working);
		}
		return nListTimes;
	}
	
	/**
	 * ����ʱ��ѭ��
	 * 
	 * @param wfTime
	 * @param listDays
	 * @param endDate
	 * @return
	 * @throws Exception
	 */
	private int calculateLoop(WFTime wfTime,List<WFDay> listDays, Calendar endDate) throws Exception{
		for (int i=0;i<listDays.size();i++){
			WFDay wfDay=listDays.get(i);
			if (wfDay.calculate(wfTime, endDate)){
				int sTime = getSecondOfDay(date);
				int eTime = getSecondOfDay(endDate);
				int ret = compare(endDate);
				if (ret == 1 ){//��ʼʱ��<����ʱ��
					
					wfTime.add(Calendar.SECOND,1);//������һ��
					return calculateLoop(wfTime,listDays, endDate);//����ݹ�
					
				}else if(ret == 0 && sTime<eTime){//��ʼʱ��ͽ���ʱ��Ϊͬһ��,����ʼʱ��<����ʱ��
					
					return calculateLoop(wfTime,listDays, endDate);//����ݹ�
					
				}else{
					return timeout;
				}
			}
		}
		return timeout;
	}
	
	/**
	 * ʱ��ѭ��
	 * 
	 * @param wfTime
	 * @param listDays
	 * @return
	 * @throws Exception
	 */
	private Date timeLoop(WFTime wfTime,List<WFDay> listDays) throws Exception{
		recursiveTime++;
		if(recursiveTime > MAX_RECURSIVE_TIME){
			throw new Exception("�������ݹ����"+MAX_RECURSIVE_TIME);
		}	
		if(listDays.size()>0){
			for (int i=0;i<listDays.size();i++){
				WFDay wfDay=listDays.get(i);
				if (wfDay.adjust(wfTime)){
					if(wfTime.isjudge==false) wfTime.isjudge=true;
					if (wfTime.getTimeout()>0){					
						wfTime.add(Calendar.SECOND,1);
						return timeLoop(wfTime,listDays);
					}else{
						return wfTime.getTime();
					}
				}
			}		
		}else{
			wfTime.adjust(wfTime.getTimeout());
		}
		return wfTime.getTime();
	}

}
