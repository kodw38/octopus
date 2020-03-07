package com.octopus.utils.expression;

import com.octopus.utils.alone.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: robai
 * Date: 2009-4-15
 * Time: 11:19:05
 */
public class BaseExpression {
    transient Log log = LogFactory.getLog(BaseExpression.class);
    /*
       + 加
       - 减
       * 乘
       / 如果两个数字都是自然数-整除，如果有一个是double，结果是double
       @ 除法商
       % 除法余数
       > 大于
       < 小于
       ] 大于等于
       [ 小于等于
       ！不等于
       | 或
       & 和
       = 等于

     */
    public static String OPTS = "+-*/%><][!|&=#@";
    public static String SPECIAL = "-";

    public Object arithmatic(String expression)throws Exception{

        return "";
    }

    public Object calculate(String expression) throws Exception {
        try {
            Stack Opts = new Stack();
            Stack Values = new Stack();
            String exp = expression + "#";
            int nCount = exp.length(), nIn, nOut, nTemp;
            Opts.push("#");
            String temp = "", optOut = "", optIn = "", value1 = "", value2 = "",
                    optTemp = "", opt = "", temp1 = "";
            int nFun = 0;
            int e_point = 0; // 1 in left , 2 in op, 3 in right
            String prevalue="";
            boolean isFun = false;
            for (int i = 0; i < nCount; ) {
                nTemp = 0;
                opt = exp.substring(i, i + 1);
                isFun = false;
                temp1 = "";
                while (i < nCount) {
                    if (!temp1.equals("")) {
                        if (opt.equals("(")) {
                            nFun++;
                            isFun = true;
                        } else if (opt.equals(")")) {
                            nFun--;
                        }
                    }
                    //前一个值或负号
                    if ((nFun > 0)
                            || (nTemp == 0 && (!isFun) &&  isSpecialValue(opt)) && (
                                (StringUtils.isBlank(temp) || temp.equals("("))
                                || (StringUtils.isNotBlank(temp) && isOpt(temp) && StringUtils.isNotBlank(prevalue) && isNumber(prevalue)))//负号,是减号还是负号,负号在操作符左边右边
                            || (nTemp > 0 && (!isFun) && StringUtils.isNotBlank(temp1) && !isNumber(temp1) && isSpecialValue(opt))
                            || ((!isFun) && this.isValue(opt))) {
                        temp1 += opt;
                        nTemp++;
                        opt = exp.substring(i + nTemp, i + nTemp + 1);
                    } else {
                        if (isFun) {
                            temp1 += opt;
                            nTemp++;
                        }
                        break;
                    }
                }
                if (temp1.equals("")) {
                    temp = opt;
                } else {
                    temp = temp1;
                }
                if (nTemp > 0) {
                    i = i + nTemp - 1;
                }
                temp = temp.trim();

                if (this.isValue(temp)) {
                    temp = this.getValue(temp);
                    Values.push(temp);
                    prevalue=temp;
                    if(e_point==2) {
                        e_point = 3;
                    }else {
                        e_point = 1;
                    }
                    i++;
                } else {
                    optIn = Opts.pop().toString();
                    nIn = this.getOptPriorityIn(optIn);
                    nOut = this.getOptPriorityOut(temp);
                    if (nIn == nOut) {
                        i++;
                    } else if (nIn > nOut) {
                        String ret = "";
                        if (!Values.isEmpty()) {
                            value1 = Values.pop().toString();

                        } else {
                            value1 = "";
                        }
                        if (e_point!=2 && !Values.isEmpty()) {
                            value2 = Values.pop().toString();

                        } else {
                            value2 = "";
                        }
                        if(temp.equals(")")){
                            Opts.pop();
                            i++;
                        }

                        ret = String.valueOf(this.calValue(value2, optIn, value1));
                        Values.push(ret);
                        prevalue=ret;
                        e_point=1;

                    } else if (nIn < nOut) {
                        Opts.push(optIn);
                        Opts.push(temp);

                        if(temp.equals("(")){
                            e_point=0;
                        }else if(temp.equals(")")){
                            e_point=0;
                        }else{
                            if(e_point==0){
                                Values.push("");
                                prevalue="";
                                e_point=1;
                            }else if(e_point==1) {
                                e_point = 2;
                            }
                        }
                        i++;
                    }
                }
            }
            if (!Values.isEmpty())
                return Values.pop();
        } catch (Exception e) {
            throw new Exception("expression calculate[" + expression + "]error!", e);
        }
        return null;
    }

    protected int getOptPriorityOut(String opt) throws Exception {
        if (opt.equals("+")) {
            return 1;
        } else if (opt.equals("-")) {
            return 2;
        } else if (opt.equals("*")) {
            return 5;
        } else if (opt.equals("/")) {
            return 6;
        } else if (opt.equals("%")) {
            return 7;
        } else if (opt.equals("@")) {
            return 30;
        } else if (opt.equals(">")) {
            return 11;
        } else if (opt.equals("<")) {
            return 12;
        } else if (opt.equals("]")) {
            return 13;
        } else if (opt.equals("[")) {
            return 14;
        } else if (opt.equals("!")) {
            return 15;
        } else if (opt.equals("|")) {
            return 16;
        } else if (opt.equals("&")) {
            return 23;
        } else if (opt.equals("=")) {
            return 25;
        } else if (opt.equals("#")) {
            return 0;
        } else if (opt.equals("(")) {
            return 1000;
        } else if (opt.equals(")")) {
            return -1000;
        }
        throw new Exception("not support operation[" + opt + "]!");
    }

    protected int getOptPriorityIn(String opt) throws Exception {
        if (opt.equals("+")) {
            return 3;
        } else if (opt.equals("-")) {
            return 4;
        } else if (opt.equals("*")) {
            return 8;
        } else if (opt.equals("/")) {
            return 9;
        } else if (opt.equals("%")) {
            return 10;
        } else if (opt.equals("@")) {
            return 31;
        } else if (opt.equals(">")) {
            return 17;
        } else if (opt.equals("<")) {
            return 18;
        } else if (opt.equals("]")) {
            return 19;
        } else if (opt.equals("[")) {
            return 20;
        } else if (opt.equals("!")) {
            return 21;
        } else if (opt.equals("|")) {
            return 22;
        } else if (opt.equals("&")) {
            return 24;
        } else if (opt.equals("=")) {
            return 26;
        } else if (opt.equals("(")) {
            return -1000;
        } else if (opt.equals(")")) {
            return 1000;
        } else if (opt.equals("#")) {
            return 0;
        }
        throw new Exception("not support operation[" + opt + "]");
    }

    protected String getOPTS() {
        return OPTS;
    }

    boolean isSpecialValue(String cValue) {
        return SPECIAL.indexOf(cValue) >= 0;
    }

    protected boolean isValue(String cValue) {
        String notValue = this.getOPTS() + "()";
        return notValue.indexOf(cValue) == -1;
    }

    protected boolean isOpt(String value) {
        return this.getOPTS().indexOf(value) >= 0;
    }

    protected Object calValue(String value1, String opt, String value2) throws Exception {
        try {
            if (null == value1 || null == value2) return 0;
            if (isNumber(value1) && value1.length()<16 && isNumber(value2) && value2.length()<16) {
                if (value1.startsWith("f")) value1 = value1.replace("f", "-");
                if (value2.startsWith("f")) value2 = value2.replace("f", "-");

                BigDecimal dbValue1 = new BigDecimal(Double.valueOf(value1).doubleValue());
                BigDecimal dbValue2 = new BigDecimal(Double.valueOf(value2).doubleValue());
                long lg = 0;
                if (opt.equals("+")) {
                    if (isInt(value1) && isInt(value2))
                        return (dbValue1.add(dbValue2)).longValue();
                    else
                        return (dbValue1.add(dbValue2)).doubleValue();
                } else if (opt.equals("-")) {
                    if (isInt(value1) && isInt(value2))
                        return (dbValue1.subtract(dbValue2)).longValue();
                    else
                        return (dbValue1.subtract(dbValue2)).doubleValue();
                } else if (opt.equals("*")) {
                    if (isInt(value1) && isInt(value2))
                        return (dbValue1.multiply(dbValue2)).longValue();
                    else
                        return (dbValue1.multiply(dbValue2)).doubleValue();
                } else if (opt.equals("/")) {
                    if (isInt(value1) && isInt(value2)) {
                        if(dbValue2.doubleValue()==0){
                            return 0;
                        }
                        return (dbValue1.divide(dbValue2, 2, RoundingMode.CEILING));
                    }else
                        return dbValue1.divide(dbValue2,2, RoundingMode.CEILING).doubleValue();
                } else if (opt.equals("@")) {
                    return (dbValue1.divideAndRemainder(dbValue2))[0].longValue();
                } else if (opt.equals("%")) {
                    return dbValue1.divideAndRemainder(dbValue2)[1].longValue();
                } else if (opt.equals(">")) {
                    if (dbValue1.doubleValue() > dbValue2.doubleValue())
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("<")) {
                    if (dbValue1.doubleValue() < dbValue2.doubleValue())
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("]")) {
                    if (dbValue1.doubleValue() >= dbValue2.doubleValue())
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("[")) {
                    if (dbValue1.doubleValue() <= dbValue2.doubleValue())
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("!")) {
                    if (!dbValue1.equals(dbValue2))
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("|")) {
                    if (dbValue1.doubleValue() > 0 || dbValue2.doubleValue() > 0)
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("&")) {
                    if (dbValue1.doubleValue() > 0 && dbValue2.doubleValue() > 0)
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("=")) {
                    if (dbValue1.equals(dbValue2))
                        return 1;
                    else
                        return 0;
                }
            } else {
                //if (StringUtils.isBlank(value1) || StringUtils.isBlank(value2)) return 0;
                if (opt.equals("=") || opt.equals(">") || opt.equals("<") || opt.equals(">=") || opt.equals("<=")) {
                    int s = value2.indexOf("?");
                    int e = value2.lastIndexOf(":");
                    String s1=null,s2=null;
                    if (s > 0 && e > s) {
                        s1 = value2.substring(s + 1, e);
                        s2 = value2.substring(e + 1);
                        value2=value2.substring(0, s);
                    }
                    double r = 0;
                    if (opt.equals("=")) {
                        if (value1.equals(value2))
                            r=1;
                        else
                            r=0;
                    } else if (opt.equals("<")) {
                        if (isNumber(value1) && isNumber(value2)) {
                            r = Double.parseDouble(value2) - Double.parseDouble(value1);
                        } else {
                            r = value2.compareTo(value1);
                        }
                    } else if (opt.equals(">")) {

                        if (isNumber(value1) && isNumber(value2)) {
                            r = Double.parseDouble(value1) - Double.parseDouble(value2);
                        } else {
                            r = value1.compareTo(value2);
                        }

                    }else{
                        return 0;
                    }

                    if (r > 0) {
                        if(null == s1)
                            return (int)r;
                        else
                            return s1;
                    } else {
                        if(s2==null)
                            return (int)r;
                        else
                            return s2;
                    }
                } else if (opt.equals("!")) {
                    if (!value1.equals(value2))
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("&")) {
                    if (StringUtils.isTrue(value1) && StringUtils.isTrue(value2))
                        return 1;
                    else
                        return 0;
                } else if (opt.equals("|")) {
                    if (StringUtils.isTrue(value1) || StringUtils.isTrue(value2))
                        return 1;
                    else
                        return 0;
                }

            }
        } catch (Exception e) {
            log.error("error", e);
            throw new Exception("ֵ" + value1 + " " + value2 + " " + opt + ". happen error");
        }
        //return value1+value2;
        throw new Exception("not support operation[" + opt + "]");
    }

    protected boolean isNumber(String str) {
        if (StringUtils.isBlank(str)) return false;
        if (str.charAt(0) == 'f') {
            str = str.substring(1);
        }
        //Pattern pattern = Pattern.compile("[0-9/.]*");
        Pattern pattern = Pattern.compile("^\\d+$|^\\d+\\.\\d+$|-\\d+$|^-\\d+\\.\\d+$");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;

    }

    protected boolean isInt(String str) {
        if (str.charAt(0) == 'f') {
            str = str.substring(1);
        }
        if ("0".equals(str)) return true;
        //Pattern pattern = Pattern.compile("[0-9/.]*");
        Pattern pattern = Pattern.compile("[1-9][0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;

    }

    protected String getValue(String oldValue) throws Exception {
        String reg = "^([a-zA-Z0-9_]+)\\(([a-zA-Z0-9_.()]+)\\)$";
        if (this.isFunctionCal(oldValue)) {
            Pattern p = Pattern.compile(reg);
            Matcher m = p.matcher(oldValue);
            m.find();
            return calFunction(m.group(1), m.group(2));
        }
        return oldValue;
    }

    protected boolean isFunctionCal(String value) {
        String reg = "^([a-zA-Z0-9_]+)\\(([a-zA-Z0-9_.()]+)\\)$";
        return value.matches(reg);
    }

    protected String calFunction(String function, String value) throws Exception {
        String lowerFun = function.toLowerCase();
        double db = 0;
        try {
            db = Double.valueOf(this.getValue(value)).doubleValue();
            if (lowerFun.equals("log")) {
                return String.valueOf(Math.log(db));
            } else if (lowerFun.equals("square")) {
                return String.valueOf(Math.pow(db, 2));
            } else if (lowerFun.equals("sqrt")) {
                return String.valueOf(Math.sqrt(db));
            } else if (lowerFun.equals("sin")) {
                return String.valueOf(Math.sin(db));
            } else if (lowerFun.equals("asin")) {
                return String.valueOf(Math.asin(db));
            } else if (lowerFun.equals("cos")) {
                return String.valueOf(Math.cos(db));
            } else if (lowerFun.equals("tan")) {
                return String.valueOf(Math.tan(db));
            } else if (lowerFun.equals("atan")) {
                return String.valueOf(Math.atan(db));
            } else if (lowerFun.equals("ceil")) {
                return String.valueOf(Math.ceil(db));
            } else if (lowerFun.equals("exp")) {
                return String.valueOf(Math.exp(db));
            }
        } catch (Exception e) {
            throw new Exception("not support function[" + function + "ֵ" + value + "]!");
        }
        throw new Exception("not support function[" + function + "]");
    }

    public static void main(String[] args) {
        BaseExpression be = new BaseExpression();
        //String exp = "sin(ceil(sqrt(100)))*29+20+30*3+0|0|1+1&1*5+2=2";
        //String exp = "(10+11)*8/10+(10*2)";
        //String exp = "true|(true&true)";

        String exp0 = "(000005=000005)|(000005=600015)";
        String exp = "'000005'='000005'|'000005'='600015'";
        String exp1 = "5-2";
        String exp2 = "-2+3";
        String exp3 = "0.0 > 0";
        String exp4 = "1!1";
        String exp5 = "05>14|05=14";
        String exp6 = "(5>14)|(5=14)";
        try {
            /*System.out.println(be.calculate(exp0));
            System.out.println(be.calculate(exp));
            System.out.println(be.calculate(exp1));
            System.out.println(be.calculate(exp2));
            System.out.println(be.calculate(exp3));
            System.out.println(be.calculate(exp4));
            System.out.println(be.calculate(exp5));
            System.out.println(be.calculate(exp6));
            System.out.println(be.calculate("false|false"));
            System.out.println(be.calculate("tb_log.20170704.out>tb_log.20170630.out"));
            System.out.println(be.calculate("INS-NJ_115!INS-WANGFENG"));
            System.out.println(be.calculate("true|false"));
            System.out.println(be.calculate("true|(1=0)"));
            System.out.println(be.calculate("true|(1=1)"));
            System.out.println(be.calculate("true|(false=false)"));
            System.out.println(be.calculate("false|(false=false)"));
            System.out.println(be.calculate("false|!false"));
            System.out.println(be.calculate("true|(false=false)"));
            System.out.println(be.calculate("(1=1)|(1=0)"));
            System.out.println(be.calculate("1|(=0)"));
            System.out.println(be.calculate("1=1|0=1"));
            System.out.println(be.calculate("false|true"));
            System.out.println(be.calculate("-1=-1"));
            System.out.println(be.calculate("-1|-1"));
            System.out.println(be.calculate("(true&(-1|-1))"));
            System.out.println(be.calculate("true&(-1=-1)"));
            System.out.println(be.calculate("(true&(-1=-1))"));
            System.out.println(be.calculate("(true|(-1|-1))"));
            System.out.println(be.calculate("(true&(-1|-1))"));
            System.out.println(be.calculate("-1+-1"));*/
            System.out.println(be.calculate("(82/100)>0.9"));
        } catch (Exception eE) {
            System.out.println(eE.getMessage());
        }
    }
}
