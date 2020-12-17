package com.hx.huixing.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * 替换文件（如果该文件含有子目录，则包括子目录所有文件）中某个字符串并写入新内容（Java代码实现）.
 *
 *原理：逐行读取源文件的内容，一边读取一边同时写一个*.tmp的文件。
 *当读取的行中发现有需要被替换和改写的目标内容‘行’时候，用新的内容‘行’替换之。
 *最终，删掉源文件，把*.tmp的文件重命名为源文件名字。
 *
 *注意！代码功能是逐行读取一个字符串，然后检测该字符串‘行’中是否含有替换的内容，有则用新的字符串‘行’替换源文件中该处整个字符串‘行’。没有则继续读。
 *注意！替换是基于‘行’，逐行逐行的替换！
 *
 * */
public class Modify {

    private String path;
    private static String target;
    private static String newContent;

    public Modify(String path, String target, String newContent) {
        // 操作目录。从该目录开始。该文件目录下及其所有子目录的文件都将被替换。
        this.path = path;
        // target:需要被替换、改写的内容。
        this.target = target;
        // newContent:需要新写入的内容。
        this.newContent = newContent;

        operation();
    }

    public void operation(String target, String newContent) {
        // target:需要被替换、改写的内容。
        this.target = target;
        // newContent:需要新写入的内容。
        this.newContent = newContent;
        operation();
    }

    public void operation() {
        File file = new File(path);
        opeationDirectory(file);
    }

    public void opeationDirectory(File dir) {
        if (dir.isFile()) {
            operationFile(dir);
            return;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory())
                // 如果是目录，则递归。
                opeationDirectory(f);
            if (f.isFile())
                operationFile(f);
        }
    }

    public void operationFile(File file) {

        try {
            InputStream is = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));

            String filename = file.getName();
            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            File tmpfile = new File(file.getParentFile().getAbsolutePath()
                    + "\\" + filename + ".tmp");

            BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));

            boolean flag = false;
            String str = null;
            while (true) {
                str = reader.readLine();

                if (str == null)
                    break;

                if (str.contains(target)) {
                    writer.write(newContent + "\n");

                    flag = true;
                } else
                    writer.write(str + "\n");
            }

            is.close();

            writer.flush();
            writer.close();

            if (flag) {
                file.delete();
                tmpfile.renameTo(new File(file.getAbsolutePath()));
            } else
                tmpfile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //代码测试：假设有一个test文件夹，test文件夹下含有若干文件或者若干子目录，子目录下可能也含有若干文件或者若干子目录（意味着可以递归操作）。
        //把test目录下以及所有子目录下（如果有）中文件含有"hi"的字符串行替换成新的"hello,world!"字符串行。
        new Modify("C:/Users/lilx/Desktop/aaa.html", "&&&", "hello,world!");
    }

    public static void modify(String target, String  newContent, Context context,String FILE_NAME){
        try {
            File file = new File(FileUtils.getMusicDir() + FILE_NAME);
            InputStream is;
            if(file.exists()){
                is = new FileInputStream(file);
            }else {
                is = context.getAssets().open(FILE_NAME);
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));


            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            String filename = file.getName();
            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            File tmpfile = new File(file.getParentFile().getAbsolutePath()
                    + "\\" + filename + ".tmp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));

            boolean flag = false;
            String str = null;
            while (true) {
                str = reader.readLine();

                if (str == null)
                    break;

                if (str.contains(target)) {
                    writer.write(newContent + "\n");

                    flag = true;
                } else
                    writer.write(str + "\n");
            }

            is.close();

            writer.flush();
            writer.close();
            if (flag) {
                file.delete();
                tmpfile.renameTo(new File(file.getAbsolutePath()));
            } else
                tmpfile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void createNewContent(String  newContent,File file){
        try {
            if(!file.exists()){
                file.getParentFile().mkdirs();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(newContent );
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}