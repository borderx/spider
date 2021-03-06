package com.borderx;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.borderx.bean.Pet;
import com.borderx.bean.Statistics;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by borderx on 2018/2/4.
 */
public class First {

    private static final Logger logger = LoggerFactory.getLogger(First.class);

    static final Object emptyLock = new Object();

    static final ReentrantLock lock = new ReentrantLock();

    static final Condition empty = lock.newCondition();

    static final Condition notEmpty = lock.newCondition();

    static Properties config = null;

    static org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient(new HttpClientParams(), new SimpleHttpConnectionManager(true));

    static ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
            return 20 * 1000; // tomcat默认keepAliveTimeout为20s
        }
    };

    static PoolingHttpClientConnectionManager connManager = null;

    static HttpClientBuilder httpClientBuilder = null;

    static {

        connManager = new PoolingHttpClientConnectionManager(20, TimeUnit.SECONDS);
        connManager.setMaxTotal(200);
        connManager.setDefaultMaxPerRoute(200);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000)
                .setConnectionRequestTimeout(10 * 1000)
                .build();
        httpClientBuilder = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
//                .setRetryHandler(new DefaultHttpRequestRetryHandler())
                .setRetryHandler((exception, executionCount, context) -> {
//                    if (executionCount > 5) {
//                        return false;
//                    }
//                    if (exception instanceof NoHttpResponseException     //NoHttpResponseException 重试
//                            || exception instanceof ConnectTimeoutException //连接超时重试
//                            ) {
//                        return true;
//                    }
                    return false;
                })
                .setKeepAliveStrategy(connectionKeepAliveStrategy);
    }

    static {
        //client.getHttpConnectionManager().getParams().setConnectionTimeout(1000);
        //client.getHttpConnectionManager().getParams().setSoTimeout(1000);
        logger.info(System.getProperty("user.dir"));
        String filePath = System.getProperty("user.dir") + File.separator +"config.properties";
        InputStream in = null;
        config = new Properties();
        try {
            in = new BufferedInputStream(new FileInputStream(filePath));
            config.load(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final List<String> gone = Lists.newArrayList();

    public static void main(String[] args) throws Exception {
        whoAmI();
        new Thread(() -> find()).start();
        new Thread(() -> sale()).start();
        new Thread(() -> buy()).start();
        new Thread(() -> statistics()).start();
        new Scanner(System.in).nextLine();
    }

    private static final Statistics statistics = new Statistics();

    public static void statistics() {
        while(true) {
            logger.info("statistics:{}", JSON.toJSONString(statistics));
            try {
                Thread.sleep(1000 * 60 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sale() {
        int count = 0;
        while(true) {
            String url = "https://pet-chain.baidu.com/data/user/get";
            String param = "{\"requestId\":1517764720015,\"appId\":1,\"tpl\":\"\"}";
            String res = First.request(url, param);
            if(StringUtils.isNotBlank(res)) {
                JSONObject user = JSON.parseObject(res);
                if ("00".equals(user.getString("errorNo"))) {
                    String userName = user.getJSONObject("data").getString("userName");
                    String amount = user.getJSONObject("data").getString("amount");
                    logger.info("-------------userName:{},amount:{}------------------", userName, amount);
                    if(Double.valueOf(amount) <= Double.valueOf(config.getProperty("amountLimit"))) {
                        logger.info("-------------userName:{},amount:{},start sale------------------", userName, amount);
                        realSale();
                    }
                }
            }
            logger.info("sale end:{}", count++);
            try {
                Thread.sleep(1000 * 60 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void realSale() {
        String url = "https://pet-chain.baidu.com/data/user/pet/list";
        String param = "{\"pageNo\":1,\"pageSize\":30,\"pageTotal\":-1,\"requestId\":1517765996155,\"appId\":1,\"tpl\":\"\"}";
        String result = request(url, param);
        if(StringUtils.isNotBlank(result)) {
            JSONObject data = JSON.parseObject(result);
            if("00".equals(data.getString("errorNo"))) {
                int configSaleCount = Integer.valueOf(config.getProperty("saleCount", "1"));
                int alreadyCount = 0;
                JSONArray list = data.getJSONObject("data").getJSONArray("dataList");
                for(int i = 0; i < list.size(); i ++) {
                    JSONObject o = list.getJSONObject(i);
                    int shelfStatus = o.getInteger("shelfStatus");
                    if(shelfStatus == 1) {
                        alreadyCount ++;
                    }
                }
                if(alreadyCount < configSaleCount) {
                    for(int i = 0; i < list.size(); i ++) {
                        JSONObject o = list.getJSONObject(i);
                        int shelfStatus = o.getInteger("shelfStatus");
                        int rareDegree = o.getInteger("rareDegree");
                        String petId = o.getString("petId");
                        if(shelfStatus == 0 && rareDegree == 0) {
                            alreadyCount ++;
                            boolean sale = sale0(petId);
                            if(sale) {
                                alreadyCount ++;
                                if(alreadyCount >= configSaleCount) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean sale0(String petId) {
        String amount = config.getProperty("amount", "1799");
        String url = "https://pet-chain.baidu.com/data/market/salePet";
        Map<String, Object> params = Maps.newHashMap();
        params.put("petId", petId);
        params.put("amount", amount);
        params.put("requestId", new Date().getTime());
        params.put("appId", "1");
        params.put("tpl", "");
        String body = JSON.toJSONString(params);
        String result = request(url, body);
        if(StringUtils.isNotBlank(result)) {
            JSONObject data = JSON.parseObject(result);
            if("00".equals(data.getString("errorNo"))) {
                logger.info("--------sale petId:{} success-----------", petId);
                return true;
            }
        }
        logger.info("--------sale petId:{} fail-----------", petId);
        return false;
    }

    private static void notEmptyBlock() {
        lock.lock();
        try {
            while(!pets.isEmpty()) {
                notEmpty.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static void emptyBlock() {
        lock.lock();
        try {
            while(pets.isEmpty()) {
                empty.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static void emptyWakeUp() {
        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private static void notEmptyWakeUp() {
        lock.lock();
        try {
            empty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static void find() {
        int count = 0;
        int page = 0;
        while(true) {
            notEmptyBlock();
            page = (page % 5) + 1;
            boolean find = false;
            String listStr = request("https://pet-chain.baidu.com/data/market/queryPetsOnSale", "{\"pageNo\":" + page + ",\"pageSize\":10,\"querySortType\":\"AMOUNT_ASC\",\"petIds\":[],\"lastAmount\":null,\"lastRareDegree\":null,\"requestId\":" + new Date().getTime() + ",\"appId\":1,\"tpl\":\"\"}");
            if (StringUtils.isNotBlank(listStr)) {
                JSONObject data = JSON.parseObject(listStr);
                if ("00".equals(data.getString("errorNo"))) {
                    List<Pet> list = JSON.parseArray(data.getJSONObject("data").getJSONArray("petsOnSale").toJSONString(), Pet.class);
                    for (int i = 0; i < list.size(); i++) {
                        Pet pet = list.get(i);
                        int generation = pet.getGeneration();
                        String petId = pet.getPetId();
                        double amount = pet.getAmount();
                        int rareDegree = pet.getRareDegree();
                        String validCode = pet.getValidCode();
                        if (gone.contains(petId)) {
                            continue;
                        }
                        if (amount <= Double.valueOf(config.getProperty("rareDegree" + rareDegree)) && generation == 0) {
                            logger.info("find pet:{},amount:{}", petId, amount);
                            find = true;
                            statistics.totalIncrease();
                            pets.push(pet);
                            notEmptyWakeUp();
//                            synchronized (emptyLock) {
//                                emptyLock.notifyAll();
//                            }
                            break;
//                            buy0(petId, rareDegree, validCode);
                        }
                    }
                    page ++;
                }
            }
            if(!find) {
                logger.info("not find start sleep ...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("find end:{}", count++);
        }
    }

    private static LinkedBlockingDeque<Pet> pets = new LinkedBlockingDeque<>();

    public static void buy() {
        while(true) {
            if(pets.isEmpty()) {
                emptyBlock();
//                synchronized (emptyLock) {
//                    try {
//                        emptyLock.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            } else {
                while(!pets.isEmpty()) {
                    logger.info("pets:{},{}", pets.size(), JSON.toJSONString(pets));
                    Pet pet = pets.pop();
                    int flag = buy0(pet.getPetId(), pet.getRareDegree(), pet.getValidCode());
                    if(flag < 0) {
                        pets.push(pet);
                    }
                }
                emptyWakeUp();
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
        String result = request(url, JSON.toJSONString(params));
        if (StringUtils.isNotBlank(result)) {
//            CommonResponse response = JSON.parseObject(result, CommonResponse.class);
            JSONObject data = JSON.parseObject(result);
            if ("00".equals(data.getString("errorNo"))) {
                String id = data.getJSONObject("data").getString("id");
                double amount = data.getJSONObject("data").getDouble("amount");
                int generation = data.getJSONObject("data").getInteger("generation");
                if (amount <= Double.valueOf(config.getProperty("rareDegree" + rareDegree)) && generation == 0) {
                    Map<String, String> validInfo = validInfo();
                    String seed = validInfo.get("seed");
                    String captcha = validInfo.get("captcha");
                    String buyStr = getBuyParam(petId, String.valueOf(amount), validCode, seed, captcha);
                    String res = request("https://pet-chain.baidu.com/data/txn/create", buyStr);
                    if (StringUtils.isNotBlank(res)) {
                        JSONObject b = JSON.parseObject(res);
                        if ("10002".equals(b.getString("errorNo"))) {
                            //他人下单
                            gone.add(petId);
                            logger.info("gone.size:{}", gone.size());
                            statistics.failIncrease();
                            return 10;
                        }
                        if ("10003".equals(b.getString("errorNo"))) {
                            //上一笔交易中
                            logger.info("buying-------------------------");
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return -2;
                        }
                        if ("00".equals(b.getString("errorNo"))) {
                            //成功
                            logger.info("buy success,amount:{},petId:{},id:{}", new Object[]{amount, petId, id});
                            statistics.successIncrease();
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return 1;
                        }
                        if ("100".equals(b.getString("errorNo"))) {
                            logger.info("gone.size:{}", gone.size());
                            statistics.codeErrorIncrease();
                            //验证码错误
                            return -1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    public static Map<String, String> validInfo() {
        int count = 1;
        while(true) {
            logger.info("validInfo time:{}", count);
            Map<String, String> gen = gen();
            if(gen != null) {
                String img = gen.get("img");
                String seed = gen.get("seed");
                String captcha = ValidCodeUtils.base64Valid(img);
                if(StringUtils.isNotBlank(captcha) && captcha.length() == 4) {
                    Map<String, String> map = Maps.newHashMap();
                    map.put("seed", seed);
                    map.put("captcha", captcha);
                    return map;
                }
            }
        }
    }

    public static Map<String, String> gen() {
        String url = "https://pet-chain.baidu.com/data/captcha/gen";
        Map<String, Object> params = Maps.newHashMap();
        params.put("requestId", new Date().getTime());
        params.put("appId", "1");
        params.put("tpl", "");
        String result = request(url, JSON.toJSONString(params));
        if (StringUtils.isNotBlank(result)) {
            JSONObject data = JSON.parseObject(result);
            if ("00".equals(data.getString("errorNo"))) {
                String img = data.getJSONObject("data").getString("img");
                String seed = data.getJSONObject("data").getString("seed");
                Map<String, String> map = Maps.newHashMap();
                map.put("img", img);
                map.put("seed", seed);
                return map;
            }
        }
        logger.info("gen fail........");
        return null;
    }

    public static String getBuyParam(String petId, String amount, String validCode, String seed, String captcha) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("petId", petId);
        params.put("amount", amount);
        params.put("seed", seed);
        params.put("captcha", captcha);
        params.put("validCode", validCode);
        params.put("requestId", new Date().getTime());
        params.put("appId", "1");
        params.put("tpl", "");
        return JSON.toJSONString(params);
    }

    public static String request1(String url, String params) {
        PostMethod postMethod = new PostMethod(url);
        try {
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Cookie", config.getProperty("cookie")));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Content-Type", "application/json"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Pragma", "no-cache"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Accept", "application/json"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Mobile Safari/537.36"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Accept-Encoding", "gzip, deflate, br"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Origin", "https://pet-chain.baidu.com"));
            postMethod.setRequestHeader(new org.apache.commons.httpclient.Header("Host", "pet-chain.baidu.com"));

            postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");

            postMethod.setRequestBody(params);

            int httpStatus = client.executeMethod(postMethod);
            if (httpStatus == 200) {
                String response = postMethod.getResponseBodyAsString();
                logger.info(response);
                return response;
            }
            logger.info("httpStatus:{}", httpStatus);
            return null;
        } catch (org.apache.http.NoHttpResponseException e1) {
//            e1.printStackTrace();
            try {
                long noHttpResponseSleep = Long.valueOf(config.getProperty("noHttpResponseSleep", "5000"));
                logger.info("NoHttpResponseException sleep " + noHttpResponseSleep + ".......");
                Thread.sleep(noHttpResponseSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        } finally {
            if(postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return null;
    }

    public static String request(String url, String params) {
        HttpClient client = null;
        HttpResponse response = null;
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("Cookie",config.getProperty("cookie")));
            headers.add(new BasicHeader("Content-Type", "application/json"));
            headers.add(new BasicHeader("Pragma", "no-cache"));
            headers.add(new BasicHeader("Accept", "application/json"));
            headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Mobile Safari/537.36"));
            headers.add(new BasicHeader("Accept-Encoding", "gzip, deflate, br"));
            headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"));
            headers.add(new BasicHeader("Origin", "https://pet-chain.baidu.com"));
            headers.add(new BasicHeader("Host", "pet-chain.baidu.com"));

            StringEntity reqEntity = new StringEntity(params, "utf-8");//new UrlEncodedFormEntity(formParams, "utf-8");
            reqEntity.setContentEncoding("UTF-8");
            reqEntity.setContentType("application/json");

//            RequestConfig requestConfig = RequestConfig.custom()
//                    .setConnectTimeout(1000)//一、连接超时：connectionTimeout-->指的是连接一个url的连接等待时间
//                    .setSocketTimeout(3000)// 二、读取数据超时：SocketTimeout-->指的是连接上一个url，获取response的返回等待时间
//                    .setConnectionRequestTimeout(3000)
//                    .build();

            client = httpClientBuilder.build();
            HttpPost post = new HttpPost(url);
            post.setEntity(reqEntity);
//            post.setConfig(requestConfig);
            post.setHeaders(headers.toArray(new Header[]{}));
            response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity resEntity = response.getEntity();
                String message = EntityUtils.toString(resEntity, "utf-8");
                if(!"https://pet-chain.baidu.com/data/captcha/gen".equals(url)) {
                    logger.info(message);
                }
                EntityUtils.consume(resEntity);
                return message;
            }
            logger.info("statusCode:{}", response.getStatusLine().getStatusCode());
        } catch (NoHttpResponseException e1) {
//            e1.printStackTrace();
            try {
                long noHttpResponseSleep = Long.valueOf(config.getProperty("noHttpResponseSleep", "5000"));
                logger.info("NoHttpResponseException sleep {}.......{}", noHttpResponseSleep, e1.getMessage());
                Thread.sleep(noHttpResponseSleep);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (ConnectTimeoutException e) {
            logger.info("ConnectTimeoutException:{}", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        if("https://pet-chain.baidu.com/data/txn/create".equals(url)) {
            return request(url, params);
        }
        return null;
    }

    public static void whoAmI() {
        boolean who = false;
        String url = "https://pet-chain.baidu.com/data/user/get";
        String param = "{\"requestId\":1517764720015,\"appId\":1,\"tpl\":\"\"}";
        String res = First.request(url, param);
        if(StringUtils.isNotBlank(res)) {
            JSONObject user = JSON.parseObject(res);
            if ("00".equals(user.getString("errorNo"))) {
                who = true;
                String userName = user.getJSONObject("data").getString("userName");
                String amount = user.getJSONObject("data").getString("amount");
                logger.info("-------------userName:{},amount:{}------------------", userName, amount);
            }
        }
        if(!who) {
            whoAmI();
        }
    }
}
