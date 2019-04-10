package com.octopus.utils.img.impl;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.img.ColPoint;
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
 * Time: 下午4:31
 */
public class BackgroundValidateCode implements IImgIdent{
    static transient Log log = LogFactory.getLog(BackgroundValidateCode.class);
    int[] removeBackRGB={-986383,-5592131,-1117455,-986637};
    public BackgroundValidateCode(){
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

    static char[] chars={'0','1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
    };
    static String[] ShortChars ={"j","i","h","2","t","p","T"};
    static HashMap<String,String> charMap=new HashMap<String, String>();
    static List<String> keyList= new ArrayList();
    static {
        charMap.put("1:151:150:1600I0I160.16.16.","1");
        charMap.put("1i1i0;00;0;0;0;0;10;1!30!","2");
        charMap.put("00;0;0;0;0;10;1!30!","2");
        charMap.put("1:140:1600;0;0;0;0;10`1!20!","3");
        charMap.put("10.9.08.06:505:603:802:901:1000I00I110.","4");
        charMap.put("0!0!0;0;0;0;0;0;0i0i","5");
        charMap.put("513I02;01;00;00;0;0;0;0;10i100.","6");
        charMap.put("0.0:160i0i0i0i0i0i0i0I01","7");
        charMap.put("8.200.15.012.00:1300:160`0:110:90;0:60:40:30.0","7");
        charMap.put("1112!01;01;0;00;0;0;0`10;20!1101","8");
        charMap.put("4.2!01;00;00;0;0;0;10;1;20I501","9");
        charMap.put("4.2!01;00`00`0;0;0;10;1;20I501","9");

        charMap.put(".i0;0;;;;0II0.;0","a");
        charMap.put("12.6i05;05;5;5;5;60I6I160.16.","a");
        charMap.put("12.6i05;05;5;5;5;60I6I160","a");
        charMap.put("0I0I70:86:905:1105:115:115:1060:86180.","b");
        charMap.put("917I06:805:1005:115:115:115:1160:9","c");
        charMap.put("8:5140.8:905;05:93`050;5;5;4`04;0I00I0;","d");
        charMap.put("8.7106:805:1105:115:115:1160:96:80I00I","d");
        charMap.put("5.9017I06;05;05;5;5;60;6!80!","e");
        charMap.put("917I06;05;05;5;5;60;6!80!","e");
        charMap.put("5.5.1I00I00:50:50:50.","f");
        charMap.put("9.7!06;05;05;5;5;60;6;5I05I","g");
        charMap.put("0I0I70.6.05.05.5.5.601701","h");
        charMap.put("0I70.6.05.05.5.5.601701","h");
        charMap.put("0i0i","i");
        charMap.put("20.210.21.21.0i00i","j");
        charMap.put("0i00i","j");
        charMap.put("0I0I110.10.09.08:407:606:805:1005:11170.","k");
        charMap.put("0I00!110:710:804:504;70:64`050;5`170:25:1101.030.","k");

        charMap.put("5I5I70.6.05.05.5.5.60I70I7.6.05.05.5.5.60I70I","m");
        charMap.put("5I5I70.6.05.05.5.5.60I70I","n");

        charMap.put("II0:8:90:110:11:11:100:810.","p");
        charMap.put("86:905:1105:115:115:1060:86180.","p");
        charMap.put("817106:805:1105:115:115:1160:96:85I05I","q");
        charMap.put("5I5I70.6.05.05.5.","r");
        charMap.put("7!6!05;05;5;5;5;60i1301","s");
        charMap.put("5.5.2I02I50:115:115:11","t");
        charMap.put("5.2I02I50:115:115:11","t");
        charMap.put("5I5I160.16.16.16.15.014.05I05I","u");
        charMap.put("I5I160.16.16.16.15.014.05I05I","u");
        charMap.put("5.5160I90I120I150112I09I06I05105.","v");
        charMap.put("5.5160I90I120I150112I08I05I0515I80I120I150112I09I06I05105.","w");
        charMap.put("60I90I120I150112I08I05I0515I80I120I150112I09I0","w");
        charMap.put("5:125!60!70!80!90I100I9!07!06!05!05:12","x");
        charMap.put("5.5I70I90I120!150I14I012I09I07I05105.","y");
        charMap.put("5i5i5i5;5;5;5;5;5!5!5!","z");

        charMap.put("17.151012I010I08I05!03!01!00!010!30!50!80I100I120I1501170.","A");
        charMap.put("17.15.012.010.08.05:603:801:1000:11010:1030:850:680.100.120.150.170","A");
        charMap.put("4.3:2050.130:41;020;30;0;00`30:81:1000;010:1030:850;80:105;0120.150.170","A");
        charMap.put("17.151012I010I08I05!03!01!00!010!30!50!80I100I120I","A");
        charMap.put("0I0I0;0;0;0;0;0`10;20:9120.","B");
        charMap.put("0I0I0`0;0;0;0;0;10;20:9120.150.","B");
        charMap.put("513I02:1001:1301:140:1500:160:160:160:160:160:160:1510:141:13","C");
        charMap.put("513I02:1001:1301:140:1500:160:160:160:160:160:160:1510:14","C");
        charMap.put("02:1001:1301:140:1500:160:160:160:160:160:160","C");
        charMap.put("0I0I0:160:160:160:160:160:160:160:1510:141:1320","D");
        charMap.put("0I0I0:160:160:160:160:160:160:160:1510:141","D");
        charMap.put("7;180.0I00I0:160`0:160:160:160:160;0:1510`1;20;30150.5`","D");
        charMap.put("0I0I0;0;0;0;0;0;0;0:16160.","E");
        charMap.put("0I0I0:80:80:80:80:80:80:80.","F");
        charMap.put("513I02:1001:1301:140:1500:160:160:160:160:160:1610i1i","G");
        charMap.put("0I0I80.8.8.8.8.8.8.8.8.8.0I00I","H");
        charMap.put("19.19.19.19.18.00I00I","J");
        charMap.put("0I0I80.7.06:305:504:703:902:1101:1300:1500:16170.","K");
        charMap.put("0I0I160.16.16.16.16.16.16.16.16.","L");
        charMap.put("0I0I0.20.40.70.90.120.140.12.09.07.04.02.00.00I0I","M");
        charMap.put("0I0I0.20.30.50.70.80.100.110.130.140.0I00I","N");
        charMap.put("02:1001:1301:140:1500:160:160:160:160:160:1510:141:1320:1030I501","O");
        charMap.put("1001:1301:140:1500:160:160:160:160:160:1510:141:1320:1030I5","O");
        charMap.put("0I0I0:80:80:80:80:80:710:51.20.","P");
        charMap.put("02:1001:1301:140:1500:160:160:160:160:160:1510:141;20;30!50","Q");
        charMap.put("0I0I0:80:80:80:80:80;10;1:1220:12160.170.","R");
        charMap.put("3!1!01;0;00;0;0;0;0;10i1101","S");
        charMap.put("0.0.0.0.0.0.0I0I0.0.0.0.0.0.","T");
        charMap.put("0.0.0.0I0I0.0.0.0.0.0.","T");
        charMap.put("0I0I140.150.160.16.16.16.16.15.014.00I00I","U");
        charMap.put("0.0110I40I60I90I120I140112I09I06I04I01I00100.","V");
        charMap.put("0.0.10.40.60.90.120.140.12.09.06.04.01.00","V");
        charMap.put("0:170:1610!20!30!50!60I7016I05!03!02!01!00:1600:17","X");
        charMap.put("160!0!0!0!0I01I0!0!0!0!0:160","X");
        charMap.put("1320:1130:850:560170.6105:503:902:1101:1300:1600:17","X");
        charMap.put("1320:1130:850:560.70.6.05:503:902:1101:1300:1600:17","X");
        charMap.put("0.0.10.20.40.50.70I7I6.04.02.01.00.00.","Y");
        charMap.put("1510i00i0;0;0;0;0;0;0;0!0!0!","Z");
        charMap.put("7.15011510;00i0;0;0;0;0;0;0;0!0;0!170.1.0","Z");
        charMap.put("1i0i;;;;;;;!!!","Z");
        keyList.addAll(charMap.keySet());
        Collections.sort(keyList,new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((String)o1).length()>((String)o2).length()?-1:1;
            }
        });
    }
    String reconize(LinkedList<ColPoint[]> scan){
        //find height
        int maxColHeith=0;
        int minStartY=0;
        for(ColPoint[] cps:scan){
            for(ColPoint cp:cps){
                if(cp.getContinueNumber()>maxColHeith)
                    maxColHeith=cp.getContinueNumber();
                if(cp.getStartY()<minStartY)
                    minStartY=cp.getStartY();
            }
        }
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<scan.size();i++){
            ColPoint[] cps = scan.get(i);
            int count = cps.length;
            String ma;
            if(count==1 && cps[0].getContinueNumber()>maxColHeith*0.7){
                ma="I";  //大竖
            }else if(count==1 && cps[0].getContinueNumber()>maxColHeith*0.4){
                ma="1";  //小竖
            }else if(count==2 && cps[0].getContinueNumber()>maxColHeith*0.4){
                ma="!";  //上竖，下点
            }else if(count==2 && cps[1].getContinueNumber()>maxColHeith*0.4){
                ma="i";  //上点，下竖
            }else if(count==1){
                ma=".";  //一个点
            }else if(count==2){
                int n = cps[1].getStartY()-cps[0].getStartY();
                ma=":"+n;  //两个点
            }else if(count==3){
                ma=";";  //三个点
            }else if(count==4){
                ma="`";
            }else{
                ma="?";
            }
            sb.append(cps[0].getStartY()-minStartY);
            if(i>0){
                if(scan.get(i-1)[0].getStartY()>cps[0].getStartY())
                    sb.append(ma+"0");
                else if(scan.get(i-1)[0].getStartY()==cps[0].getStartY())
                    sb.append(ma);
                else
                    sb.append("0"+ma);
            }else
                sb.append(ma);
        }
        String ret = sb.toString();
        //System.out.println(ret);
        TreeMap treeMap = new TreeMap();
        List<int[]> startIndex=new ArrayList<int[]>();
        for(String k:keyList){
            Integer[] is = StringUtils.indexOf(ret,k,startIndex);
            if(null !=is && (((double)k.length())/ret.length()>0.3 || ArrayUtils.isInStringArray(ShortChars, (String)charMap.get(k)) )){
                for(int i:is){
                    startIndex.add(new int[]{i,i+k.length()});
                    treeMap.put(i,charMap.get(k));
                }
            }
        }
        if(treeMap.size()>0){
            StringBuffer r = new StringBuffer();
            Iterator vs = treeMap.values().iterator();
            while(vs.hasNext())
                r.append(vs.next());
            return r.toString();
        }
        return "";
    }
    public String getValidatecode(BufferedImage image,int len,String temppath) throws Exception {
        try{
            StringBuffer result = new StringBuffer();
            if(null != temppath)
            ImageIO.write(image,"png",new File(temppath+"/login.png"));
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
            //从上往下逐行扫描，猜测
            LinkedList<ColPoint[]> scans= new LinkedList();
            LinkedList<ColPoint> cols= new LinkedList();
            boolean charStart=true;
            for (int x = 0; x < width; x++) {
                boolean isStart=true;
                int continuenum=0;
                int startY=0;
                for (int y = 0; y < height; y++) {
                    if(image.getRGB(x, y)==Color.BLACK.getRGB()){
                        if(isStart){
                            startY=y;
                        }
                        continuenum++;
                        isStart=false;
                    }
                    if(image.getRGB(x, y)!=Color.BLACK.getRGB() || y==height-1){
                        if(continuenum>0){
                            cols.add(new ColPoint(startY,continuenum));
                            continuenum=0;
                        }
                        isStart=true;
                    }

                }
                if(cols.size()>0){
                    scans.add(cols.toArray(new ColPoint[0]));
                    charStart=false;
                }else {
                    charStart=true;
                }
                cols.clear();
                if(charStart && scans.size()>0){
                    //根据已经扫描的列点判断
                    result.append(reconize(scans));
                    scans.clear();
                }


            }

            String ret = result.toString();
            if(ret.length()==len){
                return ret;
            }
            if(null != temppath) {
                System.out.println("please input login validate code in " + temppath + "/validate.txt");
                while (true) {
                    //System.out.println("----get---");
                    try {
                        StringBuffer sb = FileUtils.getFileContentStringBuffer(temppath + "/validate.txt");
                        if (null != sb) {
                            //System.out.println("==:" + sb.toString());
                            String s = sb.toString().trim();
                            if (StringUtils.isNotBlank(s)) {
                                System.out.println("load:" + s);
                                return s;
                            }
                        }
                        Thread.currentThread().sleep(60000);
                    } catch (Exception ex) {

                    }
                }
            }else
                throw new Exception("get error validate code:"+ret);

        }catch (Exception e){
            throw e;
        }
    }

    public static void main(String[] args){
        try{
            BackgroundValidateCode id = new BackgroundValidateCode();
            System.out.println(id.getValidatecode(ImageIO.read(new File("c:\\log\\var1415257387443.png")),4,null));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
