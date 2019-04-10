package com.octopus.utils.expression;


import com.octopus.utils.time.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * User: robai
 * Date: 2009-4-15
 * Time: 12:04:05
 */
public class ExpressionParse {
    public static final int DATE_TYPE_YEAR= 1;
    public static final int DATE_TYPE_MONTH= 2;
    public static final int DATE_TYPE_DAY= 3;
    public static final int DATE_TYPE_HOUR= 4;
    public static final int DATE_TYPE_MINUTE= 5;
    public static final int DATE_TYPE_SECOND= 6;
    SimpleDateFormat dateFormat = new  SimpleDateFormat("yyyyMMdd");
    String exp = null;
    HashMap map = new HashMap();
    static BaseExpression ex = new BaseExpression();

    public ExpressionParse(){

    }
    public ExpressionParse(String exp){
        this.exp = exp;
    }
    public void setExpression(String exp){
        this.exp = exp;
    }
    public void setValue(String name,String value)throws Exception{
        map.put(name,value);
    }
    public void setDateValue(String name , Date value,int type)throws Exception{
        switch(type){
            case DATE_TYPE_YEAR : map.put(name, dateFormat.format(DateTimeUtils.getWholeYearDate(value)));
            case DATE_TYPE_MONTH : map.put(name, dateFormat.format(DateTimeUtils.getWholeMonthDate(value)));
            case DATE_TYPE_DAY : map.put(name, dateFormat.format(DateTimeUtils.getWholeDayDate(value)));
            case DATE_TYPE_HOUR : map.put(name, dateFormat.format(DateTimeUtils.getWholeHourDate(value)));
            case DATE_TYPE_MINUTE : map.put(name, dateFormat.format(DateTimeUtils.getWholeMinuteDate(value)));
            case DATE_TYPE_SECOND : map.put(name, dateFormat.format(DateTimeUtils.getWholeSecondDate(value)));
        }

    }
    private String chgExp(String exp,HashMap pars){
        if(null != null && pars.size()>0){
            Iterator its = map.keySet().iterator();
            while(its.hasNext()){
                String name = (String)its.next();
                String value = (String)map.get(name);
                exp = exp.replaceAll(name,value);
            }
        }
        return exp;
    }
    public Object parse()throws Exception{
        String expr = chgExp(exp,map);


        Object r =  ex.calculate(expr);

        return r;
    }
    public static Object parse(String expr)throws Exception{
        //long m = System.currentTimeMillis();
        return ex.calculate(expr);
        //System.out.println(expr+":"+(System.currentTimeMillis()-m));
        //return r;
    }
    public boolean parseLogic()throws Exception{
        String expr = chgExp(exp,map);
        Object ret =  ex.calculate(expr);
        if((Double.parseDouble((String)ret))>0){
            return true;
        }
        return false;

    }

    public static void main(String[] args){
        try{
            //String exp = "(2>1)&(3>2)&(M!m)";
            //String exp = "(10+11)*8/10+(10*2)";
            //����ۼ�������>50 ���� �ȼ�Ϊ3����>20,�����г�������
            /*String exp = "((:OVER_TIME>1)|(:LEVEL_TIME>20))|(:TIME>50)|((:CURRENT_DATE-:START_DATE)>3)&(����=����2)";
            ExpressionParse parse = new ExpressionParse(exp);
            parse.setValue(":OVER_TIME","0");
            parse.setValue(":LEVEL_TIME","20");
            parse.setValue(":TIME","21");
            parse.setDateValue(":CURRENT_DATE",new Date(),ExpressionParse.DATE_TYPE_DAY);
            parse.setDateValue(":START_DATE",parse.dateFormat.parse("20090412"),ExpressionParse.DATE_TYPE_DAY);
            System.out.println(parse.parseLogic());
            */
            ExpressionParse parse1 = new ExpressionParse("(f0.04 > 0.01)|((f0.04 = 0.01))&(f0.04 < 0.05)&(0.04 < 0)&(0.02 < f0.05)");
            parse1.setValue("1.rate","0.02");
            parse1.setValue("2.rate","f0.02");
            parse1.setValue("3.rate","f0.07");
            parse1.setValue("4.rate","f0.07");
            parse1.setValue("5.rate","f0.07");
            System.out.println(parse1.parseLogic());
            parse1 = new ExpressionParse("0<=10");
            System.out.println(parse1.parse());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
