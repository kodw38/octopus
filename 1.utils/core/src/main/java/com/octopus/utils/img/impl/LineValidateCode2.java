package com.octopus.utils.img.impl;

import com.octopus.utils.img.IImgIdent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-11-24
 * Time: 下午3:50
 */
public class LineValidateCode2 implements IImgIdent {

    class CharPoint{
        CharPoint(String c,int r){
            charpter=c;
            rightX=r;
        }
        String charpter;
        int rightX;

        public String getCharpter() {
            return charpter;
        }

        public void setCharpter(String charpter) {
            this.charpter = charpter;
        }

        public int getRightX() {
            return rightX;
        }

        public void setRightX(int rightX) {
            this.rightX = rightX;
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
    @Override
    public String getValidatecode(BufferedImage img, int len,String tempdir) {
        int width = img.getWidth();
        int height = img.getHeight();
        //去掉背景色
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (isRemoveColor(img.getRGB(x, y))) {
                    img.setRGB(x, y, Color.WHITE.getRGB());
                }else{
                    img.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for(int x=0;x<width;x++){
            for(int y=0;y<height;y++){
                if(img.getRGB(x,y)== Color.BLACK.getRGB()){
                    CharPoint c = findChar(img, x,y);
                    if(null !=c){
                        sb.append(c.getCharpter());
                        x=c.getRightX();
                        break;
                    }
                    break;
                }

            }
        }
        return sb.toString();
    }
    int[] findTop(BufferedImage img,int h,int w,int tx,int ty,Lines right){
        while(ty>0 && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            if(null != right){
                findAllRight(img, h, w, tx, ty, right);
            }
            if(ty-1>0 && img.getRGB(tx, ty - 1)==Color.BLACK.getRGB()){
                ty--;
                continue;
            }

            if(tx-1>0 && ty-1>0 && img.getRGB(tx-1,ty-1)==Color.BLACK.getRGB()){
                ty--;
                tx--;
                continue;
            }
            if(tx+1<w && ty-1>0 && img.getRGB(tx+1,ty-1)==Color.BLACK.getRGB()){
                ty--;
                tx++;
                continue;
            }
            break;
        }
        return new int[]{tx,ty};
    }
    int[] findButton(BufferedImage img,int h,int w,int tx,int ty,Lines right){
        while(ty<h && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            if(null != right){
                findAllRight(img, h, w, tx, ty, right);
            }
            if(ty+1<h && img.getRGB(tx,ty+1)==Color.BLACK.getRGB()){
                ty++;
                continue;
            }

            /*if(tx-1>0 && ty+1<h && img.getRGB(tx-1,ty+1)==Color.BLACK.getRGB()){
                ty++;
                tx--;
                continue;
            }*/
            if(tx+1<w && ty+1<h && img.getRGB(tx+1,ty+1)==Color.BLACK.getRGB()){
                ty++;
                tx++;
                continue;
            }
            break;
        }
        return new int[]{tx,ty};
    }
    boolean isOneLine(BufferedImage img,List<int[]> t1,List<int[]> t2){
        int sx = t1.get(0)[0]>t2.get(0)[0]?t1.get(0)[0]:t2.get(0)[0];
        int ex = t1.get(t1.size()-1)[0]>t2.get(t2.size()-1)[0]?t2.get(t2.size()-1)[0]:t1.get(t1.size()-1)[0];
        for(;sx<=ex;sx++){
            boolean is=false;
            for(int[] m1:t1){
                for(int[] m2:t2){
                    if(m1[0]==sx && m2[0]==sx){
                        int sy = m1[1]>m2[1]?m2[1]:m1[1];
                        int ey = m1[1]>m2[1]?m1[1]:m2[1];
                        for(;sy<=ey;sy++){
                            if(img.getRGB(sx,sy)!=Color.BLACK.getRGB()){
                                return false;
                            }
                        }
                        is=true;
                    }
                    if(is) break;
                }
                if(is) break;
            }
        }
        return true;
    }
    void findAllRight(BufferedImage img,int h,int w,int tx,int ty,Lines list){
        int sx = tx;
        int sy=ty;
        List<int[]> t0 = new ArrayList<int[]>();
        while(tx<w && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            t0.add(new int[]{tx,ty});
            if(tx+1<w && img.getRGB(tx+1,ty)==Color.BLACK.getRGB()){
                tx++;
                continue;
            }
            break;
        }
        int[] o0 = new int[]{tx,ty,sx,sy};
        tx=sx;
        ty=sy;
        List<int[]> t1 = new ArrayList<int[]>();
        while(tx<w && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            t1.add(new int[]{tx,ty});
            if(tx+1<w && img.getRGB(tx+1,ty)==Color.BLACK.getRGB()){
                tx++;
                continue;
            }
            if(tx+1<w && ty+1<h && img.getRGB(tx+1,ty+1)==Color.BLACK.getRGB()){
                ty++;
                tx++;
                continue;
            }
            if(tx+1<w && ty-1>0 && img.getRGB(tx+1,ty-1)==Color.BLACK.getRGB()){
                ty--;
                tx++;
                continue;
            }
            break;
        }
        int[] o1 = new int[]{tx,ty,sx,sy};
        tx=sx;
        ty=sy;
        List<int[]> t2 = new ArrayList<int[]>();
        while(tx<w && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            t2.add(new int[]{tx,ty});
            if(tx+1<w && ty+1<h && img.getRGB(tx+1,ty+1)==Color.BLACK.getRGB()){
                ty++;
                tx++;
                continue;
            }
            if(tx+1<w && img.getRGB(tx+1,ty)==Color.BLACK.getRGB()){
                tx++;
                continue;
            }
            break;
        }
        int[] o2 = new int[]{tx,ty,sx,sy};
        tx=sx;
        ty=sy;
        List<int[]> t3 = new ArrayList<int[]>();
        while(tx<w && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            t3.add(new int[]{tx,ty});
            if(tx+1<w && ty-1>0 && img.getRGB(tx+1,ty-1)==Color.BLACK.getRGB()){
                ty--;
                tx++;
                continue;
            }
            if(tx+1<w && img.getRGB(tx+1,ty)==Color.BLACK.getRGB()){
                tx++;
                continue;
            }
            break;
        }
        int[] o3 = new int[]{tx,ty,sx,sy};

        if(isOneLine(img, t1,t2) && isOneLine(img, t1,t3))
            addRight(img, sx,sy,list,o1,t1);
        if(isOneLine(img, t1,t2) && !isOneLine(img, t1,t3)){
            addRight(img, sx,sy,list,o1,t1);
            addRight(img, sx,sy,list,o3,t3);
        }
        if(!isOneLine(img, t1,t2) && (isOneLine(img, t1,t3)||isOneLine(img, t2,t3)) ){
            addRight(img, sx,sy,list,o1,t1);
            addRight(img, sx,sy,list,o2,t2);
        }
        if(!isOneLine(img, t1,t2) && !isOneLine(img, t1,t3) && !isOneLine(img, t2,t3) ){
            addRight(img, sx,sy,list,o1,t1);
            addRight(img, sx,sy,list,o2,t2);
            addRight(img, sx,sy,list,o3,t3);
        }

    }
    int[] findRight(BufferedImage img,int h,int w,int tx,int ty){
        int sx = tx;
        int sy=ty;
        while(tx<w && img.getRGB(tx,ty)==Color.BLACK.getRGB()){
            if(tx+1<w && img.getRGB(tx+1,ty)==Color.BLACK.getRGB()){
                tx++;
                continue;
            }
            if(tx+1<w && ty+1<h && img.getRGB(tx+1,ty+1)==Color.BLACK.getRGB()){
                ty++;
                tx++;
                continue;
            }
            if(tx+1<w && ty-1>0 && img.getRGB(tx+1,ty-1)==Color.BLACK.getRGB()){
                ty--;
                tx++;
                continue;
            }
            break;
        }
        return new int[]{tx,ty,sx,sy};
    }
    void addRight(BufferedImage img,int sx,int sy,Lines right,int[] r,List<int[]> t){
        if(Math.abs(r[0]-sx)<=6 && Math.abs(r[1]-sy)<=6)return;
        if(Math.abs(r[0]-sx)>25 || Math.abs(r[1]-sy)>25){
            if(null == right.getMaxTrace()){
                right.setMaxTrace(t);
                return;
            }else if(!isOneLine(img,right.getMaxTrace(),t)){
                List<int[]> tt=null;
                int o = right.getMaxTrace().get(0)[0];
                int n = t.get(0)[0];
                int dif =0;
                int of =0;
                if(n>o){
                   for(int[] oi : right.getMaxTrace()){
                       of++;
                       if(oi[0]==n)
                           break;
                   }
                }
                int nf=0;
                if(o>n){
                    for(int[] oi : t){
                        nf++;
                        if(oi[0]==o)
                            break;
                    }
                }
                if(right.getMaxTrace().size()-of>t.size()-nf){
                    tt = right.getMaxTrace();
                    right.setMaxTrace(t);
                }else{
                    tt = t;
                }
                List<int[]> ts = new ArrayList<int[]>();
                for(int k=0;k<tt.size();k++){
                    ts.add(tt.get(k));
                    if(tt.get(k)[0]>tt.get(0)[0]+6)
                       break;

                }
                r = new int[]{ts.get(ts.size()-1)[0],ts.get(ts.size()-1)[1],sx,sy};
                t =ts;
            }else{
                return;
            }
        }

        for(int i=0;i<right.getLs().size();i++){
            int[] a= right.getLs().get(i);
            if(a[0]==r[0] && a[1]==r[1]){
               return;
            }
            int p= Math.abs(a[0]-r[0]);
            int h = Math.abs(a[1]-r[1]);
            if(p<4 && h<4){
               return;
            }
            if(isOneLine(img,t,right.getLsTrace().get(i))){
                return;
            }
        }

        right.getLs().add(r);
        right.getLsTrace().add(t);
    }
    boolean isDirect(int[] top,int[] button){
        if(top[0]>button[0] && top[0]-button[0]<=6){
            return true;
        }
        if(top[0]<button[0] && button[0]-top[0]<=6){
            return true;
        }
        return false;
    }
    class Lines{
        List<int[]> ls = new ArrayList<int[]>();
        List<List<int[]>> lsTrace = new ArrayList();
        List<int[]> maxTrace ;

        public List<int[]> getLs() {
            return ls;
        }

        public void setLs(List<int[]> ls) {
            this.ls = ls;
        }

        public List<int[]> getMaxTrace() {
            return maxTrace;
        }

        public void setMaxTrace(List<int[]> maxTrace) {
            this.maxTrace = maxTrace;
        }

        public List<List<int[]>> getLsTrace() {
            return lsTrace;
        }

        public void setLsTrace(List<List<int[]>> lsTrace) {
            this.lsTrace = lsTrace;
        }
    }
    CharPoint findChar(BufferedImage img,int x,int y){
        int tx=x;
        int ty=y;
        int h = img.getHeight();
        int w = img.getWidth();

        Lines right=new Lines();
        int[] top=findTop(img,h,w,tx,ty,right);
        int[] button=findButton(img, h,w,tx,ty,right);

        if( Math.abs(top[0]-button[0])<=6
                &&Math.abs(top[1]-button[1])<=6)
            return null;

        if(right.getLs().size()>0 && null !=right.getMaxTrace()){
            for(int i=right.getLs().size()-1;i>=0;i--){
                 if(isOneLine(img,right.getLsTrace().get(i),right.getMaxTrace())){
                     right.getLs().remove(i);
                     right.getLsTrace().remove(i);
                 }
            }

        }
        boolean dir=isDirect(top,button);

        if(dir){
           if(right.getLs().size()==1){
               int[] r = right.getLs().get(0);
               if(Math.abs(r[1]-r[3])<6){
                   if(Math.abs(r[1]-top[1])<Math.abs(r[1]-button[1])){
                       int[] b = findButton(img,h,w,r[0],r[1],null);
                       if(Math.abs(b[1]-r[1])>6 && Math.abs(Math.abs(button[1]-top[1])-Math.abs(b[1]-r[1]))<6){
                           int[] br = findRight(img,h,w,b[0],b[1]);
                           if(br[0]-top[0]>20)
                               return new CharPoint("n",b[0]>r[0]?b[0]:r[0]);
                           else
                               return new CharPoint("n",br[0]>r[0]?br[0]:r[0]);
                       }else if(Math.abs(b[1]-r[1])>6 && Math.abs(Math.abs(button[1]-top[1])-Math.abs(b[1]-r[1]))>6){
                           int[] br = findRight(img,h,w,b[0],b[1]);
                           if(br[0]-top[0]>20)
                               return new CharPoint("q",b[0]>r[0]?b[0]:r[0]);
                           else
                               return new CharPoint("q",br[0]>r[0]?br[0]:r[0]);
                       }
                   }else{
                       int[] b = findTop(img,h,w,r[0],r[1],null);
                       int[] bt = findButton(img, h, w, r[0], r[1], null);
                       if(Math.abs(b[1]-r[1])>6 && Math.abs(Math.abs(button[1]-top[1])-Math.abs(b[1]-bt[1]))<=6){
                           int[] br = findRight(img,h,w,b[0],b[1]);
                           if(br[0]-top[0]>20)
                               return new CharPoint("u",b[0]>r[0]?b[0]:r[0]);
                           else
                               return new CharPoint("u",br[0]>r[0]?br[0]:r[0]);
                       }
                   }
               }else{
                   int[] t = findTop(img,w,h,r[0],r[1],null);
                   if(isDirect(r,t) && Math.abs(Math.abs(button[1]-top[1])-Math.abs(t[1]-r[1]))<6){
                       int[] tr = findRight(img,h,w,t[0],t[1]);
                       if(tr[0]-top[0]>20)
                           return new CharPoint("N",t[0]>r[0]?t[0]:r[0]);
                       else
                           return new CharPoint("N",tr[0]>r[0]?tr[0]:r[0]);
                   }
               }
           }else if(right.getLs().size()==2){

           }
        }
        return null;
    }

    public static void main(String[] args){
        try{
            LineValidateCode2 l = new LineValidateCode2();
            /*File fs = new File("C:\\log\\temp");
            for(File f:fs.listFiles()){
                BufferedImage im = ImageIO.read(f);
                String s = l.getValidatecode(im,4);
                System.out.println(f.getName()+":" +s);
                ImageIO.write(im,"png",f);
            }*/
            String s = l.getValidatecode(ImageIO.read(new File("C:\\\\log\\\\temp\\m_230441.png")),4,null);
            System.out.println(s);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
