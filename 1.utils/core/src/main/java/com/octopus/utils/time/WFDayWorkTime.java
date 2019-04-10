package com.octopus.utils.time;

import java.io.Serializable;

/**
 * <p>
 * ����ʱ��ζ���
 * </p>
 * @author �¾�
 * @Date 2010-06-09
 */
public class WFDayWorkTime implements Serializable{
	private int begin=0;
	private int	end=0;
	public WFDayWorkTime(int begin,int end){
		this.begin=begin;
		this.end=(end>WFTime.SECONDS_OF_DAY)?WFTime.SECONDS_OF_DAY:end;
	}
	public int getBegin() {
		return begin;
	}
	public int getEnd() {
		return end;
	}
}
