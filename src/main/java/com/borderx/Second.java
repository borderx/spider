package com.borderx;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by borderx on 2018/2/5.
 */
public class Second {

    public static void main(String[] args) throws IOException, TesseractException {
//        while(true) {
            //First.buy0("1855391615213116804", 0);
//        }

//
//
//        System.out.println("end");

        String s = "fdfdfdf()[]dfdfdf";
        String s1 = s.replaceAll("\\W", "");
        System.out.println(s1);


//        File imageFile = new File("/Users/borderx/job/img/test10.jpeg");
//        Tesseract instance = new Tesseract();
//        instance.setDatapath("/usr/local/Cellar/tesseract/3.05.01/share/tessdata");
//        //将验证码图片的内容识别为字符串
//        String result = instance.doOCR(imageFile);
//        System.out.println(result);

//        File imageFile = new File("/Users/borderx/job/img/test10.jpeg");
//        BufferedImage bi = ImageIO.read(new File("/Users/borderx/job/img/WechatIMG52.jpeg"));
//        BufferedImage br = removeLine(bi, 2);
//        ImageIO.write(br, "jpeg", imageFile);
//        System.out.println("end");

    }







}
