package com.octopus.utils.img;

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

    public static InputStream toBase64Image(String imgContentStr){
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            // Base64解码
            byte[] b = decoder.decodeBuffer(imgContentStr);
            return new ByteArrayInputStream(b);
        }catch(Exception e){

        }
        return null;

    }

    public static void main(String[] args){

        try {
            //ImageUtils.addImage("c://log/bg.jpg","c://log/add.jpg",200,400,"c://log/chg.jpg");
            String s = ImageUtils.getBase64String("C:\\Users\\Administrator\\Pictures\\storage_light_box.png");
            System.out.println(s);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
