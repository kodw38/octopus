package com.octopus.utils.img;

import com.octopus.utils.img.impl.ImagePHash;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * User: wfgao_000
 * Date: 16-3-13
 * Time: 下午1:41
 */
public class ImageUtils {
    /**
     * 以背景图的右下角为起始点，向左上角增幅。
     * @param src  background image file
     * @param add  append image file
     * @param x    pix x>0 x more big more left
     * @param y    pix y>0 y more big more top
     * @param out  output file
     * @throws Exception
     */
    public static void addImage(InputStream src,InputStream add,int x,int y,OutputStream out)throws Exception{
            Image formerImage = ImageIO.read(src);
            //以下2行代码分别获得图片的宽(width)和高(height)
            int width = formerImage.getWidth(null);
            int height = formerImage.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
            Graphics g = image.createGraphics();
            g.drawImage(formerImage, 0, 0, width, height, null);

            Image waterMarkImage = ImageIO.read(add);
            int widthWMI = waterMarkImage.getWidth(null);
            int heightWMI = waterMarkImage.getHeight(null);
            g.drawImage(waterMarkImage, width - widthWMI - x, height - heightWMI - y, widthWMI,
                    heightWMI, null);

            g.dispose();
            //下面代码将被加上水印的图片转换为JPEG、JPG文件
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            encoder.encode(image);
            out.close();

    }
    public static void addImage(String backgroundImage,String appendImage,int x,int y,String targetImage) throws Exception {
        addImage(new FileInputStream(new File(backgroundImage)),new FileInputStream(new File(appendImage))
                ,x,y,new FileOutputStream(new File(targetImage)));
    }

    public static String getBase64String(String imgFilePath){
        InputStream in = null;
        byte[] data = null;
        // 读取图片字节数组
        try {
            in = new FileInputStream(imgFilePath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    /*public static float compareImage(String imageFile1Path,String imageFile2Path)throws Exception{
        Binarization b = new Binarization();
        FileInputStream is = new FileInputStream(new File(imageFile1Path));
        int a[][] = b.toBinarization(is);
        Distribution d = new Distribution();
        ArrayList<Square> s = d.toDistribution(a);
        System.out.println(s.size());

        FileInputStream is2 = new FileInputStream(new File(imageFile2Path));
        int a2[][] = b.toBinarization(is2);
        d = new Distribution();
        ArrayList<Square> s2 = d.toDistribution(a2);
        System.out.println(s.size());
        ImageCompare c= new ImageCompare();
        double m1 = c.toCompare(s, s2, b.getArea());
        double m2 = c.toCompare(s2,s, b.getArea());
        System.out.println("'''''" +m1 + "," +m2 +"," );
    }*/

    public static float compareImages(String f1,String f2) throws Exception {
        float percent = compare(getData(f1),
                getData(f2));
        if (percent == 0) {
           throw new Exception("无法比较");
        } else {
            return percent;
        }
    }
    private static int[] getData(String name) {
        try {
            BufferedImage img = ImageIO.read(new File(name));
            BufferedImage slt = new BufferedImage(100, 100,
                    BufferedImage.TYPE_INT_RGB);
            slt.getGraphics().drawImage(img, 0, 0, 100, 100, null);
            // ImageIO.write(slt,"jpeg",new File("slt.jpg"));
            int[] data = new int[256];
            for (int x = 0; x < slt.getWidth(); x++) {
                for (int y = 0; y < slt.getHeight(); y++) {
                    int rgb = slt.getRGB(x, y);
                    Color myColor = new Color(rgb);
                    int r = myColor.getRed();
                    int g = myColor.getGreen();
                    int b = myColor.getBlue();
                    data[(r + g + b) / 3]++;
                }
            }
            // data 就是所谓图形学当中的直方图的概念
            return data;
        } catch (Exception exception) {
            System.out.println("有文件没有找到,请检查文件是否存在或路径是否正确");
            return null;
        }
    }

    public static float compare(int[] s, int[] t) {
        try {
            float result = 0F;
            for (int i = 0; i < 256; i++) {
                int abs = Math.abs(s[i] - t[i]);
                int max = Math.max(s[i], t[i]);
                result += (1 - ((float) abs / (max == 0 ? 1 : max)));
            }
            return (result / 256) * 100;
        } catch (Exception exception) {
            return 0;
        }
    }

    //比较两个图片的相似度，如果不相同的数据位不超过5，就说明两张图片很相似；如果大于10，就说明这是两张不同的图片。
    public static int comparePHash(String f1,String f2)throws Exception{
        ImagePHash p = new ImagePHash();
        String image1;
        String image2;
        try {
            image1 = p.getHash(new FileInputStream(new File(
                    f1)));
            image2 = p.getHash(new FileInputStream(new File(
                    f2)));
            return p.distance(image1, image2);

        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
           throw e;
        }
    }
    //比较两个图片的相似度，如果不相同的数据位不超过5，就说明两张图片很相似；如果大于10，就说明这是两张不同的图片。
    public static int comparePHash(String f1,ByteArrayOutputStream f2)throws Exception{
        ImagePHash p = new ImagePHash();
        String image1;
        String image2;
        FileInputStream fi=null;
        try {
            fi = new FileInputStream(new File(
                    f1));
            image1 = p.getHash(fi);
            image2 = p.getHash(new ByteArrayInputStream(f2.toByteArray()));
            return p.distance(image1, image2);

        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }finally {
            if(null != fi)
            fi.close();
        }
    }
    public static void main(String[] args){

        try {
            //ImageUtils.addImage("c://log/bg.jpg","c://log/add.jpg",200,400,"c://log/chg.jpg");
            //String s = ImageUtils.getBase64String("C:\\Users\\Administrator\\Pictures\\storage_light_box.png");
            System.out.println(compareImages("C:\\work\\tb_workspace\\logs\\images\\camera_20200705194008.jpg"
                    ,"C:\\work\\tb_workspace\\logs\\images\\camera_20200705194010.jpg"));
            System.out.println(comparePHash("C:\\work\\tb_workspace\\logs\\images\\camera_20200705194008.jpg"
                    ,"C:\\work\\tb_workspace\\logs\\images\\camera_20200705194010.jpg"));
            System.out.println(comparePHash("C:\\work\\tb_workspace\\logs\\images\\camera_20200705194008.jpg"
                    ,"C:\\work\\tb_workspace\\logs\\images\\camera_20200705195833.jpg"));

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
