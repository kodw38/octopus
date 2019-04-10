package com.octopus.tools.jvminsmgr.ds;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午4:38
 */
public class SynResults {
    Object[] results;

    public boolean waitAllFinished(){
        return true;
    }

    public Object[] getResults() {
        return results;
    }

    public void setResults(Object[] results) {
        this.results = results;
    }
}
