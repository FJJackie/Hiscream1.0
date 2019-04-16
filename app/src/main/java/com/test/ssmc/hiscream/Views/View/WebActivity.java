package com.test.ssmc.hiscream.Views.View;

import android.Manifest;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;
import com.test.ssmc.hiscream.MyService;
import com.test.ssmc.hiscream.R;
import com.test.ssmc.hiscream.Views.URLFactory;
import com.test.ssmc.hiscream.Views.greendao.HistoryRecord;
import com.test.ssmc.hiscream.Views.greendao.ListAdapter;
import com.test.ssmc.hiscream.Views.greendao.MyApplication;
import com.test.ssmc.hiscream.Views.greendao.dao.HistoryRecordDao;
import com.test.ssmc.hiscream.Views.greendao.dao.DaoSession;
import com.test.ssmc.hiscream.Views.utils.BaseActivity;

import java.util.List;

public class WebActivity extends BaseActivity implements UpDateWebViewInterface {
    private WebView webView;

    private ImageButton backButton, frontButton, settingButton, homeButton;
    private LinearLayout buttonBar;
    private FloatingActionButton hiscreenButton;//进入隐私模式的button控件
    private CalibrationProvider calibrationProvider;
    private boolean isHide = false;
    private CalibrationDialog calibrationDialog;
    private MyService myService;
    private Intent serviceIntent;

    private ListView mListView;
    private ListAdapter mAdapter;
    private ListAdapter mQueryAdapter;
    private HistoryRecordDao mHistoryRecordDao;
    private List<HistoryRecord> mHistoryRecordList;
    private List<HistoryRecord> mQueryList;
    private int mSalary;

    private Toolbar toolbar;
    private SearchView searchView;
    private SearchManager searchManager;
    private RelativeLayout relativeLayout;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.MyBinder binder = (MyService.MyBinder)iBinder;
            myService = binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Toast.makeText(myService, "失败", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        setToolbar();

        //get the company DAO
        DaoSession daoSession = ((MyApplication)getApplication()).getDaoSession();
        mHistoryRecordDao = daoSession.getCompanyDao();
        //query all employee to show in the list
        mHistoryRecordList = mHistoryRecordDao.queryBuilder().list();
        setupView();
        handleIntent(getIntent());

        serviceIntent=new Intent(WebActivity.this,MyService.class);
        requestCameraPermission();
        binding();
        //获得屏幕大小信息
        //绑定provider
        calibrationProvider = new CalibrationProvider(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        calibrationProvider.setDisplayMetrics(displayMetrics);
        webView.loadUrl("https://www.cn.bing.com");
        //绑定服务
        bindService(serviceIntent,conn,Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {

        if(myService==null||!myService.isWork){
            bindService(serviceIntent,conn,Context.BIND_AUTO_CREATE);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(myService!=null&&myService.isWork){
            myService.releaseView();
            stopService(serviceIntent);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(myService!=null&&myService.isWork){
            myService.releaseView();
            stopService(serviceIntent);
        }
        super.onDestroy();
    }

    /**
     * FindViewBy ID
     */
    private void binding() {
        relativeLayout = (RelativeLayout)findViewById(R.id.record_layout);

        buttonBar = findViewById(R.id.buttonBar);

        //绑定webView
        webView = findViewById(R.id.myWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //urlEditText.setText(view.getTitle());
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);


        //绑定返回键
        backButton = findViewById(R.id.back);
        backButton.setOnClickListener(v -> webView.goBack());

        //绑定前进键
        frontButton = findViewById(R.id.front);
        frontButton.setOnClickListener(v -> webView.goForward());

        //绑定主页键
        homeButton = findViewById(R.id.home);
        homeButton.setOnClickListener(v -> webView.loadUrl("https://www.v2ex.com/"));

        //绑定设置键
        settingButton = findViewById(R.id.setting);
        settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(WebActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        //绑定悬浮按钮
        hiscreenButton = findViewById(R.id.hiscreenButton);
        //按键响应 修改主题颜色
        hiscreenButton.setOnClickListener(v -> {
            if (isHide) {
                isHide = false;
                myService.releaseView();
                stopService(serviceIntent);
                buttonBar.setBackgroundColor(0x00ffffff);
                backButton.setBackgroundColor(0x00ffffff);
                frontButton.setBackgroundColor(0x00ffffff);
                settingButton.setBackgroundColor(0x00ffffff);
                homeButton.setBackgroundColor(0x00ffffff);
                toolbar.setBackgroundColor(0x00ffffff);
                hiscreenButton.setImageResource(R.drawable.eye);
            } else {
                startService(serviceIntent);
                isHide = true;
                buttonBar.setBackgroundColor(0x91131313);
                backButton.setBackgroundColor(0x91131313);
                homeButton.setBackgroundColor(0x91131313);
                frontButton.setBackgroundColor(0x91131313);
                settingButton.setBackgroundColor(0x91131313);
                toolbar.setBackgroundColor(0x91131313);
                hiscreenButton.setImageResource(R.drawable.hide);
            }
        });
    }

    /**
     * 申请摄像头权限
     */
    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(WebActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            return;

        PermissionsUtil.requestPermission(this, new PermissionListener() {
            @Override
            public void permissionGranted(@NonNull String[] permission) {
                Toast.makeText(WebActivity.this, "Success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void permissionDenied(@NonNull String[] permission) {
                Toast.makeText(WebActivity.this, "用户拒绝了访问摄像头", Toast.LENGTH_LONG).show();
            }
        }, new String[]{Manifest.permission.CAMERA});
    }


    //重写返回键，如果网页后退栈中还有元素，返回键就返回之前的网页，否则就退出程序
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    //js代码加载 动态改变页面显示
    @Override
    public void LoadJsView(String js) {
        if(calibrationDialog.isShowing()){
            calibrationDialog.dismiss();
        }
        webView.loadUrl(js);
    }

    @Override
    public void ShowSnackbar(String msg) {
        //绑定在webview上
        Snackbar.make(webView, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void updateBrightness(float brightness) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void showWaiting() {
        calibrationDialog=new CalibrationDialog(this);
        calibrationDialog.setCanceledOnTouchOutside(false);
        calibrationDialog.show();
    }


    //设置toolbar
    private void setToolbar(){
        //使用ToolBar控件替代ActionBar控件
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.toolbar, menu);

        //set searchView configuration to search something
        searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        //包括 搜索和内容发生变化 监听
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            //点击提交搜索时触发
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                if (query.equals("")) {
                    mListView.setAdapter(mAdapter);
                    refreshList();
                    return true;
                } else {
                    String url = query.trim();
                    Log.d(TAG, "onQueryTextSubmit: " + url);
                    relativeLayout.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    buttonBar.setVisibility(View.VISIBLE);

                    HistoryRecord recordItem = new HistoryRecord(null, url, mSalary);
                    mHistoryRecordDao.insert(recordItem);
                    refreshList();

                    url = URLFactory.handleURL(url);
                    webView.loadUrl(url);
                    webView.setVisibility(View.VISIBLE);
                    return false;
                }
            }

            //当内容发生改变时触发
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals("")) {
                    mListView.setAdapter(mAdapter);
                    refreshList();
                    return true;
                } else {
                    return false;
                }
            }
        });

        //监控当searchView打开、关闭时调用事件
        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.search), new MenuItemCompat.OnActionExpandListener() {
            //打开searchView时触发
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //searchView打开事件
                relativeLayout.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                buttonBar.setVisibility(View.GONE);
                return true;
            }
            //关闭searchView时触发
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                relativeLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                buttonBar.setVisibility(View.VISIBLE);

                mListView.setAdapter(mAdapter);
                refreshList();
                return true;
            }
        });

        return true;
    }

    //show the queryResult in the listView
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mQueryList = mHistoryRecordDao.queryBuilder().where(HistoryRecordDao.Properties.Name.eq(query)).list();

            //instantiate adapter which used to hold queryResult show in queryList
            mQueryAdapter = new ListAdapter(this, R.layout.list_item, mQueryList);
            mListView.setAdapter(mQueryAdapter);
        }
    }

    private void setupView() {

        //Set listView
        mListView = (ListView) findViewById(R.id.list_view);

        //instantiate adapter which used to hold data show in normal list
        mAdapter = new ListAdapter(this, R.layout.list_item, mHistoryRecordList);

        mListView.setAdapter(mAdapter);
        //点击搜索记录，填充输入框
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //获取历史记录并填充
                EditText editText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
                String historyItem = mHistoryRecordList.get(i).getName();
                editText.setText(historyItem);//设置EditText控件的内容
                editText.setSelection(historyItem.length());//将光标移至文字末尾

                //弹出键盘
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.requestFocus();
                InputMethodManager inputManager =
                        (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(editText, 0);
            }
        });

        //删除搜索记录
        mAdapter.setOnItemDeleteClickListener(new ListAdapter.onItemDeleteListener() {
            @Override
            public void onDeleteClick(int i) {
                showDeleteAlertDialog(mHistoryRecordList.get(i).getId());
            }
        });
    }

    /**
     *
     * @param id id of employee in database table
     */
    private void showDeleteAlertDialog(final long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_alert);
        //确认删除
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mHistoryRecordDao.deleteByKey(id);
                refreshList();
            }
        });
        //取消
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });
        builder.create().show();
    }

    //refresh the listView to show latest data
    private void refreshList() {
        mHistoryRecordList.clear();
        mHistoryRecordList.addAll(mHistoryRecordDao.queryBuilder().list());
        mAdapter.notifyDataSetChanged();
    }
}
