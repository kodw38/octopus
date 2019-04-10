package com.octopus.utils.time;

import com.octopus.utils.alone.NumberUtils;
import com.octopus.utils.alone.StringUtils;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * <p>Title: Asiainfo Portal System</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: Asiainfo Technologies (China),Inc.HangZhou</p>
 *
 * @author Asiainfo PSO/yuanjq
 * @version 1.0
 */
public class DateTimeUtils {
    public static final SimpleDateFormat DATA_FORMAT_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat DATA_FORMAT_YYYYMM = new SimpleDateFormat("yyyyMM");
    public static final SimpleDateFormat DATA_FORMAT_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat DATA_FORMAT_YYMMDDHHMMSS = new SimpleDateFormat("yyMMddHHmmss");
    public static final SimpleDateFormat DATA_FORMAT_YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat DATA_FORMAT_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATA_FORMAT_YYMMDD = new SimpleDateFormat("yyMMdd");
    public static final SimpleDateFormat DATA_FORMAT_HHMMSS = new SimpleDateFormat("HHmmss");
    public static final SimpleDateFormat DATA_FORMAT_HH_MM_SS = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat DATA_FORMAT_ALL = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    public static final int DATE_CYCLE_TYPE_YEAR = 1;
    public static final int DATE_CYCLE_TYPE_MONTH = 2;    
    public static final int DATE_CYCLE_TYPE_DAY = 4;
    public static final int DATE_CYCLE_TYPE_HOUR = 5;
    public static final int DATE_CYCLE_TYPE_MINUTE = 6;
    
    public static final int DATE_CYCLE_START_CUR_TIME = 1;
    public static final int DATE_CYCLE_START_CUR_WHOLE_TIME = 2;    
    public static final int DATE_CYCLE_START_PARENT_WHOLE_TIME = 3;

    private static DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HHmmss");    

    public static final int SUNDAY = 1;

    public static final int MONDAY = 2;

    public static final int TUESDAY = 3;

    public static final int WEDNESDAY = 4;

    public static final int THURSDAY = 5;

    public static final int FRIDAY = 6;

    public static final int SATURDAY = 7;

    public static final int LAST_DAY_OF_MONTH = -1;

    public static final long MILLISECONDS_IN_MINUTE = 60L * 1000L;

    public static final long MILLISECONDS_IN_HOUR = 60L * 60L * 1000L;

    public static final long SECONDS_IN_DAY = 24L * 60L * 60L;

    public static final long MILLISECONDS_IN_DAY = SECONDS_IN_DAY * 1000L;

    private static final transient Logger log = Logger.getLogger(DateTimeUtils.class);

    public static SimpleDateFormat getSimpleDateFormate(String pattern)throws Exception{
        if(pattern.equals("yyyyMMdd"))
            return DATA_FORMAT_YYYYMMDD;
        if(pattern.equals("yyyyMMddHHmmss"))
            return DATA_FORMAT_YYYYMMDDHHMMSS;
        if(pattern.equals("yyMMddHHmmss"))
            return DATA_FORMAT_YYMMDDHHMMSS;
        if(pattern.equals("yyyyMMddhhmmss"))
            return DATA_FORMAT_YYYYMMDDHHMMSS;
        if(pattern.equals("yyyy-MM-dd HH:mm:ss"))
            return DATA_FORMAT_YYYY_MM_DD_HH_MM_SS;
        if(pattern.equals("yyyy-MM-dd"))
            return DATA_FORMAT_YYYY_MM_DD;
        if(pattern.equals("yyMMdd"))
            return DATA_FORMAT_YYMMDD;
        if(pattern.equals("HHmmss"))
            return DATA_FORMAT_HHMMSS;
        if(pattern.equals("HH:mm:ss"))
            return DATA_FORMAT_HH_MM_SS;
        if(pattern.equals("yyyyMMddHHmmssSSS"))
            return DATA_FORMAT_ALL;
        if(pattern.equals("HH_MM_SS"))
            return DATA_FORMAT_HH_MM_SS;
        throw new Exception("not support date pattern["+pattern+"]");
    }

    /**
     * yyyy+-num mm+-num dd+- num
     * @param pattern
     * @return
     */
    public static boolean isDatePattern(String pattern){
        if("hhmmss".equals(pattern))return true;
       //return StringUtils.regExpress(pattern,"^yyyy([-|/+]\\d{1,2})?[-| |/]?(MM([-|/+]\\d{1,2})?)?[-| |/]?(dd([-|/+]\\d{1,2})?)? ?(hh([-|/+]\\d{1,2})?)?:?(mm([-|/+]\\d{1,2})?)?:?(ss([-|/+]\\d{1,2})?)?");
       return StringUtils.regExpress(pattern,"^(yyyy([-|/+]\\d{1,2})?|MM)([-|/+]\\d{1,2})?[-| |/]?(MM([-|/+]\\d{1,2})?|dd([-|/+]\\d{1,2})?)?[-| |/]?(dd([-|/+]\\d{1,2})?|yyyy([-|/+]\\d{1,2})?)? ?(hh|\\d{1,2}([-|/+]\\d{1,2})?)?:?(mm|\\d{1,2}([-|/+]\\d{1,2})?)?:?(ss|\\d{1,2}([-|/+]\\d{1,2})?)?");
    }
    public static boolean isNumChar(char c){
        if(c>=48 && c<=57)
            return true;
        return false;
    }

    /**
     * 枚举出两个字符日期之间，指定格式的日期
     * @param pattern
     * @param Date1
     * @param Date2
     * @return
     */
    public static String[] getBetweenString(String pattern,String Date1,String Date2) throws Exception {
        String[] r = StringUtils.getSplitTableNameByDateRange("",Date1+"-"+Date2);
        if(null != r) {
            LinkedList ret = new LinkedList();
            for(int i=0;i<r.length;i++) {
                String s = formatDate(r[i],pattern);
                if(!ret.contains(s)){
                    ret.add(s);
                }
            }
            return (String[])ret.toArray(new String[0]);
        }
        return null;
    }
    /**
     * 获取两个格式相同，有加减计算符号之间的日期字符串集合
     * @param earlypattern
     * @param laterpattern
     * @return
     * @throws Exception
     */
    public static String[] getStringBetweenDates(String earlypattern,String laterpattern) throws Exception {
        String s = getStringDate(earlypattern,null);
        String s1 = getStringDate(laterpattern,null);
        return StringUtils.getSplitTableNameByDateRange("",s+"-"+s1);
    }
    public static String getStringDate(String pattern,Date date){
        return getStringDateByPattern(pattern,date);
/*        String yyyy=null,MM=null,dd=null,hh=null,mm=null,ss=null;
        String num="";

        int yc=0,Mc=0,dc=0,hc=0,mc=0,sc=0;
        boolean isreduce=false,isNumEnd=false;
        String sp1="",sp2="",sp3="";
        for(int i=0;i<pattern.length();i++){
            char c = pattern.charAt(i);
            if(c==' '|| c==':'||c=='/'){
                if(c=='/') sp1=String.valueOf(c);
                if(c==':') sp3=String.valueOf(c);
                if(null != dd && null == hh)
                    sp2=" ";
                continue;
            }
            if(c=='-'){
                if(isNumChar(pattern.charAt(i+1))){
                    isreduce=true;
                }else {
                    sp1=String.valueOf(c);
                    continue;
                }
            }else if(c=='+'){
                isreduce=false;
            }
            if(isNumChar(c)){
                num+=c;
            }
            if(num!=null && (!isNumChar(c) || i==pattern.length()-1)){
                isNumEnd=true;
            }
            if(isNumEnd){
                if(null != ss  && StringUtils.isNotBlank(num)){
                    if(isreduce)sc-= Integer.parseInt(num);
                    else        sc+= Integer.parseInt(num);
                }else if(null != mm  && StringUtils.isNotBlank(num)){
                    if(isreduce)mc-= Integer.parseInt(num);
                    else        mc+= Integer.parseInt(num);
                }else if(null != hh  && StringUtils.isNotBlank(num)){
                    if(isreduce)hc-= Integer.parseInt(num);
                    else        hc+= Integer.parseInt(num);
                }else if(null != dd  && StringUtils.isNotBlank(num)){
                    if(isreduce)dc-= Integer.parseInt(num);
                    else        dc+= Integer.parseInt(num);
                }else if(null != MM  && StringUtils.isNotBlank(num)){
                    if(isreduce)Mc-= Integer.parseInt(num);
                    else        Mc+= Integer.parseInt(num);
                }else if(null != yyyy && StringUtils.isNotBlank(num)){
                    if(isreduce)yc-= Integer.parseInt(num);
                    else        yc+= Integer.parseInt(num);
                }
                num="";
            }

            if(c=='y'){
                yyyy+=c;
            }else if(c=='M'){
                MM+=c;
            }else if(c=='d'){
                dd+=c;
            }else if(c=='h'){
                hh+=c;
            }else if(c=='m'){
                mm+=c;
            }else if(c=='s'){
                ss+=c;
            }
        }

        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        if(null == date)
            date = new Date();
        cal.setTime(date);
        cal.add(GregorianCalendar.YEAR, yc);
        cal.add(GregorianCalendar.MONTH, Mc);
        cal.add(GregorianCalendar.DAY_OF_MONTH, dc);
        cal.add(GregorianCalendar.HOUR_OF_DAY, hc);
        cal.add(GregorianCalendar.MINUTE, mc);
        cal.add(GregorianCalendar.SECOND, sc);


        StringBuffer sb = new StringBuffer();
        if(null != yyyy)
            sb.append(cal.get(GregorianCalendar.YEAR));
        if(null != MM)
            sb.append(sp1).append(StringUtils.leftPad(""+(cal.get(GregorianCalendar.MONTH)+1),2,"0"));
        if(null != dd)
            sb.append(sp1).append(StringUtils.leftPad(""+(cal.get(GregorianCalendar.DAY_OF_MONTH)),2,"0"));
        if(null != hh)
            sb.append(sp2).append(cal.get(GregorianCalendar.HOUR_OF_DAY));
        if(null != mm)
            sb.append(sp3).append(cal.get(GregorianCalendar.MINUTE));
        if(null != ss)
            sb.append(sp3).append(cal.get(GregorianCalendar.SECOND));
        return sb.toString();*/
    }

    public static String getStringDateByPattern(String pattern,Date date){
        String yyyy=null,MM=null,dd=null,hh=null,mm=null,ss=null;
        String num="";

        int yc=0,Mc=0,dc=0,hc=0,mc=0,sc=0;
        boolean isreduce=false,isNumEnd=false,isNumBegin=false;
        String sp1="",sp2="",sp3="";
        LinkedList<String> fs = new LinkedList();
        for(int i=0;i<pattern.length();i++){
            char c = pattern.charAt(i);
            if(num!=null && isNumBegin && (!isNumChar(c) || i==pattern.length()-1)){
                isNumEnd=true;
                if(i!=pattern.length()-1) {
                    isNumBegin = false;
                }
            }
            if(c==' '|| c==':'||c=='/'){
                isNumBegin=false;
                if(c=='/') sp1=String.valueOf(c);
                if(c==':') sp3=String.valueOf(c);
                if(null != dd && null == hh)
                    sp2=" ";
                continue;
            }

            if(c=='-'){
                if(isNumChar(pattern.charAt(i+1))){
                    isreduce=true;
                    isNumBegin=true;
                    isNumEnd=false;
                }else {
                    sp1=String.valueOf(c);
                    isNumBegin=false;
                    continue;
                }
            }else if(c=='+'){
                isreduce=false;
                isNumBegin=true;
                isNumEnd=false;
            }
            if(isNumBegin && isNumChar(c)){
                num+=c;
            }

            if(isNumEnd){
                if(fs.getLast().equals("s") && null != ss  && StringUtils.isNotBlank(num)){
                    if(isreduce)sc-= Integer.parseInt(num);
                    else        sc+= Integer.parseInt(num);
                }else if(fs.getLast().equals("m") && null != mm  && StringUtils.isNotBlank(num)){
                    if(isreduce)mc-= Integer.parseInt(num);
                    else        mc+= Integer.parseInt(num);
                }else if(fs.getLast().equals("h") && null != hh  && StringUtils.isNotBlank(num)){
                    if(isreduce)hc-= Integer.parseInt(num);
                    else        hc+= Integer.parseInt(num);
                }else if(fs.getLast().equals("d") && null != dd  && StringUtils.isNotBlank(num)){
                    if(isreduce)dc-= Integer.parseInt(num);
                    else        dc+= Integer.parseInt(num);
                }else if(fs.getLast().equals("M") && null != MM  && StringUtils.isNotBlank(num)){
                    if(isreduce)Mc-= Integer.parseInt(num);
                    else        Mc+= Integer.parseInt(num);
                }else if(fs.getLast().equals("y") && null != yyyy && StringUtils.isNotBlank(num)){
                    if(isreduce)yc-= Integer.parseInt(num);
                    else        yc+= Integer.parseInt(num);
                }
                num="";
                isNumEnd=false;
            }

            if(c=='y'){
                yyyy+=c;
                if(!fs.contains("y")) fs.add("y");
            }else if(c=='M'){
                MM+=c;
                if(!fs.contains("M")) fs.add("M");
            }else if(c=='d'){
                dd+=c;
                if(!fs.contains("d")) fs.add("d");
            }else if(c=='h' || (!isNumBegin && isNumChar(c) && (null == hh || hh.length()<2)) ){//是hh或具体数值
                if(null ==hh)hh="";
                hh+=c;
                if(!fs.contains("h")) fs.add("h");
            }else if(c=='m' || (!isNumBegin && isNumChar(c) && (null == mm || mm.length()<2)) ){//是hh或具体数值
                if(null == mm)mm="";
                mm+=c;
                if(!fs.contains("m")) fs.add("m");
            }else if(c=='s' || (!isNumBegin && isNumChar(c) && (null == ss || ss.length()<2)) ){//是hh或具体数值
                if(null == ss) ss="";
                ss+=c;
                if(!fs.contains("s")) fs.add("s");
            }
        }

        GregorianCalendar cal = new GregorianCalendar();
        if(null == date)
            date = new Date();
        cal.setTime(date);

        if(NumberUtils.isNumber(hh)){
            cal.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(hh));
        }
        if(NumberUtils.isNumber(mm)){
            cal.set(GregorianCalendar.MINUTE, Integer.parseInt(mm));
        }
        if(NumberUtils.isNumber(ss)){
            cal.set(GregorianCalendar.SECOND, Integer.parseInt(ss));
        }
        cal.add(GregorianCalendar.YEAR, yc);
        cal.add(GregorianCalendar.MONTH, Mc);
        cal.add(GregorianCalendar.DAY_OF_MONTH, dc);
        cal.add(GregorianCalendar.HOUR_OF_DAY, hc);
        cal.add(GregorianCalendar.MINUTE, mc);
        cal.add(GregorianCalendar.SECOND, sc);

        StringBuffer sb = new StringBuffer();
        for(int i=0;i<fs.size();i++) {
            if (null != yyyy && fs.get(i).equals("y")) {
                if (sb.length() != 0) sb.append(sp1);
                sb.append(cal.get(GregorianCalendar.YEAR));
            }
            if (null != MM && fs.get(i).equals("M")) {
                if (sb.length() != 0) sb.append(sp1);
                sb.append(StringUtils.leftPad("" + (cal.get(GregorianCalendar.MONTH) + 1), 2, "0"));
            }
            if (null != dd && fs.get(i).equals("d")) {
                if(sb.length()!=0)sb.append(sp1);
                sb.append(StringUtils.leftPad("" + (cal.get(GregorianCalendar.DAY_OF_MONTH)), 2, "0"));
            }
            if (null != hh && fs.get(i).equals("h")) {
                sb.append(sp2).append(StringUtils.leftPad("" + cal.get(GregorianCalendar.HOUR_OF_DAY), 2, "0"));
            }
            if (null != mm && fs.get(i).equals("m")) {
                sb.append(sp3).append(StringUtils.leftPad("" + cal.get(GregorianCalendar.MINUTE), 2, "0"));
            }
            if (null != ss && fs.get(i).equals("s")) {
                sb.append(sp3).append(StringUtils.leftPad("" + cal.get(GregorianCalendar.SECOND), 2, "0"));
            }
        }
        return sb.toString();
    }


    public static void main(String[] args){
        try{
            //Date d = DateTimeUtils.string2Date("2015-10-14 14:48:03","yyyy-MM-dd HH:mm:ss");
            System.out.println(getLastDayOfMonth(new Date()));
            System.out.println(getFirstDayOfMonth(new Date()));
            String s = "yyyyMMdd";
            /*if(isDatePattern(s))
                System.out.println(DateTimeUtils.getStringDate(s,null));
            System.out.println(DateTimeUtils.getStringDate("yyyyMMdd-"+1,null));
            System.out.println(DateTimeUtils.getStringDate("yyyyMMdd-"+1+" hh-1:mm:ss",null));
            System.out.println(DateTimeUtils.getStringDate("MM/dd/yyyy",null));
            System.out.println(DateTimeUtils.getStringDate("MM/dd/yyyy hh:mm:ss",null));
            System.out.println(DateTimeUtils.getStringDate("yyyy/MM/dd 00:00:00",null));*/
            System.out.println(DateTimeUtils.getStringDate("MM/dd/yyyy-1 23-1:01-1:02-2",null));
            System.out.println(DateTimeUtils.getStringDate("MM/dd/yyyy-1 23-1:01-1:02-3",null));
            System.out.println(DateTimeUtils.getStringDate("MM/dd/yyyy-1 23-1:01-1:02-3",null));
            System.out.println(DateTimeUtils.getStringDate("yyyy-MM-dd 00:00:00",null));
            System.out.println(DateTimeUtils.isDatePattern("yyyy-MM-dd 00:00:00"));
            System.out.println(DateTimeUtils.isDatePattern("MM/dd/yyyy 00-1:00-1:00-1"));
            System.out.println(DateTimeUtils.isDatePattern("MM-1/dd-1/yyyy-1 00-1:00-1:00-1"));
            System.out.println(DateTimeUtils.isDatePattern("MM-1-dd-1-yyyy-1 00-1:00-1:00-1"));
            System.out.println(DateTimeUtils.isDatePattern("yyyy-MM-dd"));
            System.out.println(DateTimeUtils.isDatePattern("yyyy-MM-dd-5"));
            System.out.println(DateTimeUtils.isDatePattern("yyyy-1-MM-2-dd-5"));
            System.out.println("\\n");
            System.out.println(getDate("16:35:03 00"));
            String[] pts = getStringBetweenDates("yyyyMMdd-14","yyyyMMdd");
            pts = getBetweenString("yyyyMM","yyyyMMdd-14","yyyyMMdd");
            System.out.println(Arrays.asList(pts));
            System.out.println(getStringDateByPattern("hhmmss",new Date()));
            System.out.println(getStringDateByPattern("yyyy-MM-dd hh:mm:ss",new Date()));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getCurrHHMMSS(String pattern) throws Exception {
        return getSimpleDateFormate(pattern).format(getCurrentDate());
    }

    /**
     */
    public static String getCurrDate() {
        return getFormattedDate(getDateByString(""));
    }

    /**
     */
    public static String getFormattedDate(String strDate, String strFormatTo) {
        if ((strDate == null) || strDate.trim().equals("")) {
            return "";
        }
        strDate = strDate.replace('/', '-');
        strFormatTo = strFormatTo.replace('/', '-');
        if (strDate.equals("0000-00-00 00:00:00") ||
                strDate.equals("1800-01-01 00:00:00")) {
            return "";
        }
        String formatStr = strFormatTo; //"yyyyMMdd";
        if ((strDate == null) || strDate.trim().equals("")) {
            return "";
        }
        formatStr = getFormateByValue(strDate);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(formatStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(formatter.parse(strDate));
            formatter = new SimpleDateFormat(strFormatTo);
            return formatter.format(calendar.getTime());
        } catch (Exception e) {
            return "";
        }
    }

    public static Date getDate(String date) throws Exception {
        try {
        /*if(StringUtils.isNotBlank(date) && !date.equalsIgnoreCase("null")){
        String formate = getFormateByValue(date);
            return getSimpleDateFormate(formate).parse(date);
        }else{
            return null;
        }*/
            int y = 0, m = 0, d = 0, h = 0, min = 0, s = 0;
            String[] ss = date.split(" ");
            String tt = "";
            if (ss[0].indexOf("-") > 0) {
                tt = ss[0].replace("-", "");
            } else if (ss[0].indexOf("/") > 0) {
                tt = ss[0].replace("/", "");
            }else{
                tt = ss[0];
            }
            if (tt.length() == 8 && StringUtils.isNumeric(tt)) {
                y = Integer.valueOf(tt.substring(0, 4));
                m = Integer.valueOf(tt.substring(4, 6));
                d = Integer.valueOf(tt.substring(6, 8));
            } else if (tt.length() == 6 && StringUtils.isNumeric(tt)) {
                y = Integer.valueOf("20" + tt.substring(0, 2));
                m = Integer.valueOf(tt.substring(2, 4));
                d = Integer.valueOf(tt.substring(4, 6));
            }
            if (ss.length > 1) {
                String[] ts = ss[1].split("\\:");
                if (ts.length > 0 && ts[0].length()==2 ) {
                    h = Integer.valueOf(ts[0]);
                }
                if (ts.length > 1 && ts[1].length()==2) {
                    min = Integer.valueOf(ts[1]);
                }
                if (ts.length > 2 && ts[2].length()==2) {
                    s = Integer.valueOf(ts[2]);
                }
            }
            Calendar c = Calendar.getInstance();
            c.set(y, m-1, d, h, min, s);
            return c.getTime();
        }catch(Exception e){
            throw new Exception("convert String["+date+"] to Date error",e);
        }
    }

    static String getFormateByValue(String strDate){
        String formatStr="";
        switch (strDate.trim().length()) {
            case 6:
                if (strDate.substring(0, 1).equals("0")) {
                    formatStr = "yyMMdd";
                } else {
                    formatStr = "yyyyMM";
                }
                break;
            case 8:
                if(strDate.contains(":")){
                    formatStr="HH:mm:ss";
                }else{
                    formatStr = "yyyyMMdd";
                }
                break;
            case 10:
                if (strDate.indexOf("-") == -1) {
                    formatStr = "yyyy/MM/dd";
                } else {
                    formatStr = "yyyy-MM-dd";
                }
                break;
            case 11:
                if (strDate.getBytes().length == 14) {
                    formatStr = "yyyy-MM-dd";
                } else {
                    return "";
                }
            case 14:
                formatStr = "yyyyMMddHHmmss";
                break;
            case 17:
                formatStr = "yyyyMMdd HH:mm:ss";
                break;
            case 19:
                if (strDate.indexOf("-") == -1) {
                    formatStr = "yyyy/MM/dd HH:mm:ss";
                } else {
                    formatStr = "yyyy-MM-dd HH:mm:ss";
                }
                break;
            case 21:
                if (strDate.indexOf("-") == -1) {
                    formatStr = "yyyy/MM/dd HH:mm:ss.S";
                } else {
                    formatStr = "yyyy-MM-dd HH:mm:ss.S";
                }
                break;
            default:
                formatStr= strDate.trim();
        }
        return formatStr;
    }
    /**
     */
    public static Timestamp getDateByString(String strDate){
        if (strDate.trim().equals("")) {
            return getCurrentDate();
        }
        try {
            strDate = getFormattedDate(strDate, "yyyy-MM-dd HH:mm:ss") +
                    ".000000000";
            return Timestamp.valueOf(strDate);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return getCurrentDate();
        }
    }

    public static Timestamp getCurrentDate() {
        //return ServiceManager.getOpDateTime();
    	return new Timestamp(new Date().getTime());
        
        
    }

    /**
     */
    public static String getFormattedDate(Timestamp dtDate) {
        return getFormattedDate(dtDate, "yyyy-MM-dd");
    }

    /**
     */
    public static String getFormattedDate(Timestamp dtDate,
                                          String strFormatTo) {
        if (dtDate == null) {
            return "";
        }
        if (dtDate.equals(new Timestamp(0))) {
            return "";
        }
        strFormatTo = strFormatTo.replace('/', '-');
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
            if (Integer.parseInt(formatter.format(dtDate)) < 1900) {
                return "";
            } else {
                formatter = new SimpleDateFormat(strFormatTo);
                return formatter.format(dtDate);
            }
        } catch (Exception e) {
            log.error( e.getMessage());
            return "";
        }
    }

    /**
     *
     * @return String
     */
    public static String getCurrDateTime() throws Exception {
        Timestamp date = getCurrentDate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    public static String getAllCurrentDateTime()throws Exception{
        return DATA_FORMAT_ALL.format(new Date());
    }

    /**
     * add by liufeng 20061111
     *
     * @return String
     */
    public static String getCurrDateTime_yyyymmddhhmmss() throws Exception {
        Timestamp date = getCurrentDate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }

    public static Date getCurrDateTime_hhmmss()throws Exception{
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.YEAR,0);
        c.set(Calendar.MONTH,0);
        c.set(Calendar.DAY_OF_MONTH,0);
        return c.getTime();
    }

    /**
     *
     * @param date1
     * @param date2
     */
    public static int DateDiff(Date date1, Date date2) {
        int i = (int) ((date1.getTime() - date2.getTime()) / 3600 / 24 / 1000);
        return i;
    }

    /**
     *
     * @param timest1
     * @param month
     * @return
     */
    public static Timestamp DateAddMonth(Timestamp timest1, int month) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timest1);
        cal.add(Calendar.MONTH, month);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     *
     * @param strDate     String
     * @param iDays       int
     * @param strFormatTo String
     * @return String
     */
    public static String getDateAddDay(String strDate, int iDays, String strFormatTo) {
        Timestamp tsDate = Timestamp.valueOf(strDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(tsDate);
        cal.add(Calendar.DAY_OF_MONTH, iDays);
        Timestamp tsEndDateAdd = new Timestamp(cal.getTimeInMillis());
        return DateTimeUtils.getFormattedDate(tsEndDateAdd, strFormatTo);
    }
    /**
     * @return String
     */
    public static Timestamp getDateAddDay(Timestamp tsDate, int iDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(tsDate);
        cal.add(Calendar.DAY_OF_MONTH, iDays);
        Timestamp tsEndDateAdd = new Timestamp(cal.getTimeInMillis());
        return tsEndDateAdd;
    }
    /**
     *
     * @param dateTime
     * @param minute
     * @return
     */
    public static Timestamp getDateAddMinute(Timestamp dateTime, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTime);
        cal.add(Calendar.MINUTE, minute);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     *
     * @param timest1
     * @return
     */
    public static Timestamp getLastDayOfMonth(Date timest1) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timest1);
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 0);
        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        return new Timestamp(cal.getTimeInMillis());
    }
    public static Timestamp getFirstDayOfMonth(Date timest1) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timest1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     */
    public static String getToday() {
        Date cDate = new Date();
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

 
    public static String getYesterday() {
        Date cDate = new Date();
        cDate.setTime(cDate.getTime() - 24 * 3600 * 1000);
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

  
    public static String getTomorrow() {
        Date cDate = new Date();
        cDate.setTime(cDate.getTime() + 24 * 3600 * 1000);
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

    /**

     *
     * @param strFormat
     * @param iYear
     * @param iMonth
     * @param iDate
     * @return
     */
    public static String getSpecTime(String strFormat, int iYear, int iMonth,
                                     int iDate, int iHour, int iMinute,
                                     int iSecond) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.set(Calendar.YEAR, rightNow.get(Calendar.YEAR) + iYear);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH) + iMonth);
        rightNow.set(Calendar.DATE, rightNow.get(Calendar.DATE) + iDate);
        rightNow.set(Calendar.HOUR, rightNow.get(Calendar.HOUR) + iHour);
        rightNow.set(Calendar.MINUTE, rightNow.get(Calendar.MINUTE) + iMinute);
        rightNow.set(Calendar.SECOND, rightNow.get(Calendar.SECOND) + iSecond);
        SimpleDateFormat df = new SimpleDateFormat(strFormat);
        return df.format(rightNow.getTime());
    }

    
    public static String getCurrentYearMonth() throws Exception {
        return getYearMonth(getCurrentDate());
    }

   
    public static String getYearMonth(Timestamp dtDate) {
        return getFormattedDate(dtDate, "yyyyMM");
    }

    /**
     *
     * @param strDate     String
     * @param strFormat   String
     * @param iDiffYear   int
     * @param iDiffMonth  int
     * @param iDiffDay    int
     * @param iDiffHour   int
     * @param iDiffMinute int
     * @param iDiffSecond int
     * @return String
     */
    public static String changeDate(String strDate, String strFormat,
                                    int iDiffYear, int iDiffMonth, int iDiffDay,
                                    int iDiffHour, int iDiffMinute,
                                    int iDiffSecond) {
        String strChangedDay = "";
        if (strDate == null || strDate.equals("")) {
            return "";
        }
        strChangedDay += strDate.substring(0, 10) + " " + iDiffHour + ":" +
                iDiffMinute + ":" + iDiffSecond;
        return strChangedDay;
    }

    /**
     * add by liufeng 20061031 strFormat  yyyy-MM-dd HH:mm:ss"
     *
     * @param strDate   String
     * @param strFormat String
     * @return boolean
     */
    public static boolean isValidDataTime(String strDate, String strFormat) {
        if (strDate == null || strDate.equals("")) {
            return false;
        }
        if (strFormat == null || strFormat.equals("")) {
            return false;
        }
        if (strDate.length() != strFormat.length()) {
            return false;
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(strFormat);
            formatter.parse(strDate);
        } catch (ParseException ex) {
            return false;
        }

        String strTemp = getFormattedDate(strDate, strFormat);
        if (strTemp.equals(strDate)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * add by liufeng 20061122
     *
     * @param strFormat 
     * @param iDays     int
     * @return String
     */
    public static String getCurenDayAddDay(String strFormat, int iDays) {
        Calendar c = new GregorianCalendar();
        c.add(Calendar.DAY_OF_MONTH, iDays);
        Date cDate = new Date();
        cDate.setTime(c.getTimeInMillis());
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat(strFormat);
        return cSimpleDateFormat.format(cDate);
    }

    /**
     * add by liufeng 20071126
     * �õ���ǰʱ��ǰ(��)�����µĵ�һ�������
     *
     * @return
     */
    public static String getMonthFrtDate(int iMonth, String strFormat) {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, iMonth);
        Date cDate = new Date();
        cDate.setTime(cal.getTimeInMillis());
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat(strFormat);
        String strNewDate = cSimpleDateFormat.format(cDate);
        return strNewDate.subSequence(0, 8) + "01";
    }
    public static boolean isWorkDayOfWeek(Date d){
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        int n = cal.get(Calendar.DAY_OF_WEEK);
        if(n==Calendar.SUNDAY || n==Calendar.SATURDAY)
            return false;
        return true;
    }
    public static boolean isTodayWorkDayOfWeek(){
        return isWorkDayOfWeek(new Date());
    }

    private static void validateDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < SUNDAY || dayOfWeek > SATURDAY) {
            throw new IllegalArgumentException("Invalid day of week.");
        }
    }

    private static void validateHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Invalid hour (must be >= 0 and <= 23).");
        }
    }

    private static void validateMinute(int minute) {
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Invalid minute (must be >= 0 and <= 59).");
        }
    }

    private static void validateSecond(int second) {
        if (second < 0 || second > 59) {
            throw new IllegalArgumentException("Invalid second (must be >= 0 and <= 59).");
        }
    }

    private static void validateDayOfMonth(int day) {
        if ((day < 1 || day > 31) && day != LAST_DAY_OF_MONTH) {
            throw new IllegalArgumentException("Invalid day of month.");
        }
    }

    private static void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month (must be >= 1 and <= 12.");
        }
    }

    private static void validateYear(int year) {
        if (year < 1949 || year > 2099) {
            throw new IllegalArgumentException("Invalid year (must be >= 1949 and <= 2099.");
        }
    }
    
    public static int getYear(Date date){
    	Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }
    
    public static int getMonth(Date date){
    	Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH);
    }

    public static Date getWholeDate(int type,Date d)throws Exception{
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_YEAR){
    		return getWholeYearDate(d);
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_MONTH){
    		return getWholeMonthDate(d);
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_DAY){
    		return getWholeDayDate(d);
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_HOUR){
    		return getWholeHourDate(d);
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_MINUTE){
    		return getWholeMinuteDate(d);
    	}
    	throw new Exception("not support the whole type:"+type+".please see DateTimeUtils#DATE_CYCLE_TYPE_XXXX");
    }


    public static Date[][] getBetweenWholeDate(int type,int cycle,Date currentDate,Date preDate,int starttype)throws Exception{
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_YEAR){
    		Date oldyear = null;
    		if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_TIME){
    			oldyear = preDate;
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldyear = DateTimeUtils.getWholeYearDate(preDate);
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldyear = DateTimeUtils.getWholeYearDate(preDate);
    		}else{
    			oldyear = preDate;
    		}
    		int count = (int) DateTimeUtils.betweenYear(oldyear, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldyear;
    			rtn[i][1] = DateTimeUtils.addOrMinusYear(oldyear.getTime(), cycle);
    			oldyear = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_MONTH){
    		Date oldmonth = null;
    		if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_TIME){
    			oldmonth = preDate;
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldmonth = DateTimeUtils.getWholeMonthDate(preDate);
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldmonth = DateTimeUtils.getWholeYearDate(preDate);
    		}else{
    			oldmonth = preDate;
    		}
    		
    		int count = (int) DateTimeUtils.betweenMonth(oldmonth, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldmonth;
    			rtn[i][1] = DateTimeUtils.addOrMinusMonth(oldmonth.getTime(), cycle);
    			oldmonth = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_DAY){
    		Date oldday = null;
    		if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_TIME){
    			oldday = preDate;
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldday = DateTimeUtils.getWholeDayDate(preDate);
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldday = DateTimeUtils.getWholeMonthDate(preDate);
    		}else{
    			oldday = preDate;
    		}
    		
    		int count = (int) DateTimeUtils.betweenDay(oldday, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldday;
    			rtn[i][1] = DateTimeUtils.addOrMinusDays(oldday.getTime(), cycle);
    			oldday = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_HOUR){
			Date oldhour = null;
    		if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_TIME){
    			oldhour = preDate;
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldhour = DateTimeUtils.getWholeHourDate(preDate);
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldhour = DateTimeUtils.getWholeDayDate(preDate);
    		}else{
    			oldhour = preDate;
    		}

    		int count = (int) DateTimeUtils.betweenHour(oldhour, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldhour;
    			rtn[i][1] = DateTimeUtils.addOrMinusHours(oldhour.getTime(), cycle);
    			oldhour = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type== DateTimeUtils.DATE_CYCLE_TYPE_MINUTE){
    		Date oldminute = null;
    		if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_TIME){
    			oldminute = preDate;
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldminute = DateTimeUtils.getWholeMinuteDate(preDate);
    		}else if(starttype== DateTimeUtils.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldminute = DateTimeUtils.getWholeHourDate(preDate);
    		}else{
    			oldminute = preDate;
    		}
    		
    		int count = (int) DateTimeUtils.betweenMinute(oldminute, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldminute;
    			rtn[i][1] = DateTimeUtils.addOrMinusMinutes(oldminute.getTime(), cycle);
    			oldminute = rtn[i][1];
    		}
    		return rtn;
    	}
    	return null;
    }
        
    
    public static boolean isWholeOverCycle(int type,Date current,Date preDate)throws Exception{
    	if(getWholeDate(type,current).getTime()==getWholeDate(type,preDate).getTime()){
    		return false;
    	}else{
    		return true;
    	}
    }
    
    public static Date getWholeYearDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DAY_OF_YEAR, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getWholeMonthDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getWholeDayDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getNextWholeHourDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + 1);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getWholeHourDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getNextWholeMinuteDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getWholeMinuteDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getNextWholeSecondDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        c.set(Calendar.SECOND, c.get(Calendar.SECOND) + 1);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getWholeSecondDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getAddHourDate(Date date, int addHour) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addHour == 0) {
            return c.getTime();
        }
        int hour = c.get(Calendar.HOUR);
        c.set(Calendar.HOUR_OF_DAY, hour + addHour);
        return c.getTime();
    }

    public static Date getAddMinuteDate(Date date, int addMinute) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addMinute == 0) {
            return c.getTime();
        }
        int minute = c.get(Calendar.MINUTE);
        c.set(Calendar.MINUTE, minute + addMinute);
        return c.getTime();
    }

    public static Date getAddSecondDate(Date date, int addSecond) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addSecond == 0) {
            return c.getTime();
        }
        int second = c.get(Calendar.SECOND);
        c.set(Calendar.SECOND, second + addSecond);
        return c.getTime();
    }

    public static Date getAddHourWholeDate(Date date, int addHour) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addHour == 0) {
            return c.getTime();
        }
        int hour = c.get(Calendar.HOUR);
        c.set(Calendar.HOUR_OF_DAY, hour + addHour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getAddMinuteWholeDate(Date date, int addMinute) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addMinute == 0) {
            return c.getTime();
        }
        int minute = c.get(Calendar.MINUTE);
        c.set(Calendar.MINUTE, minute + addMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getAddSecondWholeDate(Date date, int addSecond) {
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        if (addSecond == 0) {
            return c.getTime();
        }
        int second = c.get(Calendar.SECOND);
        c.set(Calendar.SECOND, second + addSecond);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getDateOf(int second, int minute, int hour) {
        validateSecond(second);
        validateMinute(minute);
        validateHour(hour);
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getDateOf(int second, int minute, int hour, int dayOfMonth, int month) {
        validateSecond(second);
        validateMinute(minute);
        validateHour(hour);
        validateDayOfMonth(dayOfMonth);
        validateMonth(month);
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getDateOf(int second, int minute, int hour, int dayOfMonth, int month, int year) {
        validateSecond(second);
        validateMinute(minute);
        validateHour(hour);
        validateDayOfMonth(dayOfMonth);
        validateMonth(month);
        validateYear(year);
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date translateZoneTime(Date date, TimeZone src, TimeZone dest) {
        Date newDate = new Date();
    /*    int offset=0;
        offset= (getOffset(date.getTime(), src) - getOffset(date.getTime(),dest ));

        newDate.setTime(date.getTime() + offset);
*/
        newDate.setTime(date.getTime()+(dest.getRawOffset()-src.getRawOffset()));
        return newDate;
    }

    public static int getOffset(long date, TimeZone tz) {
        if (tz.inDaylightTime(new Date(date))) {
            return tz.getRawOffset() + getDSTSavings(tz);
        }
        return tz.getRawOffset();
    }

    public static long betweenSecond(Date start, Date end) {
        return ((end.getTime() - start.getTime()) / 1000);
    }
    public static long betweenMinSecond(Date start, Date end) {
        return ((end.getTime() - start.getTime()));
    }
    public static long betweenMinute(Date start, Date end) {
        return ((end.getTime() - start.getTime()) / 60 / 1000);
    }

    public static long betweenHour(Date start, Date end) {
        return ((end.getTime() - start.getTime()) / 3600 / 1000);
    }

    public static long betweenDay(Date start, Date end) {
        return ((end.getTime() - start.getTime()) / 3600 / 24 / 1000);
    }
    public static long betweenYear(Date start, Date end) {
    	return DateTimeUtils.getYear(end) - DateTimeUtils.getYear(start);
    }
    public static long betweenMonth(Date start, Date end) {
    	return DateTimeUtils.getYear(end) * 12 + DateTimeUtils.getMonth(end) -(DateTimeUtils.getYear(start) * 12 + DateTimeUtils.getMonth(start));
    }

    private static int getDSTSavings(TimeZone tz) {
        if (tz.useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }
    public static String getZoneTime(SimpleDateFormat format,Date date){
        return format.format(date);
    }
    public static SimpleDateFormat getDateFormat(String pattern,Locale locale){
        return new SimpleDateFormat(pattern,locale);
    }

    public static int getLastDayOfMonth(int monthNum, int year) {
        switch (monthNum) {
            case 1:
                return 31;
            case 2:
                return (isLeapYear(year)) ? 29 : 28;
            case 3:
                return 31;
            case 4:
                return 30;
            case 5:
                return 31;
            case 6:
                return 30;
            case 7:
                return 31;
            case 8:
                return 31;
            case 9:
                return 30;
            case 10:
                return 31;
            case 11:
                return 30;
            case 12:
                return 31;
            default:
                throw new IllegalArgumentException("Illegal month number: " + monthNum);
        }
    }

    public static boolean isLeapYear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0));
    }


    public static Timestamp dateToTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }

    public static Timestamp getDaysByStartDateAndEndDate(Connection conn, Timestamp start, Timestamp end) throws Exception {
        Timestamp endTime = null;
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            String start_str = "to_date('" + dateformat.format(start) + "','yyyy-mm-dd hh24miss')";
            String end_str = "to_date('" + dateformat.format(end) + "','yyyy-mm-dd hh24miss')";
            rs = stmt.executeQuery("select " + end_str + " + " + start_str + " as da from dual");
            if (rs.next()) {
                endTime = rs.getTimestamp("da");
            }
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
        return endTime;
    }


    
    public static int getDayOfWeek(Date date)throws Exception{
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        return cal1.get(Calendar.DAY_OF_WEEK);    
    }


    /**
     *
     * @param ts Timestamp
     * @return String
     * @throws Exception
     */
    public static String getYYYYMMDDHHMMSS(Timestamp ts) throws Exception {
        if (ts == null) {
            return null;
        }
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String str = dateformat.format(ts);
        return str;
    }

    /**
     *
     * @param ts      Timestamp
     * @param pattern String
     * @return String
     * @throws Exception
     */
    public static String getYYYYMMDDHHMMSS(Timestamp ts, String pattern) throws Exception {
        if (ts == null) {
            return null;
        }
        DateFormat dateformat = new SimpleDateFormat(pattern);
        String str = dateformat.format(ts);
        return str;
    }


    /**
     *
     * @param time    String
     * @param pattern String yyyy-MM-dd HH:mm:ss
     * @return Timestamp
     * @throws Exception
     */
    public static Timestamp getTimestampByYYYYMMDDHHMMSS(String time, String pattern) throws Exception {
        Timestamp rtn = null;
        DateFormat dateformat2 = new SimpleDateFormat(pattern);
        rtn = new Timestamp(dateformat2.parse(time.trim()).getTime());
        return rtn;
    }


    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     * @throws Exception
     */
    public static Date addOrMinusYear(long ti, int i) throws Exception {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.YEAR, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     * @throws Exception
     */
    public static Date addOrMinusMonth(long ti, int i) throws Exception {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.MONTH, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     */
    public static Date addOrMinusWeek(long ti, int i) {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.WEEK_OF_YEAR, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     */
    public static Date addOrMinusDays(long ti, int i) {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.DAY_OF_MONTH, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     */
    public static Date addOrMinusHours(long ti, int i) {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.HOUR, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     * @param ti long
     * @param i  int
     * @return Date
     */
    public static Date addOrMinusMinutes(long ti, int i) {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.MINUTE, i);
        rtn = cal.getTime();
        return rtn;
    }

    /**
     *
     * @param ti long
     * @param i  int
     * @return Date
     */
    public static Date addOrMinusSecond(long ti, int i) {
        Date rtn = null;
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(ti);
        cal.setTime(date);
        cal.add(GregorianCalendar.SECOND, i);
        rtn = cal.getTime();
        return rtn;
    }

    

    /**
     *
     * @param date
     * @return
     * @author shaosm
     */
    public static Timestamp getDateOfNextMonthFirstDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, 1);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH) + 1);
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getDateOfPreMonthFirstDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, 1);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH) - 1);
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp formatDateTimeToDate(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getDateOfCurrentMonthEndDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, rightNow.getActualMaximum(Calendar.DAY_OF_MONTH));
        rightNow.set(Calendar.HOUR_OF_DAY, 23);
        rightNow.set(Calendar.MILLISECOND, 59);
        rightNow.set(Calendar.SECOND, 59);
        rightNow.set(Calendar.MINUTE, 59);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getLastDate(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.HOUR_OF_DAY, 23);
        rightNow.set(Calendar.MILLISECOND, 59);
        rightNow.set(Calendar.SECOND, 59);
        rightNow.set(Calendar.MINUTE, 59);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }

    public static Timestamp getLastHour(Date date){
        if (date == null) {
            date = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.setLenient(true);
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + 1);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 59);
        return new Timestamp(c.getTimeInMillis());
    }

    public static Timestamp getLastDay(Date date){
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.HOUR_OF_DAY, 23);
        rightNow.set(Calendar.MILLISECOND, 59);
        rightNow.set(Calendar.SECOND, 59);
        rightNow.set(Calendar.MINUTE, 59);
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getPreLastDate(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, rightNow.get(Calendar.DAY_OF_MONTH) - 1);
        rightNow.set(Calendar.HOUR_OF_DAY, 23);
        rightNow.set(Calendar.MILLISECOND, 59);
        rightNow.set(Calendar.SECOND, 59);
        rightNow.set(Calendar.MINUTE, 59);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getNextDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, rightNow.get(Calendar.DAY_OF_MONTH) + 1);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }
    
    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getDay(Date date,int i) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, rightNow.get(Calendar.DAY_OF_MONTH) + i);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static String getYYYYMMDD(Date date) {
        if (date == null)
            return null;
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        return dateformat.format(date);
    }

    /**
     *
     * @param date
     * @return
     */
    public static String getNoLineYYYYMMDD(Date date) {
        if (date == null)
            return null;
        DateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
        return dateformat.format(date);
    }

    /**
     *
     * @return
     */
    public static Timestamp getBillMonthDate(Date beginDate, Date endDate) {
        if (null == beginDate) {
            return null;
        }
        Timestamp monthEndDate = new Timestamp(addOrMinusDays(getDateOfNextMonthFirstDay(endDate).getTime(), -1).getTime());
        return new Timestamp(monthEndDate.getTime());
    }

 

    public static long getHHMMSSSecond(String HHmmss)throws Exception{
        String[] sp = HHmmss.split("\\:");
        long hour = Integer.parseInt(sp[0]);
        long minute = Integer.parseInt(sp[1]);
        long second = Integer.parseInt(sp[2]);
        return hour*60*60 + minute*60 + second;
    }

  

    /**
     *
     * @param date
     * @return
     * @author shaosm
     */
    public static Timestamp getTruncDate(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        return new Timestamp(rightNow.getTimeInMillis());
    }

    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getDateOfMonthFirstDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.DAY_OF_MONTH, 1);
        rightNow.set(Calendar.HOUR_OF_DAY, 0);
        rightNow.set(Calendar.MILLISECOND, 0);
        rightNow.set(Calendar.SECOND, 0);
        rightNow.set(Calendar.MINUTE, 0);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }
    /**
     *
     * @param date
     * @return
     */
    public static Timestamp getDateOfCurrentEndDay(Date date) {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.set(Calendar.HOUR_OF_DAY, 23);
        rightNow.set(Calendar.MILLISECOND, 59);
        rightNow.set(Calendar.SECOND, 59);
        rightNow.set(Calendar.MINUTE, 59);
        rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH));
        return new Timestamp(rightNow.getTimeInMillis());
    }


  
    public static Timestamp getYearLastDayTimeByDate(Timestamp tsDate)throws Exception {
 	   String strYear = getFormattedDate(tsDate, "yyyy");
 	   String strLastTime = strYear + "-12-31 23:59:59";
 	   Timestamp tsLastTime = null;
 	   return getDateByString(strLastTime);
    }
    
    /**
     * ��ǰϵͳʱ��(long��)
     * 
     * @return
     */
    public static long now() {
		return System.currentTimeMillis();
	}
    
    
    
    // #######################add by mayc ##############################
    
    public static Calendar date2Calendar(Date datetime){
        if(datetime==null)return null;
        Calendar c = Calendar.getInstance();
        c.setTime(datetime);
        return c;
    }
    public static Calendar sqldate2Calendar(java.sql.Date datetime){
        if(datetime==null)return null;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(datetime.getTime());
        return c;
    }
    public static String calendar2String(Calendar time){
        return date2String(time.getTime());
    }
    public static String date2String(Date time,String datepattern){
        if(null == time)return null;
    	if(StringUtils.isEmpty(datepattern)){
    		datepattern = DateFormatEnum.DATA_FORMAT_DEFAULT;
    	}
    	SimpleDateFormat dateFormat = new SimpleDateFormat(datepattern);
        try{
            return dateFormat.format(time);
        }catch(Exception e){
        	log.error("date to string error",e);
            return null;
        }
    }
    public static String date2String(Timestamp time,String datepattern){
    	if(StringUtils.isEmpty(datepattern)){
    		datepattern = DateFormatEnum.DATA_FORMAT_DEFAULT;
    	}
    	SimpleDateFormat dateFormat = new SimpleDateFormat(datepattern);
        try{
            return dateFormat.format(time);
        }catch(Exception e){
        	log.error(e);
            return null;
        }
    }
    public static String formatDate(String date,String datepattern){
        try {
            Date d = getDate(date);
            return DateTimeUtils.getStringDate(datepattern, d);
        }catch (Exception e){

        }
        return date;
    }
    
    
    public static String formatDate(String date,String currentpattern,String formatPattern){
        String result = "";
        if("yyyyMMddHHmmss".equals(currentpattern) && "yyyy-MM-dd HH:mm:ss".equals(formatPattern)){
            StringBuffer sb = new StringBuffer();
            int l = date.length()-1;
            for(int i=0;i<currentpattern.length();i++){
                if(i==4|| i==6)
                    sb.append("-");
                if(i==8 )
                    sb.append(" ");
                if(i==10 || i==12)
                    sb.append(":");
                if(i>l){
                    sb.append("0");
                }else{
                    sb.append(date.charAt(i));
                }

            }
            result=sb.toString();
        }
        if("yyyy-MM-dd HH:mm:ss".equals(currentpattern) && "yyyyMMddHHmmss".equals(formatPattern)){
            StringBuffer sb = new StringBuffer();
            int l = date.length()-1;
            for(int i=0;i<currentpattern.length();i++){
                if(i==4|| i==6 || i==8 || i==10 || i==12)
                    continue;
                if(l>=i)
                    sb.append(date.charAt(i));

            }
            result=sb.toString();
        }

        return result;
        /*if(StringUtil.isNotEmpty(date)){
            Date date2 = string2Date(date, currentpattern);
            if(date2!= null){
                result = date2String(date2, formatPattern);
            }
        }
        return StringUtil.isEmpty(result)?date:StringUtil.trimToEmpty(result);*/
    }


    
    public static String formatDate(String date,DateFormatEnum currentpattern,DateFormatEnum formatPattern){
        String result = "";
        if(StringUtils.isNotEmpty(date)){
            Date date2 = string2Date(date, currentpattern.PATTERN);
            if(date2!= null){
                result = date2String(date2, formatPattern.PATTERN);
            }
        }
        return StringUtils.isEmpty(result)?date:StringUtils.trimToEmpty(result);
    }
    
    public static String formatDate(String date,DateFormatEnum formatPattern){
        return formatDate(date, DateFormatEnum.YYYY_MM_DDHH_MM_SS, formatPattern);
     }
    
    public static String formateESBDate(String date,DateFormatEnum formatPattern){
        return formatDate(date, DateFormatEnum.YYMMDDHHMMSS, formatPattern);
     }
    
    /**
     * 把 yyyy-MM-dd HH:mm:ss 格式时间转换成formatPattern格式的
     * @author mayc
     * @date 2014年4月23日 
     * @Time 下午1:10:08 
     * 
     * @param date yyyy-MM-dd HH:mm:ss 格式时间
     * @param formatPattern
     * @return 
     */
    public static String formatDefault(String date,String formatPattern){
       return formatDate(date, DateFormatEnum.DATA_FORMAT_DEFAULT, formatPattern);
    }
    
    public static Calendar string2Calendar(String strtime){
        Date d = string2Date(strtime);
        if(d != null){
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        }
        return null;
    }
    public static Date string2Date(String strtime){
        try{
        	SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormatEnum.DATA_FORMAT_DEFAULT);
            return dateFormat.parse(strtime, new ParsePosition(0));
        }catch(Exception e){
            e.printStackTrace();
            log.error(e);
        }
        return null;
    }
    public static Date string2Date(String strtime,String datepattern){
        try{
        	SimpleDateFormat dateFormat = getSimpleDateFormate(datepattern);
            return dateFormat.parse(strtime, new ParsePosition(0));
        }catch(Exception e){
            e.printStackTrace();
            log.error("formate date:"+strtime,e);
        }
        return null;
    }


    public static Timestamp string2Timestamp(String strtime,String datepattern){
       Date date = string2Date(strtime, datepattern);
       if(date!=null){
    	   return new Timestamp(date.getTime());
       }
       return null;
    }
    
    public static Timestamp string2Timestamp(String strtime,DateFormatEnum datepattern){
        Date date = string2Date(strtime, datepattern.PATTERN);
        if(date!=null){
     	   return new Timestamp(date.getTime());
        }
        return null;
     }
    
    public static String date2String(Date time){
        return date2String(time,null);
    }
    
    public static String date2String(Timestamp time){
        return date2String(time,null);
    }
    
    /**
     * 时间格式枚举 
     * User: mayc
     * Date: 2014年4月23日
     * Time: 上午9:54:22
     *
     */
   public  enum DateFormatEnum{
    	YYYYMMDD("yyyyMMdd"),
    	YYYYMMDDHHMMSS("yyyyMMddHHmmss"),
    	YYMMDDHHMMSS("yyMMddHHmmss"),
    	YYYY_MM_DDHH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    	YYYY_MM_DD("yyyy-MM-dd"),
    	YYMMDD("yyMMdd"),
    	HHMMSS("HHmmss"),
    	HH_MM_SS("HH:mm:ss"),
    	YYYYMM("yyyyMM"),
    	YYMM("yyMM");
    	
    	public String PATTERN;
    	DateFormatEnum(String pattern){
    		this.PATTERN = pattern;
    	}
    	private static DateFormatEnum getDefaultEnum(){
    		return YYYY_MM_DDHH_MM_SS;
    	}
    	public static String DATA_FORMAT_DEFAULT = getDefaultEnum().PATTERN;
    }

}


