package com.octopus.tools.dataclient.v2.ds;

/**
 * User: wfgao_000
 * Date: 15-11-16
 * Time: 上午11:32
 */
public class TableStore extends com.octopus.utils.ds.TableBean {
    String dataSource;
    String suffixRule;
    String storeExpress;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getSuffixRule() {
        return suffixRule;
    }

    public void setSuffixRule(String suffixRule) {
        this.suffixRule = suffixRule;
    }

    public String getStoreExpress() {
        return storeExpress;
    }

    public void setStoreExpress(String storeExpress) {
        this.storeExpress = storeExpress;
    }
}
