package com.hx.huixing.utils;

/**
 * @Author: lilx
 * @Date: 2020/3/24 16:35
 * @Description:
 */
/**
 * 文件下载类
 * @author luweicheng
 *
 */

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import com.hx.huixing.fragment.LocalMusicFragment;

public class DownFile {
    private URL fileUrl;// 文件下载路径
    private int threadCount;// 文件下载的线程数
    private int startPos;// 每个线程下载文件的开始位置
    private int size;// 每个线程下载文件的长度
    private int fileLength;// 文件总程度
    private String pathName;// 下载的文件路径（包含文件名）
    private final static String referrer = "http://www.baidu.com";
    private Downthread[] tDownthreads;// 线程数组

    public DownFile(URL url, int threadCount, String pathName) throws IOException {
        fileUrl = url;
        this.threadCount = threadCount;
        this.pathName = pathName;
        init();

    }

    public static Document doGetDoc(String url) {
        try {
            return doGet(url).get();
        }catch (SocketTimeoutException e){
            try {
                Thread.currentThread().sleep(5000);
                return doGetDoc(url);
            }catch (Exception ex){

            }
        }catch (SSLHandshakeException e){

        }catch (Exception e){
                e.printStackTrace();
        }
        return null;
    }

    public static Connection doGet(String url) {
       return doGet(url,10 * 1000);
    }

    public static Connection doGet(String url,int var1) {
        try {
            return Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.3; WOW64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
                    + "Safari/537.36 OPR/30.0.1835.59").timeout(var1).referrer(referrer).followRedirects(false);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private void init() throws IOException {
        tDownthreads = new Downthread[threadCount];
        HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
        conn.setConnectTimeout(30000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("connection", "keep-alive");
        conn.connect();
        //fileLength =doGet(fileUrl.toString()).ignoreContentType(true).execute().bodyAsBytes().length;
        fileLength = conn.getContentLength();

        System.out.println("文件长度" + fileLength);
        size = fileLength / threadCount;
        System.out.println("每个下载量==" + size);

    }

    public URL getFileUrl() {
        return fileUrl;
    }

    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * 开始下载
     */
    public void startDown() {
        for (int i = 0; i < threadCount; i++) {
            try {
                RandomAccessFile raFile = new RandomAccessFile(pathName, "rw");
                tDownthreads[i] = new Downthread(i * size, raFile, i);
                tDownthreads[i].start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * 下载线程类
     *
     * @author luweicheng
     *
     */
    class Downthread extends Thread {
        private int startPos;// 开始的位置
        private InputStream is;
        private RandomAccessFile raFile;
        private int length;// 下载的文件长度
        private int flag;// 线程标志

        public Downthread(int startPos, RandomAccessFile raFile, int i) {
            this.startPos = startPos;
            this.raFile = raFile;
            flag = i;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("connection", "keep-alive");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
                        + "Safari/537.36 OPR/30.0.1835.59");
                connection.setConnectTimeout(5 * 1000);
                is = connection.getInputStream();
                is.skip(startPos);
                raFile.seek(startPos);
                byte[] buf = new byte[8 * 1024];
                int hasread = 0;// 读出的字节数
                // 将位置在 startPos - startPos 位置的数据读出写入
                while (length < size && (hasread = is.read(buf)) != -1) {
                    raFile.write(buf, 0, hasread);
                    length += hasread;
                    System.out.println("*****线程" + flag + "下载了*********" + length);
                }
                System.out.println("*******线程" + flag + "下载完成*********");

            } catch (IOException e) {

            } finally {

                try {
                    is.close();
                    raFile.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }

        }
    }

    public static void customBufferStreamCopy(File source, File target) {
        InputStream fis = null;
        OutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);
            byte[] buf = new byte[4096];
            int i;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
                LocalMusicFragment.refreshCache(target);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
