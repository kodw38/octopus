package com.octopus.utils.alone.impl;

import java.util.LinkedList;
import java.util.List;

/**
 * User: wfgao_000
 * Date: 16-7-29
 * Time: 下午1:26
 */
public class SplitStruct {
    String supplus;
    List points =new LinkedList();

    public String getSupplus() {
        return supplus;
    }

    public void setSupplus(String supplus) {
        this.supplus = supplus;
    }

    public List getPoints() {
        return points;
    }

    public void setPoints(List points) {
        this.points = points;
    }
}
