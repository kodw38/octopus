package com.octopus.utils.cachebatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>Title: Asiainfo Portal System</p>
 * <p>Description: </p>
 * <p>日期时间公共方法类</p>
 * <p>该类提供了日期格式,处理等功能。</p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: Asiainfo Technologies (China),Inc.HangZhou</p>
 *
 * @author Asiainfo PSO/yuanjq
 * @version 1.0
 */
public class DateTimeUtil {
    public static final SimpleDateFormat DATA_FORMAT_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat DATA_FORMAT_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat DATA_FORMAT_YYMMDDHHMMSS = new SimpleDateFormat("yyMMddHHmmss");
    public static final SimpleDateFormat DATA_FORMAT_YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat DATA_FORMAT_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATA_FORMAT_YYMMDD = new SimpleDateFormat("yyMMdd");
    public static final SimpleDateFormat DATA_FORMAT_HHMMSS = new SimpleDateFormat("HHmmss");
    public static final SimpleDateFormat DATA_FORMAT_HH_MM_SS = new SimpleDateFormat("HH:mm:ss");

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

    private static final Log logger = LogFactory.getLog(DateTimeUtil.class);

    /**
     * 得到当前日期，格式yyyy-MM-dd。
     * FrameWork使用
     *
     * @return String 格式化的日期字符串
     */
    public static String getCurrDate() {
        return getFormattedDate(getDateByString(""));
    }

    /**
     * 对输入的日期字符串进行格式化,
     * 如果输入的是0000/00/00 00:00:00则返回空串.
     * FrameWork使用
     *
     * @param strDate     String 需要进行格式化的日期字符串
     * @param strFormatTo String 要转换的日期格式
     * @return String 经过格式化后的字符串
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
        switch (strDate.trim().length()) {
            case 6:
                if (strDate.substring(0, 1).equals("0")) {
                    formatStr = "yyMMdd";
                } else {
                    formatStr = "yyyyMM";
                }
                break;
            case 8:
                formatStr = "yyyyMMdd";
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
                    formatStr = "yyyy年MM月dd日";
                } else {
                    return "";
                }
            case 14:
                formatStr = "yyyyMMddHHmmss";
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
                return strDate.trim();
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(formatStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(formatter.parse(strDate));
            formatter = new SimpleDateFormat(strFormatTo);
            return formatter.format(calendar.getTime());
        } catch (Exception e) {
            //Common.printLog("转换日期字符串格式时出错;" + e.getMessage());
            return "";
        }
    }

    /**
     * 根据传入的日期字符串转换成相应的日期对象，
     * 如果字符串为空或不符合日期格式，则返回当前时间。
     * FrameWork使用
     *
     * @param strDate String 日期字符串
     * @return java.sql.Timestamp 日期对象
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
        	logger.error(ex.getMessage());
            return getCurrentDate();
        }
    }

    //获取当前数据库时间
    public static Timestamp getCurrentDate() {
        //return ServiceManager.getOpDateTime();
    	return new Timestamp(new Date().getTime());


    }

    /**
     * 对输入的日期按照默认的格式yyyy-MM-dd转换.
     * FrameWork使用
     *
     * @param dtDate java.sql.Timestamp 需要进行格式化的日期字符串
     * @return String 经过格式化后的字符串
     */
    public static String getFormattedDate(Timestamp dtDate) {
        return getFormattedDate(dtDate, "yyyy-MM-dd");
    }

    /**
     * 对输入的日期进行格式化, 如果输入的日期是null则返回空串.
     * FrameWork使用
     *
     * @param dtDate      java.sql.Timestamp 需要进行格式化的日期字符串
     * @param strFormatTo String 要转换的日期格式
     * @return String 经过格式化后的字符串
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
        	logger.error("转换日期字符串格式时出错;" + e.getMessage());
            return "";
        }
    }

    /**
     * 得到当前日期时间,格式为yyyy-MM-dd hh:mm:ss.
     * FrameWork使用
     *
     * @return String
     */
    public static String getCurrDateTime() throws Exception {
        Timestamp date = getCurrentDate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    public static Date getDate(String date) throws ParseException {
        return DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.parse(date);
    }
    /**
     * add by liufeng 20061111
     * 得到当前日期时间,格式为yyyyMMddhhmmss.
     * FrameWork使用
     *
     * @return String
     */
    public static String getCurrDateTime_yyyymmddhhmmss() throws Exception {
        Timestamp date = getCurrentDate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }

    /**
     * 返回两个日期之间隔了多少天
     *
     * @param date1
     * @param date2
     */
    public static int DateDiff(Date date1, Date date2) {
        int i = (int) ((date1.getTime() - date2.getTime()) / 3600 / 24 / 1000);
        return i;
    }

    /**
     * 月份相加 add by yuanjq
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
     * 当前日期增加iDays天后日期 add by liufeng 20061208
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
        return DateTimeUtil.getFormattedDate(tsEndDateAdd, strFormatTo);
    }
    /**
     * 当前日期增加iDays天后日期 add by liufeng 20061208
     *
     * @param      String
     * @param iDays       int
     * @param  String
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
     * 当前日期增加或减去minute分钟后日期 add by zhourh 2009-08-20
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
     * 得到本月最后一天 add by yuanjq
     *
     * @param timest1
     * @return
     */
    public static Timestamp getLastDayOfMonth(Timestamp timest1) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timest1);
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 0);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 得到当前日期，格式yyyy年MM月dd日。
     *
     * @param strDate    String
     * @param bNextMonth boolean
     * @return String
     */
//    public static String getCNToday(String strDate, boolean bNextMonth) {
//        String strToday = strDate;
//        int iYear = TypeChange.getStrToInt(strToday.substring(0, 4));
//        int iMonth = TypeChange.getStrToInt(strToday.substring(5, 7));
//        int iDay = TypeChange.getStrToInt(strToday.substring(8, 10));
//        if (!bNextMonth) {
//            return iYear + "年" + iMonth + "月" + iDay + "日";
//        } else {
//            if (iMonth < 12) {
//                iMonth++;
//                return iYear + "年" + iMonth + "月1日";
//            } else {
//                iYear++;
//                return iYear + "年1月1日";
//
//            }
//
//        }
//    }
    /**
     * 得到当前日期 MM月dd日。
     *
     * @param strDate    String
     * @return String
     */
//    public static String getDateMMDD(String strDate) {
//        String strToday = strDate;
//        int iMonth = TypeChange.getStrToInt(strToday.substring(5, 7));
//        int iDay = TypeChange.getStrToInt(strToday.substring(8, 10));
//        return iMonth + "月" + iDay + "日";
//    }
    /**
     * 得到今天日期，格式yyyy-MM-dd。
     *
     * @return String 格式化的日期字符串
     */
    public static String getToday() {
        Date cDate = new Date();
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

    /**
     * 得到昨天日期，格式yyyy-MM-dd。
     *
     * @return String 格式化的日期字符串
     */
    public static String getYesterday() {
        Date cDate = new Date();
        cDate.setTime(cDate.getTime() - 24 * 3600 * 1000);
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

    /**
     * 得到明天日期，格式yyyy-MM-dd。
     *
     * @return String 格式化的日期字符串
     */
    public static String getTomorrow() {
        Date cDate = new Date();
        cDate.setTime(cDate.getTime() + 24 * 3600 * 1000);
        SimpleDateFormat cSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return cSimpleDateFormat.format(cDate);
    }

    /**
     * 得到指定的日期，如一年三个月零九天后(以yyyy/MM/dd格式显示)参数为("yyyy/MM/dd",1,3,9)
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

    /**
     * 得到当前日期格式yyyyMM转换。
     *
     * @return String 经过格式化后的字符串
     */
    public static String getCurrentYearMonth() throws Exception {
        return getYearMonth(getCurrentDate());
    }

    /**
     * 对输入的日期进行默认的格式yyyyMM转换。
     *
     * @return String 经过格式化后的字符串
     */
    public static String getYearMonth(Timestamp dtDate) {
        return getFormattedDate(dtDate, "yyyyMM");
    }

    /**
     * 渠道使用
     * 修改时间 chg by liufeng 20050714 修改为年、月、日+传入的时、分、秒  统一去掉以前的时、分、秒
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
     *
     * @param strDate
     * @unit YEAR,DAY,HOUR,MINUTE,SECOND
     * @return
     */
    public static boolean isLeftOrRight(String strDate,Integer range,String unit) throws ParseException {
        Date a = getDate(strDate);
        Date n = new Date();
        if("DAY".equals(unit)){
            return (n.after(addOrMinusDays(a.getTime(),-range)) && n.before(addOrMinusDays(a.getTime(),range)));
        }else if("HOUR".equals(unit)){
            return (n.after(addOrMinusHours(a.getTime(),-range)) && n.before(addOrMinusHours(a.getTime(),range)));
        }else if("MINUTE".equals(unit)){
            return (n.after(addOrMinusMinutes(a.getTime(),-range)) && n.before(addOrMinusMinutes(a.getTime(),range)));
        }else if("SECOND".equals(unit)){
            return (n.after(addOrMinusSecond(a.getTime(),-range)) && n.before(addOrMinusSecond(a.getTime(),range)));
        }
        return false;
    }

    /**
     * add by liufeng 20061031 strFormat值如："yyyy-MM-dd HH:mm:ss"
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
     * 得到当前时间（前或后）iDays天的日期
     *
     * @param strFormat String　日期格式比如　yyyy-MM-dd
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
     * 得到当前时间前(后)几个月的第一天的日期
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
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_YEAR){
    		return getWholeYearDate(d);
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_MONTH){
    		return getWholeMonthDate(d);
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_DAY){
    		return getWholeDayDate(d);
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_HOUR){
    		return getWholeHourDate(d);
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_MINUTE){
    		return getWholeMinuteDate(d);
    	}
    	throw new Exception("not support the whole type:"+type+".please see DateTimeUtil#DATE_CYCLE_TYPE_XXXX");
    }

    public static Date[][] getBetweenWholeDate(int type,int cycle,Date currentDate,Date preDate,int starttype)throws Exception{
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_YEAR){
    		Date oldyear = null;
    		if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_TIME){
    			oldyear = preDate;
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldyear = DateTimeUtil.getWholeYearDate(preDate);
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldyear = DateTimeUtil.getWholeYearDate(preDate);
    		}else{
    			oldyear = preDate;
    		}
    		int count = (int)DateTimeUtil.betweenYear(oldyear, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldyear;
    			rtn[i][1] = DateTimeUtil.addOrMinusYear(oldyear.getTime(), cycle);
    			oldyear = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_MONTH){
    		Date oldmonth = null;
    		if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_TIME){
    			oldmonth = preDate;
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldmonth = DateTimeUtil.getWholeMonthDate(preDate);
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldmonth = DateTimeUtil.getWholeYearDate(preDate);
    		}else{
    			oldmonth = preDate;
    		}

    		int count = (int)DateTimeUtil.betweenMonth(oldmonth, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldmonth;
    			rtn[i][1] = DateTimeUtil.addOrMinusMonth(oldmonth.getTime(), cycle);
    			oldmonth = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_DAY){
    		Date oldday = null;
    		if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_TIME){
    			oldday = preDate;
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldday = DateTimeUtil.getWholeDayDate(preDate);
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldday = DateTimeUtil.getWholeMonthDate(preDate);
    		}else{
    			oldday = preDate;
    		}

    		int count = (int)DateTimeUtil.betweenDay(oldday, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldday;
    			rtn[i][1] = DateTimeUtil.addOrMinusDays(oldday.getTime(), cycle);
    			oldday = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_HOUR){
			Date oldhour = null;
    		if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_TIME){
    			oldhour = preDate;
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldhour = DateTimeUtil.getWholeHourDate(preDate);
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldhour = DateTimeUtil.getWholeDayDate(preDate);
    		}else{
    			oldhour = preDate;
    		}

    		int count = (int)DateTimeUtil.betweenHour(oldhour, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldhour;
    			rtn[i][1] = DateTimeUtil.addOrMinusHours(oldhour.getTime(), cycle);
    			oldhour = rtn[i][1];
    		}
    		return rtn;
    	}
    	if(type==DateTimeUtil.DATE_CYCLE_TYPE_MINUTE){
    		Date oldminute = null;
    		if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_TIME){
    			oldminute = preDate;
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_CUR_WHOLE_TIME){
    			oldminute = DateTimeUtil.getWholeMinuteDate(preDate);
    		}else if(starttype==DateTimeUtil.DATE_CYCLE_START_PARENT_WHOLE_TIME){
    			oldminute = DateTimeUtil.getWholeHourDate(preDate);
    		}else{
    			oldminute = preDate;
    		}

    		int count = (int)DateTimeUtil.betweenMinute(oldminute, currentDate);
    		int len = count/cycle;
    		Date[][] rtn = new Date[len][2];
    		for(int i=0;i<len;i++){
    			rtn[i][0] = oldminute;
    			rtn[i][1] = DateTimeUtil.addOrMinusMinutes(oldminute.getTime(), cycle);
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
        int offset = (getOffset(date.getTime(), dest) - getOffset(date.getTime(), src));
        newDate.setTime(date.getTime() - offset);
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
    	return DateTimeUtil.getYear(end) -DateTimeUtil.getYear(start);
    }
    public static long betweenMonth(Date start, Date end) {
    	return DateTimeUtil.getYear(end) * 12 + DateTimeUtil.getMonth(end) -(DateTimeUtil.getYear(start) * 12 + DateTimeUtil.getMonth(start));
    }

    private static int getDSTSavings(TimeZone tz) {
        if (tz.useDaylightTime()) {
            return 3600000;
        }
        return 0;
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
     * 获得时间的字符串
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
     * 获得时间的字符串
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
     * 根据时间串获得时间对象
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
     * 在一个时间上加上对应的年
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
     * 在一个时间上加上对应的月份数
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
     * 在一个时间上加上或减去周
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
     * 在一个时间上加上或减去天数
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
     * 在一个时间上加上或减去小时
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
     * 在一个时间上加上或减去分钟
     *
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
     * 在一个时间上加上或减去秒数
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
     * 根据指定的日期获取下个月的第一天的时间
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
     * 根据指定的日期获取上个月的第一天的时间
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
     * 将带有时间类型的日期转换成不带时间的日期
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
     * 根据指定日期获取该月的最后一天的最后时间点
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
     * 根据指定日期获取当天的最后的时间点
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
     * 根据指定日期获取前一天的最后的时间点
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
     * 根据指定日期获取下一天的开始的时间点
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
     * 根据指定日期获取某天的开始的时间点
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
     * 将时间格式化为YYYY-MM-DD
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
     * 将时间格式化为YYYY-MM-DD
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
     * 处理计费月时间
     *
     * @return
     */
    public static Timestamp getBillMonthDate(Date beginDate, Date endDate) {
        if (null == beginDate) {
            return null;
        }
        //获取结束时间的月底时间
        Timestamp monthEndDate = new Timestamp(addOrMinusDays(getDateOfNextMonthFirstDay(endDate).getTime(), -1).getTime());
        return new Timestamp(monthEndDate.getTime());
    }



/*
    public static long getHHMMSSSecond(Date date)throws Exception{
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY)*60*60 + c.get(Calendar.MINUTE)*60 + c.get(Calendar.SECOND);
    }
*/

    public static long getHHMMSSSecond(String HHmmss)throws Exception{
        String[] sp = HHmmss.split("\\:");
        long hour = Integer.parseInt(sp[0]);
        long minute = Integer.parseInt(sp[1]);
        long second = Integer.parseInt(sp[2]);
        return hour*60*60 + minute*60 + second;
    }

    public static void main(String[] args) throws Exception {

/*
        System.out.println(DateTimeUtil.getDayOfWeek(DateTimeUtil.DATA_FORMAT_YYYYMMDD.parse("20100124")));
        System.out.println(DateTimeUtil.betweenDay(DateTimeUtil.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.parse("2010-01-16 00:00:00"),DateTimeUtil.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.parse("2010-01-17 23:00:00")));

        System.out.println(DateTimeUtil.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.format(DateTimeUtil.addSpaceTime(DateTimeUtil.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.parse("2010-01-25 13:00:00"),3600*2,"22:00:00","08:00:00")));
*/

//        System.out.println(getCurrentDate());
    }

    /**
     * 将指定的日期取整
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
     * 根据指定的日期获取月的第一天的时间
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
     * 获取当前时间的最后时间点
     */
    /**
     * 根据指定日期获取最后时间点
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

    /**
     * 在开始时间上增加指定的妙数,考虑非工作时间,周六周日,节假日时间
     * @param start     开始时间
     * @param addsecond 增加的妙
     * @param starthhmmss  非工作开始时间
     * @param endhhmmss    非工作结束时间
     * @return
     * @throws Exception
     */
/*    public static Date addSpaceTime(Date start, long addsecond, String starthhmmss, String endhhmmss) throws Exception {
        Date endTime=null;
        long ss = getHHMMSSSecond(starthhmmss);
        long se = getHHMMSSSecond(endhhmmss);
        if(se<ss){//隔天
            se=se+24*60*60;
        }
        endTime = start;

        long s1 = getHHMMSSSecond(endTime);
        if(s1>= ss && s1 < se){
            endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(se-s1));
        }
        int curweekindex = DateTimeUtil.getDayOfWeek(endTime);
        if(curweekindex==7){//星期六
            endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(2*24*60*60 - getHHMMSSSecond(endTime)));
        }
        if(curweekindex==1){//星期日
            endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(24*60*60 - getHHMMSSSecond(endTime)));
        }

        long count = addsecond;
        for(int i=0;i<count;i++){
            endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),1);
            s1 = getHHMMSSSecond(endTime);
            if(s1>= ss && s1 < se){
                endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(se-s1));
            }

            curweekindex = DateTimeUtil.getDayOfWeek(endTime);
            if(curweekindex==7){//星期六
                endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(2*24*60*60 - getHHMMSSSecond(endTime)));
            }
            if(curweekindex==1){//星期日
                endTime = DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)(24*60*60 - getHHMMSSSecond(endTime)));
            }

        }


*//*
        Date endTime = DateTimeUtil.addOrMinusSecond(start.getTime(),(int)addsecond);
        //处理非工作时间
        long overday = DateTimeUtil.betweenDay(start,endTime);
        int weekcount = DateTimeUtil.getCountInWeekDay(start,endTime);
        int curweekindex = DateTimeUtil.getDayOfWeek(start);
        int endweekindex = DateTimeUtil.getDayOfWeek(endTime);
        long s1 = getHHMMSSSecond(start);
        long s2 = getHHMMSSSecond(endTime);
        long ss = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(starthhmmss));
        long se = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(endhhmmss));

        long temp = 0;
        long cycle = overday;

        if(curweekindex != 1 && curweekindex != 7 && (endweekindex==1 || endweekindex== 7)){

            if(endweekindex==7){
                temp += 24*60*60*(weekcount+1);
            }else{
                temp += 24*60*60*(weekcount);
            }

        }else if((curweekindex==1 || curweekindex==7) && (endweekindex != 1 && endweekindex !=7)){//开始时间就在周日，周六

            temp += 24*60*60*(weekcount-1) + (24*60*60 - getHHMMSSSecond(start));

        }else if((curweekindex==1 || curweekindex==7) && (endweekindex == 1 || endweekindex ==7)){

            temp += 24*60*60*(weekcount-1) + (24*60*60 - getHHMMSSSecond(start));

        }else{
            if(weekcount>0){
                temp += 24*60*60*weekcount;
            }
        }

        if(se<ss){//隔天
            se=se+24*60*60;
        }
        temp += cycle*(se-ss);

        if(s2<s1){
            s2=s2+24*60*60;
        }
        if(s1<ss && s2>=ss && s2<=se){
            temp += se-ss;
        }
        if(s1<ss && s2>se){
            temp += se-ss;
        }
        if(s1>=ss && s1<= se){
            temp += se-s1;
        }

        Date restTime =  DateTimeUtil.addOrMinusSecond(endTime.getTime(),(int)temp);
*//*

        return endTime;
    }*/

    /**
     * 求两时间秒差,除去非工作日,周六周日,节假日时间
     * @param start
     * @param endTime
     * @param starthhmmss
     * @param endhhmmss
     * @return
     * @throws Exception
     */
/*    public static long secondSpaceTime(Date start, Date endTime, String starthhmmss, String endhhmmss) throws Exception {
        Date tempDate = start;
        long intersecond=0;
        long ss = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(starthhmmss));
        long se = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(endhhmmss));
        if(se<ss){//隔天
            se=se+24*60*60;
        }

        long s1 = getHHMMSSSecond(tempDate);
        if(s1>= ss && s1 < se){
            tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(se-s1));
        }
        int curweekindex = DateTimeUtil.getDayOfWeek(tempDate);
        if(curweekindex==7){//星期六
            tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(2*24*60*60 - getHHMMSSSecond(tempDate)));
        }
        if(curweekindex==1){//星期日
            tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(24*60*60 - getHHMMSSSecond(tempDate)));
        }

        while(tempDate.before(endTime)){
            intersecond += 1;
            tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),1);
            s1 = getHHMMSSSecond(tempDate);
            if(s1>= ss && s1 < se){
                tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(se-s1));
            }
            curweekindex = DateTimeUtil.getDayOfWeek(tempDate);
            if(curweekindex==7){//星期六
                tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(2*24*60*60 - getHHMMSSSecond(tempDate)));
            }
            if(curweekindex==1){//星期日
                tempDate = DateTimeUtil.addOrMinusSecond(tempDate.getTime(),(int)(24*60*60 - getHHMMSSSecond(tempDate)));
            }

        }

*//*        long dursecond = DateTimeUtil.betweenSecond(start,endTime);//需要减去非工作时间、周末时间、节假日时间


        //处理非工作时间
        long overday = DateTimeUtil.betweenDay(start,endTime);
        int weekcount = DateTimeUtil.getCountInWeekDay(start,endTime);
        int curweekindex = DateTimeUtil.getDayOfWeek(start);
        int endweekindex = DateTimeUtil.getDayOfWeek(endTime);
        long s1 = getHHMMSSSecond(start);
        long s2 = getHHMMSSSecond(endTime);
        long ss = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(starthhmmss));
        long se = getHHMMSSSecond(DateTimeUtil.DATA_FORMAT_HH_MM_SS.parse(endhhmmss));


        long temp = 0;
        long cycle = overday;

        if(curweekindex != 1 && curweekindex != 7 && (endweekindex==1 || endweekindex== 7)){

            cycle = cycle - (weekcount-1);
            temp += 24*60*60*(weekcount-1) + (getHHMMSSSecond(endTime));

        }else if((curweekindex==1 || curweekindex==7) && (endweekindex != 1 && endweekindex !=7)){//开始时间就在周日，周六

            cycle = cycle - (weekcount-1);
            temp += 24*60*60*(weekcount-1) + (24*60*60 - getHHMMSSSecond(start));

        }else if((curweekindex==1 || curweekindex==7) && (endweekindex == 1 || endweekindex ==7)){

            cycle = cycle - (weekcount-1);
            temp += 24*60*60*(weekcount-1) + (24*60*60 - getHHMMSSSecond(start));

        }else{
            if(weekcount>0){
                cycle = cycle - (weekcount);
                temp += 24*60*60*weekcount;
            }
        }

        if(se<ss){//隔天
            se=se+24*60*60;
        }
        temp += cycle*(se-ss);

        if(s2<s1){
            s2=s2+24*60*60;
        }
        if(s1<ss && s2>=ss && s2<=se){
            temp += se-ss;
        }
        if(s1<ss && s2>se){
            temp += se-ss;
        }
        if(s1>=ss && s1<= se){
            temp += se-s1;
        }

        return dursecond - temp;*//*
        return intersecond;
    }*/
    /**
     * 得到当前日期，格式yyyy-MM-dd。
     * FrameWork使用
     *
     * @return String 格式化的日期字符串
     */
    public static Timestamp getYearLastDayTimeByDate(Timestamp tsDate)throws Exception {
 	   String strYear = getFormattedDate(tsDate,"yyyy");
 	   String strLastTime = strYear + "-12-31 23:59:59";
 	   Timestamp tsLastTime = null;
 	   return getDateByString(strLastTime);
    }
    
    /**
     * 当前系统时间(long型)
     * 
     * @return
     */
    public static long now() {
		return System.currentTimeMillis();
	}
}
