package com.octopus.utils.expression;

/**
 * Created by kod on 2017/5/18.
 */
public class LogicExpression {
    //operation order by priority
    final static char[] ops = new char[]{'*','/','%','+','-','>','<','=','!',']','[','&','|'};


    public static Object calExpression(String exp){
        return null;
    }

    public static void main(String[] args){
        System.out.println((true || 1==2 && 2==2));
    }
}
