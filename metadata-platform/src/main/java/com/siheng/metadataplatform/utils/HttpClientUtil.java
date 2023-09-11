package com.siheng.metadataplatform.utils;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author Shinya
 * @date 2023/7/15 4:20 下午
 * @Desc
 */
public class HttpClientUtil {

    // 编码格式。发送编码格式统一用UTF-8
    private static final String ENCODING = "UTF-8";

    // 设置连接超时时间，单位毫秒。
    private static final int CONNECT_TIMEOUT = 5000;

    // 请求获取数据的超时时间(即响应时间)，单位毫秒。
    private static final int SOCKET_TIMEOUT = 5000;

    /**
     * Description: get请求
     *
     * @param url 路径
     * @return 响应结果
     */
    public static String get(String url) throws Exception {
        return get(url, null);
    }

    /**
     * Description: post请求
     *
     * @param url    路径
     * @param params 参数
     * @return 响应结果
     */
    public static String post(String url, Map<String, Object> params) throws Exception {
        return post(url, params, null);
    }

    /**
     * Description: post请求（用于请求json格式的参数）
     *
     * @param url    路径
     * @param params json参数
     * @return 响应结果
     */
    public static String jsonPost(String url, String params) throws Exception {
        return jsonPost(url, params, null);
    }

    /**
     * Description: get请求
     *
     * @param url     路径
     * @param headers 请求头信息
     * @return 响应结果
     */
    public static String get(String url, Map<String, String> headers) throws Exception {
        CloseableHttpClient httpClient = null;
        HttpGet httpGet;
        CloseableHttpResponse httpResponse = null;
        String result;
        try {
            httpClient = HttpClientBuilder.create().build();

            httpGet = new HttpGet(url);
            //设置请求头
            setHeader(headers, httpGet);
            RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
            httpGet.setConfig(config);
            result = getHttpClientResult(httpResponse, httpClient, httpGet);
        } finally {
            release(httpResponse, httpClient);
        }
        return result;
    }


    /**
     * Description: post请求
     *
     * @param url     路径
     * @param params  参数
     * @param headers 请求头信息
     * @return 响应结果
     */
    public static String post(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        CloseableHttpClient httpClient = null;
        HttpPost httpPost;
        CloseableHttpResponse httpResponse = null;
        String result;
        try {
            httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(url);
            //设置请求头
            setHeader(headers, httpPost);
            //设置参数
            setParam(params, httpPost);

            RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
            httpPost.setConfig(config);

            result = getHttpClientResult(httpResponse, httpClient, httpPost);
        } finally {
            release(httpResponse, httpClient);
        }
        return result;
    }


    /**
     * Description: post请求（用于请求json格式的参数）
     *
     * @param url     路径
     * @param params  json参数
     * @param headers 请求头信息
     * @return 响应结果
     */
    public static String jsonPost(String url, String params, Map<String, String> headers) throws Exception {
        CloseableHttpClient httpClient = null;
        HttpPost httpPost;
        CloseableHttpResponse httpResponse = null;
        String result;
        try {
            httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(url);
            httpPost.addHeader("Content-type", "application/json; charset=utf-8");
            httpPost.setHeader("Accept", "application/json");
            //设置请求头
            setHeader(headers, httpPost);
            //设置参数
            httpPost.setEntity(new StringEntity(params, ENCODING));
            RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
            httpPost.setConfig(config);

            result = getHttpClientResult(httpResponse, httpClient, httpPost);
        } finally {
            release(httpResponse, httpClient);
        }
        return result;
    }

    /**
     * Description: 封装请求头
     *
     * @param headers    请求头
     * @param httpMethod httpMethod
     */
    public static void setHeader(Map<String, String> headers, HttpRequestBase httpMethod) {
        // 封装请求头
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                httpMethod.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Description: 封装请求参数
     *
     * @param params     请求参数
     * @param httpMethod httpMethod
     */
    public static void setParam(Map<String, Object> params, HttpEntityEnclosingRequestBase httpMethod) throws UnsupportedEncodingException {

        List<NameValuePair> list = new ArrayList<>();
        if (null != params) {
            for (Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                list.add(new BasicNameValuePair(key, value));
            }
        }
        if (list.size() > 0) {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, ENCODING);
            httpMethod.setEntity(entity);
        }
    }

    /**
     * Description: 获取响应结果
     *
     * @param httpResponse httpResponse
     * @param httpClient   httpClient
     * @param httpMethod   httpMethod
     * @return 响应结果
     * @throws Exception Exception
     */
    public static String getHttpClientResult(CloseableHttpResponse httpResponse, CloseableHttpClient httpClient, HttpRequestBase httpMethod) throws Exception {
        String result = null;
        // 执行请求
        httpResponse = httpClient.execute(httpMethod);

        // 获取返回结果
        if (httpResponse != null) {
            HttpEntity resEntity = httpResponse.getEntity();
            if (resEntity != null) {
                result = EntityUtils.toString(resEntity, ENCODING);
            }
        }
        return result;
    }


    /**
     * Description: 释放资源
     *
     * @param httpResponse HttpResponse
     * @param httpClient   HttpClient
     * @throws IOException IOException
     */
    public static void release(CloseableHttpResponse httpResponse, CloseableHttpClient httpClient) throws IOException {
        if (httpResponse != null) {
            httpResponse.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }
}

