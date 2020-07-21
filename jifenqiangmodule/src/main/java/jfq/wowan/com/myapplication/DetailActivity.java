package jfq.wowan.com.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/11.
 */

public class DetailActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private String mStringCid;//渠道id

    public String getmStringCid() {
        return mStringCid;
    }

    public void setmStringCid(String mStringCid) {
        this.mStringCid = mStringCid;
    }

    public static WebView mWebViewSingleInstance; //可能需要与下载状态建立连接的web
    private WebView mWebView;


    private SwipeRefreshLayout mRefreshLayout;

    private String mStringUrl;

    private ImageButton mButton;

    public TextView mTextTitle;

    // 第一次不刷新、
    private boolean mBooleanPageNeedLoad;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wowan_detail);

        //添加activity
        AppManager.getInstance().addActivity(this);

        mBooleanPageNeedLoad = false;

        mStringUrl = getIntent().getStringExtra("url");
        mStringCid = getIntent().getStringExtra("cid");

        initView();
    }

    private void initView() {
        // TODO Auto-generated method stub
        mButton = (ImageButton) findViewById(R.id.top_back_detail);
        mTextTitle = findViewById(R.id.tv_wowan_title_detail);

        mWebView = (WebView) findViewById(R.id.webview_detail);
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

        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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

                if (!TextUtils.isEmpty(url) && !url.contains("Wall_Adinfo.aspx")) {
                    // 跳转外部浏览器
                    try {
                        Uri content_url = Uri.parse(url);
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        intent.setData(content_url);
                        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }

            }
        });
        mWebView.addJavascriptInterface(new X5JavaScriptInterface(DetailActivity.this, mWebView), "android");

        if (!TextUtils.isEmpty(mStringUrl)) {
//			Log.e("dferfew", mStringUrl);
            mWebView.loadUrl(mStringUrl);
        }

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //清理activity
                AppManager.getInstance().finishActivity();

            }
        });

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_srl_detail);
        mRefreshLayout.setOnRefreshListener(this);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //清理activity
        AppManager.getInstance().finishActivity();
    }

    @Override
    public void finish() {
        super.finish();
        mBooleanPageNeedLoad = false;
        //判断栈顶是否是detailactivity，如果不是，回收可能与下载建立连接的web
        if (AppManager.getInstance().currentActivity() == null || !(AppManager.getInstance().currentActivity() instanceof DetailActivity)) {
            mWebViewSingleInstance = null;//置空等待回收
//            Log.e("huishou", "finish: " );
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        mWebViewSingleInstance = mWebView;//可能打开了另一个页面，将此web指向对象改变，这里指向回来
        if (!TextUtils.isEmpty(mStringUrl)) {
            String adid = getParam(mStringUrl, "adid");
            if (!TextUtils.isEmpty(adid)) {
                try {
                    int intAdid = Integer.parseInt(adid);
                    mWebViewSingleInstance.setTag(intAdid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        if (!mBooleanPageNeedLoad) {
            mBooleanPageNeedLoad = true;
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

    }


    private String getParam(String url, String name) {
        try {
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(name)) {
                return "";
            }
            String[] urlParts = url.split("\\?");
            //没有参数
            if (urlParts.length == 1) {
                return "";
            }
            //有参数
            String[] params = urlParts[1].split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (name.equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }

    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        String url = mWebView.getUrl();
        if (TextUtils.isEmpty(url)) {
            url = mStringUrl;
        }
        if (!TextUtils.isEmpty(url)) {
            mWebView.loadUrl(url);
        }
    }

    private String mStringApkPath;

    public void setmStringApkPath(String mStringApkPath) {
        this.mStringApkPath = mStringApkPath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 101) {
            // 开始安装
            if (!TextUtils.isEmpty(mStringApkPath)) {
                File file = new File(mStringApkPath);
                if (file != null && file.exists()) {
                    install(this, file);
                }
            }
        }
    }

    /**
     * 通过隐式意图调用系统安装程序安装APK
     */
    private void install(Context context, File file) {

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 24) { // 判读版本是否在7.0以上
                // 参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致 参数3
                SharedPreferences sp = context.getSharedPreferences("authorities", Activity.MODE_PRIVATE);
                String authorities = sp.getString("authorities", context.getPackageName() + ".fileProvider");
                Uri apkUri = FileProvider.getUriForFile(context, authorities, file);

                // 添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            context.startActivity(intent);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

}
