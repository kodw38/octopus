package com.octopus.utils.img;

import java.awt.image.BufferedImage;

/**
 * User: Administrator
 * Date: 14-11-20
 * Time: 下午6:18
 */
public interface IImgIdent {

    public String getValidatecode(BufferedImage img,int len,String temppath) throws Exception;
}
