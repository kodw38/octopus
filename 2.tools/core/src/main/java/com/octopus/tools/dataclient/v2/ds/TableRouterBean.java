package com.octopus.tools.dataclient.v2.ds;

/**
 * User: wfgao_000
 * Date: 15-12-6
 * Time: 下午1:10
 */
public class TableRouterBean {
    String dataSource;
    String tableName;
    String splitExpress;
    String routeExpress;
    String splitRange;
    String routerId;

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getSplitRange() {
        return splitRange;
    }

    public void setSplitRange(String splitRange) {
        this.splitRange = splitRange;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSplitExpress() {
        return splitExpress;
    }

    public void setSplitExpress(String splitExpress) {
        this.splitExpress = splitExpress;
    }

    public String getRouteExpress() {
        return routeExpress;
    }

    public void setRouteExpress(String routeExpress) {
        this.routeExpress = routeExpress;
    }
}
