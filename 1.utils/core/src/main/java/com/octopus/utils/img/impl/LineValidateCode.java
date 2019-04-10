package com.octopus.utils.img.impl;

import com.octopus.utils.img.IImgIdent;
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
 * Date: 14-11-20
 * Time: 下午4:39
 */
public class LineValidateCode implements IImgIdent {
    static transient Log log= LogFactory.getLog(LineValidateCode.class);
    static Map<String,BufferedImage> SINGLE_IMG_POINTLINE = new HashMap();

    //预处理
    public void preDeal(File f){
        try{
            BufferedImage img = ImageIO.read(f);
            int width = img.getWidth();
            int height = img.getHeight();
            //去背景,贴边
            int left=-1,top=-1;
            List<int[]> singlePoint = new ArrayList<int[]>();
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (isRemoveColor(img.getRGB(x, y))) {
                        img.setRGB(x, y, Color.WHITE.getRGB());
                    }else{
                        if(top==-1)top=y;
                        if(left==-1)left=x;
                        if(top>y)top=y;
                        if(left>x)left=x;
                        singlePoint.add(new int[]{x,y});
                    }
                }
            }
            BufferedImage n = new BufferedImage(width-left,height-top,BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < n.getWidth(); ++x) {
                for (int y = 0; y <n.getHeight(); ++y) {
                    n.setRGB(x,y,Color.WHITE.getRGB());
                }
            }
            for(int[] p:singlePoint){
                n.setRGB(p[0]-left,p[1]-top,Color.BLACK.getRGB());
            }
            ImageIO.write(n,"png",f);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public LineValidateCode(String pngpath){
        try{
            File d = new File(pngpath);
            File[] fs=d.listFiles();
            for(File f:fs){
                preDeal(f);
                SINGLE_IMG_POINTLINE.put(f.getName(),ImageIO.read(f));
            }
            if(SINGLE_IMG_POINTLINE.size()==0){
                log.error("need single char image in "+pngpath);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    boolean isRemoveColor(int rgb){
        int R = (rgb & 0xff0000) >> 16;
        int G = (rgb & 0xff00) >> 8;
        int B = (rgb & 0xff);
        if(R>=170 || B>=170 || G>=170 ){
            return true;
        }
        return false;
    }
    //旋转图片
    public static BufferedImage Rotate(Image src, int angel,int leftorright) {
        int src_width = src.getWidth(null);
        int src_height = src.getHeight(null);
        // calculate the new image size
        Rectangle rect_des = CalcRotatedSize(new Rectangle(new Dimension(
                src_width, src_height)), angel);

        BufferedImage res = null;
        res = new BufferedImage(rect_des.width, rect_des.height,
                BufferedImage.TYPE_INT_RGB);
        for(int i=0;i<rect_des.width;i++){
            for(int j=0;j<rect_des.height;j++){
                res.setRGB(i,j,Color.WHITE.getRGB());
            }
        }
        Graphics2D g2 = res.createGraphics();

        // transform
        g2.translate((rect_des.width - src_width) / 2,
                (rect_des.height - src_height) / 2);
        if(leftorright>0)
            g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);
        else
            g2.rotate(Math.toRadians(0 - angel), src_width / 2, src_height / 2);
        g2.drawImage(src, null, null);

        return res;
    }

    public static Rectangle CalcRotatedSize(Rectangle src, int angel) {
        // if angel is greater than 90 degree, we need to do some conversion
        if (angel >= 90) {
            if(angel / 90 % 2 == 1){
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }

        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);

        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha
                - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha
                - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new java.awt.Rectangle(new Dimension(des_width, des_height));
    }

    void match(BufferedImage img,Map<String,BufferedImage> map,double rate,int len,Map<Integer,String> ret){
        int h = img.getHeight();
        int w = img.getWidth();
        for(int x=0;x<w;x++){
            boolean isf=false;
            for(int y=0;y<h;y++){
                Iterator its = map.keySet().iterator();
                while(its.hasNext()){
                    String fileName = ((String)its.next());
                    String ch = fileName.substring(0,1);
                    BufferedImage s = map.get(fileName);

                    if(find(w,h,x,y,img,s,rate,ch,len,ret)){
                        isf=true;
                        if(len >0 && ret.size()==len)
                            return;
                    }else{

                        for(int i=1;i<30;i++){
                            BufferedImage sr = Rotate(s,i,1);
                            if(find(w,h,x,y,img,sr,rate,ch,len,ret)){
                                isf=true;
                                break;
                            }
                        }
                        if(!isf){
                            for(int i=1;i<30;i++){
                                BufferedImage sr = Rotate(s,i,-1);
                                if(find(w,h,x,y,img,sr,rate,ch,len,ret)){
                                    isf=true;
                                    break;
                                }
                            }
                        }

                    }
                    if(isf)
                        break;
                }
                if(isf){
                    x+=4;
                    break;
                }
            }
        }
    }

    boolean find(int w,int h,int x,int y,BufferedImage img,BufferedImage s,double rate,String ch,int len,Map<Integer,String> ret){
        if(y+s.getHeight()>h || x+s.getWidth()>w)return false;
        long size=0;
        for(int i=0;i<s.getWidth();i++){
            for(int j=0;j<s.getHeight();j++){
                if(s.getRGB(i,j)==Color.BLACK.getRGB())
                    size++;
            }
        }

        double onerate=0;
        for(int tx=0;tx<s.getWidth();tx++){
            for(int ty=0;ty<s.getHeight();ty++){
                if(x+tx<w && y+ty<h && img.getRGB(x+tx,y+ty)==Color.BLACK.getRGB() && s.getRGB(tx,ty)==Color.BLACK.getRGB()){
                    onerate++;
                }
            }
        }
        if((onerate/size)>rate){
            ret.put(x,ch);
            return true;
        }
        return false;
    }

    public String getValidatecode(BufferedImage image,int len,String tempdir){
        StringBuffer result = new StringBuffer();
        //BufferedImage image = ImageIO.read(new File("c:\\log\\var1415370040346.png"));
        int width = image.getWidth();
        int height = image.getHeight();
        //去掉背景色
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (isRemoveColor(image.getRGB(x, y))) {
                    image.setRGB(x, y, Color.WHITE.getRGB());
                }else{
                    image.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        try{
            //ImageIO.write(image,"png",new File("c:\\log\\lines\\a.png"));
        }catch (Exception e){
            e.printStackTrace();
        }
        //获取标准字符坐标。
        //不断的以中心旋转，上下扫描位置匹配图片中的字符，最高图片中的字符匹配率达到6成，确认该字符
        Map<Integer,String> ret = new TreeMap();
        try{
            match(image,SINGLE_IMG_POINTLINE,0.95,len,ret);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(ret.size()>0){
            StringBuffer sb = new StringBuffer();
            Iterator is = ret.keySet().iterator();
            while(is.hasNext()){
                sb.append(ret.get(is.next()));
            }
            return sb.toString();
        }
        return "";
    }

    public static void main(String[] args){
        try{
            LineValidateCode c = new LineValidateCode("c:\\log\\lines");
            File fs = new File("C:\\log\\temp");
            File[] ff = fs.listFiles();
            //for(File f:ff)
            //System.out.println(f.getName()+"  " +c.getValidatecode(ImageIO.read(f),4));
            System.out.println(c.getValidatecode(ImageIO.read(new File("C:\\log\\temp\\m_230530.png")),4,null));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
