package com.octopus.tools.translate;

public class BaiDuTranslate {

    // 在平台申请的APP_ID 详见 http://api.fanyi.baidu.com/api/trans/product/desktop?req=developer
    private static final String APP_ID = "20180601000170645";
    private static final String SECURITY_KEY = "L9ZSNGxcI3BVLEif8Iny";

    public static String translate(String v,String language) {
        com.octopus.tools.translate.TransApi api = new com.octopus.tools.translate.TransApi(APP_ID, SECURITY_KEY);
        return api.getTransResult(v, "auto", language);
    }
    public static String translate(String v,String old,String language) {
        com.octopus.tools.translate.TransApi api = new com.octopus.tools.translate.TransApi(APP_ID, SECURITY_KEY);
        return api.getTransResult(v, old, language);
    }
}
