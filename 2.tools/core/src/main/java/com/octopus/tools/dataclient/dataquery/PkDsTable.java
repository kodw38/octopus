package com.octopus.tools.dataclient.dataquery;

import java.util.List;

/**
 * Created by robai on 2017/10/17.
 */
public class PkDsTable {
        String ds;
        String tableName;
        List pkv;

        public String getDs() {
            return ds;
        }

        public void setDs(String ds) {
            this.ds = ds;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List getPkv() {
            return pkv;
        }

        public void setPkv(List pkv) {
            this.pkv = pkv;
        }

}
