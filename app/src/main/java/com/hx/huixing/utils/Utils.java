package com.hx.huixing.utils;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.lidroid.xutils.BitmapUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分享首选项的工具类
 */
public class Utils {

    /**
     * 上下文
     */
    public static Context context;


    // 单例模式
    private static BitmapUtils bitmapUtils;

    /**
     * 获取bitmapUtils实例的方法
     *
     * @return
     */
    public static BitmapUtils getBitmapUtils() {
        if (bitmapUtils == null) {
            bitmapUtils = new BitmapUtils(context);

            bitmapUtils.configDefaultBitmapConfig(Bitmap.Config.RGB_565);// 编码格式

            bitmapUtils.configMemoryCacheEnabled(true);// 内存缓存
            bitmapUtils.configDiskCacheEnabled(true);// 磁盘缓存

        }
        return bitmapUtils;
    }


    public static void setContext(Context context) {
        Utils.context = context;
    }

    /**
     * 写入首选项文件(.xml)
     *
     * @param filename
     * @param key
     * @param value
     */
    public static void writeData(String filename, String key, String value) {
        //实例化SharedPreferences对象,参数1是存储文件的名称，参数2是文件的打开方式，当文件不存在时，直接创建，如果存在，则直接使用
        SharedPreferences mySharePreferences =
                context.getSharedPreferences(filename, Context.MODE_PRIVATE);

        //实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = mySharePreferences.edit();

        //用putString的方法保存数据
        editor.putString(key, value);

        //提交数据
        editor.commit();
    }


    /**
     * 从首选项中读取值
     *
     * @param filename
     * @param key
     */
    public static String readData(String filename, String key) {
        //实例化SharedPreferences对象
        SharedPreferences mySharePerferences =
                context.getSharedPreferences(filename, Context.MODE_PRIVATE);

        //用getString获取值
        String name = mySharePerferences.getString(key, "");
        return name;
    }


    /**
     * 获取全部的键值对
     *
     * @param filename
     * @return
     */
    public static Map<String, ?> getAll(String filename) {
        SharedPreferences sp =
                context.getSharedPreferences(filename, Context.MODE_PRIVATE);

        return sp.getAll();
    }

    /**
     * 查询某个key是否已经存在
     *
     * @param filename
     * @param key
     * @return
     */
    public static boolean contains(String filename, String key) {
        SharedPreferences sp =
                context.getSharedPreferences(filename, Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    /**
     * 移除某个值
     *
     * @param filename
     * @param key
     */
    public static void remove(String filename, String key) {
        SharedPreferences sp = context.getSharedPreferences(filename,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.commit();
    }

    /**
     * 移除首选项全部值
     *
     * @param filename
     */
    public static void removeAll(String filename) {
        SharedPreferences sp = context.getSharedPreferences(filename,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
        editor.commit();
    }

    /**
     * 获取网址中的图片名称
     *
     * @param url
     * @return
     */
    public static String cutImagePath(String url) {
        String res = "";
        int start = url.lastIndexOf("/") + 1;
        res = url.substring(start);
        return res;
    }

    /**
     * 获取SDcard根路径
     *
     * @return
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }



    /**
     * 集合到数组
     *
     * @return
     */
    public static String[] listToArray(List<String> list) {
        String[] arr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }


    /**
     * 获取文件中的所有图片的绝对路径
     *
     * @param dir
     * @return
     */
    public static List<String> getDownloadImages(String dir) {
        List<String> res = new ArrayList<String>();
        File fdir = new File(dir);
        File[] files = fdir.listFiles();
        if (files != null) {
            // 遍历
            for (int i = 0; i < files.length; i++) {
                res.add(files[i].getAbsolutePath());// 绝对路径
            }
        }
        return res;
    }


    /**
     * 过滤出有效链接
     *
     * @param links
     * @return
     */
    public static List<String> getUseableLinks(Elements links, String currURL) {
        Map<String, String> mapLinks = new HashMap<String, String>();
        List<String> lstLinks = new ArrayList<String>();

        String home = currURL;// 本站的域名

        //遍历所有links,过滤,保存有效链接
        for (Element link : links) {
            String href = link.attr("href");// abs:href, "http://"
            //Log.i("spl","过滤前,链接:"+href);
            // 设置过滤条件
            if (href.equals("")) {
                continue;// 跳过
            }
            if (href.equals(home)) {
                continue;// 跳过
            }
            if (href.startsWith("javascript")) {
                continue;// 跳过
            }

            if (href.startsWith("/")) {
                href = home + href;
            }
            if (!mapLinks.containsKey(href)) {
                mapLinks.put(href, href);// 将有效链接保存至哈希表中
                lstLinks.add(href);
            }

            Log.i("wxs", "有效链接:" + href);
        }

        return lstLinks;
    }


    /**
     * 判断当前应用程序处于前台还是后台
     * return false 在前台
     * return true 在后台
     */
    public static boolean isAppToBackground(final Context context) {
        if (context != null){
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                if (!topActivity.getPackageName().equals(context.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }




    /**
     * 分割字符串
     *
     * @param all   要分割的字符串
     * @param start 开始
     * @param end   结束
     * 如"("  ")"
     * @return
     */
    public static String splitStr(String all, String start, String end) {
        int s = all.indexOf(start);
        int l = all.indexOf(end);
        end = all.substring(s + 1, l);
        return end;
    }

    /**
     * 更新历史记录集合
     * @param list 要全部添加的集合
     * @param filePath 历史记录文件路径
     */
    public static List<String> updateHistory(List<String> list, String filePath){
        Map<String, ?> spUrls = Utils.getAll(filePath);//从首选项中拿值

        Log.i("wxs", "--------");
        String[] arr = new String[spUrls.size()];// 定义一个数组

        int index = 0;
        for (String key : spUrls.keySet()) {
            //String url = surl.get(key).toString();
            arr[index] = key;
            index++;
        }

        final String[] historyUrls = arr;//历史记录列表

        //数组转化成集合
        list = Arrays.asList(historyUrls);
        return list;
    }


}
