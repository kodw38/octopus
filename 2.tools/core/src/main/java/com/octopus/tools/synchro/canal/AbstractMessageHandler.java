package com.octopus.tools.synchro.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-13
 * Time: 上午11:13
 */
public abstract class AbstractMessageHandler extends XMLDoObject implements ICanalMessageHandler {
    transient static Log log = LogFactory.getLog(AbstractMessageHandler.class);
    public AbstractMessageHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void handle(XMLParameter env,List<CanalEntry.Entry> entrys) throws Exception {
        for (CanalEntry.Entry entry : entrys) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            try {
                CanalEntry.RowChange rowChage = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChage.getEventType();
                Map oldData = new HashMap();
                Map newData = new HashMap();
                for (CanalEntry.RowData rowData : rowChage.getRowDatasList()) {
                    if (eventType == CanalEntry.EventType.DELETE) {
                        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                            newData.put(column.getName(),column.getValue());
                        }
                    } else if (eventType == CanalEntry.EventType.INSERT) {
                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            newData.put(column.getName(),column.getValue());
                        }
                    } else {
                        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                            oldData.put(column.getName(),column.getValue());
                        }
                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            newData.put(column.getName(),column.getValue());
                        }
                    }
                    doRecord(env,eventType.toString(),entry.getHeader().getSchemaName().toUpperCase()+"."+entry.getHeader().getTableName().toUpperCase(),oldData,newData);
                }
            } catch (Exception e) {
                log.error(e.getMessage(),e);
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                        e);
            }
        }
    }
    public abstract void  doRecord(XMLParameter env,String type,String tableCode,Map oldData,Map newData)throws Exception;
}
