package com.octopus.utils.img;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.img.impl.BackgroundValidateCode;
import com.octopus.utils.img.impl.LineValidateCode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-9-12
 * Time: 上午11:41
 */
public class ImgIdent {
    public static int VALIDATECODE_TYPE_BACKGROUND=0;
    public static int VALIDATECODE_TYPE_LINE=1;
    static transient Log log = LogFactory.getLog(ImgIdent.class);

    public static String getValidatecode(BufferedImage image,int type) throws Exception {
        if(type==VALIDATECODE_TYPE_BACKGROUND){
            return new BackgroundValidateCode().getValidatecode(image,type,"");
        }
        if(type==VALIDATECODE_TYPE_LINE){
            return new LineValidateCode(image).getValidatecode(image,type,"");
        }
        return "";
    }


    public static void main(String[] args){
        try{
            File f = new File("c:\\log");
            File[] fs = f.listFiles();
            for(File pf:fs){
                if(pf.getName().endsWith("png")){
                    //ImgIdent img = new ImgIdent(ImageIO.read(pf));
                    //System.out.println(pf.getName()+ "    "+img.getValidatecode());

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
