package com.octopus.utils.xml.auto.logic;

import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-13
 * Time: 下午9:54
 */
public class TradeFinish implements ITradeFinish {
    static transient Log log = LogFactory.getLog(TradeFinish.class);
    XMLParameter par;

    public TradeFinish(){

    }

    public void setParameter(XMLParameter par) {
        this.par = par;
    }

    @Override
    public void run() {
        Map m = par.getTradeConsoles();
        if(null != m){
            Iterator its = m.values().iterator();
            while(its.hasNext()){
                try {
                    Object o = its.next();
                    if(null != o ){
                        if(Connection.class.isAssignableFrom(o.getClass())){
                            ((Connection)o).commit();
                            ((Connection)o).close();
                        }else{
                            log.error("now not support the reade console:"+o.getClass().getName());
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            par.removeTrade();
        }
    }

    public void clear(){
        par.removeTrade();
    }
}
