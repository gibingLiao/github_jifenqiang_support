package jfq.wowan.com.myapplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.liulishuo.filedownloader.FileDownloader;

import java.net.URLEncoder;

/**
 * Created by Administrator on 2019/4/11.
 */

public class WowanIndex extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    private WebView mWebView;

    private SwipeRefreshLayout mRefreshLayout;

    private String mStringUrl;

    private String md5Str;

    private String keycode;

    private ImageButton mButton;

    public TextView mTextTitle;


    // 第一次不刷新、
    private boolean mBooleanPageNeedLoad;


    private String cid = "";//渠道id
    private String cuid = "";//用户标识id
    private String deviceid = "";//手机设备号
    private String oaid = "";//10.0oaid

    private String appid = "";//同cid的多个渠道通过appid和appname统计数据
    private String appname = "";//同cid的多个渠道通过appid和appname统计数据


    private String mStringKey = "";//秘钥


    public static final String mStringVer = "1.0";

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private int mIntLoadingRealProgress;//当前web加载真正的进度

    private int progress;
    private Runnable mRunnableProgress = new Runnable() {
        @Override
        public void run() {
            progress = progress + 2;
            if (progress >= 100) {
                progress = 100;
            }
            if (progress < mIntLoadingRealProgress) {
                mHandler.post(this);
            }


            if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
                mProgressBar.setProgress(progress);
            }

            if (progress >= 100) {
                if (mRelativeLoading != null && mRelativeLoading.getVisibility() == View.VISIBLE) {
                    mRelativeLoading.setVisibility(View.GONE);
                }

                if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        }
    };

    //加载的loading布局
    private RelativeLayout mRelativeLoading;
    //加载进度
    private ProgressBar mProgressBar;

    private boolean showIndexLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wowan);
        //添加activity
        AppManager.getInstance().addActivity(this);
        cid = getIntent().getStringExtra("cid");
        cuid = getIntent().getStringExtra("cuid");
        deviceid = getIntent().getStringExtra("deviceid");
        mStringKey = getIntent().getStringExtra("key");
        oaid = getIntent().getStringExtra("oaid");
        appid = getIntent().getStringExtra("appid");
        appname = getIntent().getStringExtra("appname");
        if (TextUtils.isEmpty(cid) || TextUtils.isEmpty(cuid) || TextUtils.isEmpty(mStringKey)) {
            //清理activity
            AppManager.getInstance().finishActivity();
        }
        mBooleanPageNeedLoad = false;
        md5Str = "t=2&cid=" + cid + "&cuid=" + cuid + "&deviceid=" + deviceid + "&unixt="
                + System.currentTimeMillis();
        keycode = PlayMeUtil.encrypt(md5Str + mStringKey);

        String osversion = "";
        String phonemodel = "";
        try {
            osversion = Build.VERSION.RELEASE; // 操作系统版本号
            phonemodel = Build.MODEL; // 手机型号
        } catch (Exception e) {
            e.printStackTrace();
        }
        md5Str = md5Str + "&keycode=" + keycode + "&issdk=1&sdkver=" + mStringVer + "&oaid=" + oaid + "&osversion=" + osversion + "&phonemodel=" + phonemodel;
        mStringUrl = "https://m.playmy.cn/View/Wall_AdList.aspx?" + md5Str;
        if (!TextUtils.isEmpty(appid)) {
            mStringUrl = mStringUrl + "&appid=" + appid;
        }
        if (!TextUtils.isEmpty(appname)) {
            mStringUrl = mStringUrl + "&appname=" + URLEncoder.encode(appname);
        }

        initView();

    }

    private void initView() {

        mButton = (ImageButton) findViewById(R.id.top_back);

        mRelativeLoading = findViewById(R.id.rl_loading);

        mProgressBar = findViewById(R.id.pro_webview);

        if (mRelativeLoading != null) {
            mRelativeLoading.setVisibility(View.GONE);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }


        SharedPreferences sp = getSharedPreferences("authorities", Activity.MODE_PRIVATE);
        showIndexLoading = sp.getBoolean("showIndexLoading", false);

        mTextTitle = findViewById(R.id.tv_wowan_title);

        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);

        // 允许混合模式（http与https）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        /*
         * NORMAL：正常显示，没有渲染变化。 SINGLE_COLUMN：把所有内容放到WebView组件等宽的一列中。
         * //这个是强制的，把网页都挤变形了 NARROW_COLUMNS：可能的话，使所有列的宽度不超过屏幕宽度。 //好像是默认的
         */
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDefaultTextEncodingName("UTF-8");
        // 提高渲染的优先级
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setTextZoom(100);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                //记录最新的web进度
                if (newProgress > mIntLoadingRealProgress) {
                    mIntLoadingRealProgress = newProgress;
                    //开始做进度progress展示
                    if (mHandler != null && mRunnableProgress != null) {
                        mHandler.removeCallbacks(mRunnableProgress);
                        mHandler.post(mRunnableProgress);
                    }
                    if (showIndexLoading) {
                        if (mRelativeLoading != null && mRelativeLoading.getVisibility() != View.VISIBLE) {
                            mRelativeLoading.setVisibility(View.VISIBLE);
                        }

                        if (mProgressBar != null && mProgressBar.getVisibility() != View.VISIBLE) {
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                    }


                }

            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                //做一手防御，2秒钟后进度消失
                if ((mRelativeLoading != null && mRelativeLoading.getVisibility() == View.VISIBLE)
                        || (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE)) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mRelativeLoading != null && mRelativeLoading.getVisibility() == View.VISIBLE) {
                                mRelativeLoading.setVisibility(View.GONE);
                            }

                            if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
                                mProgressBar.setVisibility(View.GONE);
                            }

                        }
                    }, 2000);

                }

                if (null != mRefreshLayout) {
                    // 关闭加载进度条
                    mRefreshLayout.setRefreshing(false);
                }

                // 设置标题
                if (null != mTextTitle) {
                    mTextTitle.setText(view.getTitle());
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // TODO Auto-generated method stub
                // super.onReceivedSslError(view, handler, error);
                // 接受所有网站的证书，忽略SSL错误，执行访问网页
                handler.proceed();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                if (!TextUtils.isEmpty(url)) {
                    PlayMeUtil.openAdDetail(WowanIndex.this, cid, url);
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }

            }
        });


        mWebView.addJavascriptInterface(new X5JavaScriptInterface(WowanIndex.this, mWebView), "android");

        if (!TextUtils.isEmpty(mStringUrl)) {
            mWebView.loadUrl(mStringUrl);

        }

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (null != mWebView && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    //清理activity
                    AppManager.getInstance().finishActivity();
                }

            }
        });

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_srl);
        mRefreshLayout.setOnRefreshListener(this);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //清理activity
        AppManager.getInstance().finishActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBooleanPageNeedLoad = false;

        if (mHandler != null && mRunnableProgress != null) {
            mHandler.removeCallbacks(mRunnableProgress);
        }

        //回收AppManager
        AppManager.getInstance().AppExit(this);
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        // super.onResume();

        if (!mBooleanPageNeedLoad) {
            mBooleanPageNeedLoad = true;
            super.onResume();
            return;
        }

        if (mWebView != null) {
            mWebView.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mWebView.loadUrl("javascript:pageViewDidAppear()");
                }
            });
        }
        super.onResume();

    }

    @Override
    public void onRefresh() {
        String url = mWebView.getUrl();
        if (TextUtils.isEmpty(url)) {
            url = mStringUrl;
        }
        if (!TextUtils.isEmpty(url)) {
            mWebView.loadUrl(url);
        }
    }
}
