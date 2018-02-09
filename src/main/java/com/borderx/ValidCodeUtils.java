package com.borderx;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by borderx on 2018/2/7.
 */
public class ValidCodeUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidCodeUtils.class);

    public static String base64Valid(String base64) {
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] decoderBytes = decoder.decodeBuffer(base64);
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(decoderBytes));
            BufferedImage bo = removeLine(bi, 2);

            Tesseract instance = new Tesseract();
            //instance.setDatapath("/usr/local/Cellar/tesseract/3.05.01/share/tessdata");
            instance.setDatapath("E:\\java\\workspace\\tessdata");
            //将验证码图片的内容识别为字符串
            String result = instance.doOCR(bo);
            String finalResult = result.replaceAll("\\W", "");
            logger.info(result.trim() + "------>" + finalResult);
            return finalResult;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TesseractException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static BufferedImage removeLine(BufferedImage img, int px) {
        if (img != null) {
            img = removeX(img, px);
            img = removeY(img, px);
            img = removeX(img, px);
        }
        return img;
    }

    private static BufferedImage removeX(BufferedImage img, int px) {
        int width = img.getWidth();
        int height = img.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int count = 0;
                List<Integer> list = new ArrayList<>();
                while (y < height - 1) {
                    if (isBlack(img.getRGB(x, y))) {
                        count++;
                        y++;
                    } else {
                        img.setRGB(x, y, Color.white.getRGB());
                        break;
                    }
                }
                if (count <= px && count > 0) {
                    for (int i = 0; i <= count; i++) {
                        list.add(y - i);
                    }
                }
                if (list.size() != 0) {
                    for (int i = 0; i < list.size(); i++) {
                        img.setRGB(x, list.get(i), Color.white.getRGB());
                    }
                }
            }
        }
        return img;
    }

    private static BufferedImage removeY(BufferedImage img, int px) {
        int width = img.getWidth();
        int height = img.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;
                List<Integer> list = new ArrayList<>();
                while (x < width - 1) {
                    if (isBlack(img.getRGB(x, y))) {
                        count++;
                        x++;
                    } else {
                        img.setRGB(x, y, Color.white.getRGB());
                        break;
                    }
                }
                if (count <= px && count > 0) {
                    for (int i = 0; i <= count; i++) {
                        list.add(x - i);
                    }
                }
                if (list.size() != 0) {
                    for (int i = 0; i < list.size(); i++) {
                        img.setRGB(list.get(i), y, Color.white.getRGB());
                    }
                }
            }
        }
        return img;
    }

    public static boolean isBlack(int rgb) {
        Color c = new Color(rgb);
        int b = c.getBlue();
        int r = c.getRed();
        int g = c.getGreen();
        int sum = r + g + b;
        if (sum < 210) {
            return true;
        }
        return false;
        //sum的值越小（最小为零，黑色）颜色越重，
        //sum的值越大（最大值是225*3）颜色越浅，
        //sum的值小于10就算是黑色了.
    }
}
