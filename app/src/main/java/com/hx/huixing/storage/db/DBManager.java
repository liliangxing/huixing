package com.hx.huixing.storage.db;

import android.content.Context;
import android.os.Environment;

import org.greenrobot.greendao.database.Database;

import java.io.File;

import com.hx.huixing.storage.db.greendao.DaoMaster;
import com.hx.huixing.storage.db.greendao.DaoSession;
import com.hx.huixing.storage.db.greendao.MusicDao;

/**
 * Created by wcy on 2018/1/27.
 */
public class DBManager {
    private static final String DB_NAME = "database";
    private MusicDao musicDao;

    public static DBManager get() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static DBManager instance = new DBManager();
    }
    /**
     * 获取db文件在sd卡的路径
     * @return
     */
    private static String getDirPath(){
        //TODO 这里返回存放db的文件夹的绝对路径
        return new File(Environment.getExternalStorageDirectory()+"").getAbsolutePath();
    }

    public void init(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(new CustomPathDatabaseContext(context, getDirPath()), DB_NAME);
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        musicDao = daoSession.getMusicDao();
    }

    private DBManager() {
    }

    public MusicDao getMusicDao() {
        return musicDao;
    }
}
