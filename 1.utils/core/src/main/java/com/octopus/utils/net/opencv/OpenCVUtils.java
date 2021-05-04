package com.octopus.utils.net.opencv;/**
 * Created by admin on 2020/7/14.
 */

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;

import org.bytedeco.opencv.opencv_core.MatExpr;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
/**
 * @ClassName OpenCV
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/14 21:36
 * @Version 1.0
 **/
public class OpenCVUtils {
    public static void saveRtsp(String url) throws FrameGrabber.Exception {
        String file = "rtsp://192.168.2.38:5554/2";
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(file);
        grabber.setOption("rtsp_transport", "tcp"); // 使用tcp的方式，不然会丢包很严重
        // 一直报错的原因！！！就是因为是 2560 * 1440的太大了。。
        grabber.setImageWidth(960);
        grabber.setImageHeight(540);
        System.out.println("grabber start");
        grabber.start();
       // OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        // OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        while (true){
            Frame frame = grabber.grabImage();
            //Mat m = converter.convert(frame);
            System.out.println(frame.data.array().length);
        }


    }
    public static void main(String[] args){
        try{
            //OpenCVUtils.saveRtsp("http://192.168.1.18:81/stream");
            System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
            MatExpr mat = Mat.eye( 3, 3, CvType.CV_8UC1 );
            System.out.println( "mat = " + mat.asMat().toString() );
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
