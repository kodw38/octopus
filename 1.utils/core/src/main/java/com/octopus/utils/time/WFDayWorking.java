package com.octopus.utils.time;

import java.util.Calendar;
import java.util.List;

/**
 * <p>
 * ������ʱ�޽ӿ�
 * </p>
 * @author �¾�
 * @Date 2010-06-09
 * <p>
 * 2010-6-10 ½���� 
 * 				����calculate�ӿ�,���ڼ��㹤��ʱ��
 * </p>
 */
public class WFDayWorking implements WFDay {
	private List<WFDayWorkTime> listTimes=null;
	public WFDayWorking(List<WFDayWorkTime> listTimes){
		this.listTimes=listTimes;
	}
	
	public boolean adjust(WFTime wfTime) throws Exception {
		if(listTimes.size()>0){
			adjustLoop(wfTime,0);
			return true;
		}else{
			return false;
		}
	}
	
	private void adjustLoop(WFTime wfTime,int index){
		if (index<listTimes.size()){
			WFDayWorkTime workTime=listTimes.get(index);
			int bTime=workTime.getBegin();
			int	eTime=workTime.getEnd();
			int cTime=wfTime.getSecondOfDay();
			int timeout=wfTime.getTimeout();
			if (cTime<=eTime){
				if (cTime<bTime){
					wfTime.add(Calendar.SECOND,bTime-cTime);
					cTime=bTime;
				}
				if (cTime+timeout<=eTime){
					wfTime.adjust(timeout);								
				}else{
					wfTime.adjust(eTime-cTime);					
				}
			}
			if (wfTime.getTimeout()>0){
				adjustLoop(wfTime,index+1);
			}
		}else{
			int cTime=wfTime.getSecondOfDay();
			if (cTime<WFTime.SECONDS_OF_DAY){
				wfTime.add(Calendar.SECOND,WFTime.SECONDS_OF_DAY-cTime);				
			}
		}
	}

	
	public boolean calculate(WFTime wfTime, Calendar endDate) throws Exception {
		calculateLoop(wfTime, endDate, 0);
		return true;
	}
	
	private void calculateLoop(WFTime wfTime,Calendar endDate, int index){
		
		int curEndTime = 0;//��ǰ����ʱ��
		
		int ret = wfTime.compare(endDate);
		if(ret == 0){
			//��ʼʱ��ͽ���ʱ��Ϊͬһ��,endTimeȡendDate��ȫ������
			curEndTime = wfTime.getSecondOfDay(endDate);
		}else if(ret == 1){
			//��ʼʱ�� < ����ʱ��,��ʼʱ��ȡȫ������
			curEndTime = 86400;
		}
		
		if (index < listTimes.size()){
			
			WFDayWorkTime workTime = listTimes.get(index);
			int bTime=workTime.getBegin();//������ʼʱ��
			int	eTime=workTime.getEnd();//��������ʱ��
			int cTime=wfTime.getSecondOfDay();//��ǰʱ��
			
			if(curEndTime < bTime){
				//��ǰ����ʱ���ڴ����ù������֮��
				//����ݹ�
				calculateLoop(wfTime,endDate, index+1);
				return;
			}
			
			if(bTime<= cTime && curEndTime <= eTime){
				//�ڹ����������
				wfTime.addTimeOut(curEndTime - cTime);
				
			}else if(cTime < bTime && curEndTime <= eTime){
				
				//��ǰʱ���ڹ����������,��ǰ����ʱ���ڹ����������
				wfTime.add(Calendar.SECOND,bTime-cTime);
				wfTime.addTimeOut(curEndTime - bTime);
				
			}else if(bTime<= cTime && curEndTime > eTime){
				//��ǰʱ���ڹ����������,��ǰ����ʱ���ڹ����������
				wfTime.addTimeOut(eTime - cTime);
			
			}else if(cTime < bTime &&  curEndTime > eTime){
				//��ǰʱ��͵�ǰ����ʱ��������
				wfTime.add(Calendar.SECOND,bTime-cTime);
				wfTime.addTimeOut(eTime - bTime);
			}
			
			//����ݹ�
			calculateLoop(wfTime,endDate, index+1);
			
		}else{
			//�ֶι���ʱ�䴦�����,����ǰʱ��������������59:59��
			int cTime=wfTime.getSecondOfDay();
			if (cTime<WFTime.SECONDS_OF_DAY){
				wfTime.add(Calendar.SECOND,WFTime.SECONDS_OF_DAY-cTime);				
			}
		}
	} 
}
