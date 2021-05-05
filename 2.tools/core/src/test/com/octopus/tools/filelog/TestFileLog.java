package com.octopus.tools.filelog;/**
 * @program: treasurebag
 * @description: testcase
 * @author: liu yan
 * @create: 2021-05-04 07:15
 */

import com.octopus.tools.filelog.impl.ExpLogFileName;
import com.octopus.tools.filelog.impl.LineSizeRollEvent;
import junit.framework.TestCase;


/**
 *@program: treasurebag
 *@description: testcase
 *@author: wangfeng2
 *@create: 2021-05-04 07:15
 */
public class TestFileLog extends TestCase {
    public static void main(String[] args){
        try {
            MyLogger logger = new MyLogger("test","c:/logs/fileLog","c:/logs/fileLog/his"
                    ,new ExpLogFileName("c:/logs/fileLog","bill.tmp","bill")
                    ,new LineSizeRollEvent(1000),0,"UTF-8",null);
            for(int j=0;j<10001;j++){
                logger.addLog("1,2,3,4,5,6,7", null);
            }

            //SaveLogger save = SaveLogger.getMyLogger("tt","c:/logs/fileLog","c:/logs/fileLog","ttt","fff","yyyyMMddHHmmss",0,30,null,"utf-8");

        }catch (Exception e){

        }
    }
}
