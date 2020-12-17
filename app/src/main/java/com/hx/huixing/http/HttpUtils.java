package com.hx.huixing.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**用于json解析的工具类 获取服务端返回数据 并且转为字符串
 * Created by KBLW on 2016/5/28.
 */
public class HttpUtils {

    public static String getJsonStringContent(String url_path) {
        return changeInputStream(getInputStream(url_path));
    }

    public static InputStream getInputStream(String url_path) {
        try {
            URL url = new URL(url_path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            int code = connection.getResponseCode();
            if (code == 200) {
                return connection.getInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTotal(String url_path) {
        try {
            URL url = new URL(url_path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            int code = connection.getResponseCode();
            if (code == 200) {
                return connection.getContentLength()+"";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String changeInputStream(InputStream inputStream) {
        String jsonString = "";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int len = 0;
        byte[] data = new byte[1024];
        try {
            while ((len = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, len);
            }
            jsonString = new String(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    public static String getFileName(String url) {
        String suffixes = "avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|txt|html|htm|java|doc";
        String file = url.substring(url.lastIndexOf('/') + 1);//截取url最后的数据
        Pattern pat = Pattern.compile("([\\w|-]+)[\\.](" + suffixes + ")");//正则判断
        Matcher mc = pat.matcher(file);
        while(mc.find())
        {
            String substring = mc.group(0);//截取文件名后缀名
            return substring;
        }
        return "";
    }

    public static boolean IsVideoUrl(String file) {
        String suffixes = "avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|txt|java|doc";
        Pattern pat = Pattern.compile("([\\w|-]+)[\\.](" + suffixes + ")");//正则判断
        Matcher mc = pat.matcher(file);
        while(mc.find()){
            return true;
        }
        return false;
    }
}