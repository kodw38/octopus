package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

/**
 * User: wangfeng2
 * Date: 14-5-5
 * Time: 下午9:26
 */
public interface ClientConst {

    /*
      * 终端类型
      */
    public static class ClientKind{

        public static String PC="pc";
        public static String PHONE="phone";
        public static String PAD="pad";

    }

    /*
      * 浏览器名称
      */
    public static class Browser{

        public static String NAVIGATOR="NAVIGATOR";
        public static String FIREFOX="FIREFOX";
        public static String MSIE="MSIE";
        public static String CHROME="CHROME";
        public static String SAFARI="SAFARI";
        public static String OPERA="OPERA";

    }

    /*
      * 屏幕信息
      */
    public static class Screen{

        public static String SCREEN_WIDTH="SCREEN_WIDTH";//终端屏幕宽度
        public static String SCREEN_HEIGHT="SCREEN_HEIGHT";//终端屏幕高度
        public static String PIX_WIDTH="PIX_WIDTH";//像素宽度
        public static String PIX_HEIGHT="PIX_HEIGHT";//像素高度
    }
}
