package com.octopus.utils.img;

/**
 * User: Administrator
 * Date: 14-11-7
 * Time: 上午12:01
 */
public class ColPoint {
    public static int TYPE_Hli=1;
    public static int TYPE_HalfHli=2;
    public static int TYPE_Point=3;
    int startY;
    int type;
    int continueNumber;

    public ColPoint(int startY,int continueNumber){
        this.startY=startY;
        this.continueNumber=continueNumber;
    }
    public int getContinueNumber() {
        return continueNumber;
    }

    public void setContinueNumber(int continueNumber) {
        this.continueNumber = continueNumber;
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }
}
