package com.test.ssmc.hiscream.Views.greendao;

import android.app.Application;

import com.test.ssmc.hiscream.Views.greendao.dao.DaoMaster;
import com.test.ssmc.hiscream.Views.greendao.dao.DaoSession;

import org.greenrobot.greendao.database.Database;


/**
 * Created by Crazypudding on 2017/1/4.
 */

public class MyApplication extends Application {

    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "company-db");
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }
}
