package com.borderx;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by borderx on 2018/2/5.
 */
public class Second {

    private static final Logger logger = LoggerFactory.getLogger(Second.class);


    public static void main(String[] args) throws IOException, TesseractException {
//        new Thread(() -> {
//            buy();
//        }).start();
//        buyRare();
        findRare();
//        rare();
    }

    public static void amount() {
        int count = 0;
        while(true) {
            count ++;
            int page = count;
            String listStr = First.request("https://pet-chain.baidu.com/data/market/queryPetsOnSale", "{\"pageNo\":" + page + ",\"pageSize\":20,\"querySortType\":\"AMOUNT_ASC\",\"petIds\":[],\"lastAmount\":null,\"lastRareDegree\":null,\"requestId\":1517729851864,\"appId\":1,\"tpl\":\"\"}");
            if (StringUtils.isNotBlank(listStr)) {
                JSONObject data = JSON.parseObject(listStr);
                if ("00".equals(data.getString("errorNo"))) {
                    boolean need = false;
                    JSONArray list = data.getJSONObject("data").getJSONArray("petsOnSale");
                    if(list.size() == 0) {
                        System.exit(-1);
                    }
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject object = list.getJSONObject(i);
                        int generation = object.getInteger("generation");
                        String petId = object.getString("petId");
                        double amount = object.getDouble("amount");
                        int rareDegree = object.getInteger("rareDegree");
                        if(amount >= 2000) {
                            System.exit(-1);
                        }
                        if(rareDegree > 0) {
                            logger.info("page:" + page + ",rareDegree:" + rareDegree + ",amount:" + amount + ",petId:" + petId);
                        }

                    }
                }
            } else {
                count --;
            }
            logger.info("page:" + page + " end -----------");
        }
    }

    public static void findRare() {
        int count = 0;
        List<Object> pets = Lists.newArrayList();
        boolean endFlag = false;
        while(!endFlag) {
            count ++;
            int page = count;
            String listStr = First.request("https://pet-chain.baidu.com/data/market/queryPetsOnSale", "{\"pageNo\":" + page + ",\"pageSize\":20,\"querySortType\":\"RAREDEGREE_DESC\",\"petIds\":[],\"lastAmount\":null,\"lastRareDegree\":null,\"requestId\":1517980498392,\"appId\":1,\"tpl\":\"\"}");
            if (StringUtils.isNotBlank(listStr)) {
                JSONObject data = JSON.parseObject(listStr);
                if ("00".equals(data.getString("errorNo"))) {
                    JSONArray list = data.getJSONObject("data").getJSONArray("petsOnSale");
                    if(list.size() == 0) {
                        endFlag = true;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject object = list.getJSONObject(i);
                        int generation = object.getInteger("generation");
                        String petId = object.getString("petId");
                        double amount = object.getDouble("amount");
                        int rareDegree = object.getInteger("rareDegree");
                        if(rareDegree <= 2) {
                            logger.info("end with rareDegree <= 2 ............");
                            endFlag = true;
                            break;
                        }
                        if((amount < 13000 && rareDegree == 3) || (amount < 30000 && rareDegree == 4)) {
                            Map<String, String> map = Maps.newHashMap();
                            map.put("generation", String.valueOf(generation));
                            map.put("petId", petId);
                            map.put("amount", String.valueOf(amount));
                            map.put("rareDegree", String.valueOf(rareDegree));
                            pets.add(map);
                            logger.info("add pet,size:" + pets.size());
                            if(pets.size() >= 1000) {
                                logger.info("end with pets.size() >= 1000 ............");
                                endFlag = true;
                                break;
                            }
                        }
                    }
                }
            } else {
                count --;
            }
            logger.info("page:" + page + " end -----------");
        }
        logger.info(JSON.toJSONString(pets));
    }

    private static LinkedBlockingDeque<Map<String, String>> pets = new LinkedBlockingDeque<>();

    public static void buy() {
        while (true) {
            logger.info("pets:" + pets.size() + "," + JSON.toJSONString(pets));
            if (pets.size() == 0) {
                try {
                    Thread.sleep(1000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Map<String, String> pet = pets.pop();
                int flag = buy0(pet.get("petId"), Integer.valueOf(pet.get("rareDegree")), pet.get("validCode"));
                if (flag != -2) {
                    pets.push(pet);
                }
            }
        }
    }

    public static int buy0(String petId, int rareDegree, String validCode) {
        String url = "https://pet-chain.baidu.com/data/pet/queryPetById";
        Map<String, Object> params = Maps.newHashMap();
        params.put("petId", petId);
        params.put("requestId", new Date().getTime());
        params.put("appId", "1");
        params.put("tpl", "");
        String result = First.request(url, JSON.toJSONString(params));
        if (StringUtils.isNotBlank(result)) {
            JSONObject data = JSON.parseObject(result);
            if ("00".equals(data.getString("errorNo"))) {
                String id = data.getJSONObject("data").getString("id");
                double amount = data.getJSONObject("data").getDouble("amount");
                int generation = data.getJSONObject("data").getInteger("generation");
                if (amount <= Double.valueOf(First.config.getProperty("rareDegree" + rareDegree)) && generation == 0) {
                    Map<String, String> validInfo = First.validInfo();
                    String seed = validInfo.get("seed");
                    String captcha = validInfo.get("captcha");
                    String buyStr = First.getBuyParam(petId, String.valueOf(amount), validCode, seed, captcha);
                    String res = First.request("https://pet-chain.baidu.com/data/txn/create", buyStr);
                    if (StringUtils.isNotBlank(res)) {
                        JSONObject b = JSON.parseObject(res);
                        if ("10002".equals(b.getString("errorNo"))) {
                            return -2;
                        }
                        if ("10003".equals(b.getString("errorNo"))) {
                            logger.info("buying-------------------------");
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if ("00".equals(b.getString("errorNo"))) {
                            logger.info("buy success,amount:" + amount + ",petId:" + petId + ",id:" + id);
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return -2;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static void buyRare() {
        int count = 0;
        while(true) {
            count ++;
            int page = count;
            String listStr = First.request("https://pet-chain.baidu.com/data/market/queryPetsOnSale", "{\"pageNo\":" + page + ",\"pageSize\":20,\"querySortType\":\"RAREDEGREE_DESC\",\"petIds\":[],\"lastAmount\":null,\"lastRareDegree\":null,\"requestId\":1517980498392,\"appId\":1,\"tpl\":\"\"}");
            if (StringUtils.isNotBlank(listStr)) {
                JSONObject data = JSON.parseObject(listStr);
                if ("00".equals(data.getString("errorNo"))) {
                    JSONArray list = data.getJSONObject("data").getJSONArray("petsOnSale");
                    if(list.size() == 0) {
                        count = 0;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject object = list.getJSONObject(i);
                        int generation = object.getInteger("generation");
                        String petId = object.getString("petId");
                        double amount = object.getDouble("amount");
                        int rareDegree = object.getInteger("rareDegree");
                        String validCode = object.getString("validCode");
                        if(rareDegree <= 2) {
                            logger.info("end with rareDegree <= 2 ............");
                            count = 0;
                            break;
                        }
                        if((amount < 10000 && rareDegree == 3) || (amount < 30000 && rareDegree == 4)) {
                            Map<String, String> map = Maps.newHashMap();
                            map.put("generation", String.valueOf(generation));
                            map.put("petId", petId);
                            map.put("amount", String.valueOf(amount));
                            map.put("rareDegree", String.valueOf(rareDegree));
                            map.put("validCode", validCode);
                            pets.push(map);
                            logger.info("add pet,size:" + pets.size());
                        }
                    }
                }
            } else {
                count --;
            }
            logger.info("page:" + page + " end -----------");
        }
    }


}
