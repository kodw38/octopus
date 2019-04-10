package com.octopus.utils.time;

import junit.framework.TestCase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WFTimeTest extends TestCase{
	private ArrayList<WFDay> listDays=new ArrayList<WFDay>();
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		ArrayList<WFDayWorkTime> listTimes=new ArrayList<WFDayWorkTime>();
		//listTimes.add(new WFDayWorkTime(8*60*60,12*60*60));
		//listTimes.add(new WFDayWorkTime(14*60*60,18*60*60));
		
		// 6��12��Ϊ����ڵ���
		listDays.add(new WFDaySpecially(format.parse("2010-06-12 00:00:00"),new WFDayWorking(listTimes)));
		
		// 6��13��Ϊ����ڵ���
		listDays.add(new WFDaySpecially(format.parse("2010-06-13 00:00:00"),new WFDayWorking(listTimes)));
		
		// 6��14,15,16��Ϊ����ڼ���
		listDays.add(new WFDayHoliday(format.parse("2010-06-14 00:00:00")));
		listDays.add(new WFDayHoliday(format.parse("2010-06-15 00:00:00")));		
		listDays.add(new WFDayHoliday(format.parse("2010-06-16 00:00:00")));
		
		// ��6����Ϊ�̶�����
		listDays.add(new WFDayHoliday(Calendar.SATURDAY));		
		listDays.add(new WFDayHoliday(Calendar.SUNDAY));
		
		// ��׼������
		listDays.add(new WFDayWorking(listTimes));		
	}
	
	public void testWorkTime() throws Exception{
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<WFDay> li = new ArrayList<WFDay>();
		li.add(new WFDayHoliday(Calendar.SATURDAY));
        li.add(new WFDayHoliday(Calendar.SUNDAY));
        li.add(new WFDayHoliday(format.parse("2011-10-18 00:00:00")));
        li.add(new WFDayHoliday(format.parse("2011-10-27 00:00:00")));
        List t = new ArrayList();
        t.add(new WFDayWorkTime(0,WFTime.SECONDS_OF_DAY));
        li.add(new WFDayWorking(t));
		WFTime wfTime=new WFTime(format.parse("2011-10-15 11:52:28"),2*(WFTime.SECONDS_OF_DAY));
		Date rtn = wfTime.getLimitTime(li);
		System.out.println("testWorkTime="+format.format(rtn.getTime()));
	}
}
