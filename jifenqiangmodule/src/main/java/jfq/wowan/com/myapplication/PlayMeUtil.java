package jfq.wowan.com.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.liulishuo.filedownloader.FileDownloader;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Created by Administrator on 2019/4/11.
 */

public class PlayMeUtil {
    /**
     * md5
     *
     * @param plaintext 明文
     * @return ciphertext 密文
     */
    public final static String encrypt(String plaintext) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            byte[] btInput = plaintext.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 初始化sdk
     *
     * @param authorities provider的author
     */
    public static void init(Application application, String authorities) {
        init(application, authorities, false, false);
    }

    //重载初始化sdk，控制页面加载loading效果
    @SuppressLint("CommitPrefEdits")
    public static void init(Application application, String authorities, boolean showIndexLoading, boolean showDetailLoading) {
        if (application == null) {
            return;
        }
        //下载器绕过https验证
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(10 * 60, TimeUnit.SECONDS)
                .writeTimeout(10 * 60, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1));
        // 添加 SSL 认证
        SSLTrustManager.addVerify(builder);
        // 自实现 OkHttp3Connection
        FileDownloader.setupOnApplicationOnCreate(application)
                .connectionCreator(new OkHttp3Connection.Creator(builder))
                .commit();
        //存入authorities
        authorities = TextUtils.isEmpty(authorities) ? application.getPackageName() + ".fileProvider" : authorities;
        SharedPreferences sp = application.getSharedPreferences("authorities", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("authorities", authorities);
        edit.putBoolean("showIndexLoading", showIndexLoading);
        edit.putBoolean("showDetailLoading", showDetailLoading);
        edit.commit();
    }

    /**
     * 原生调用打开广告详细页面
     *
     * @param url     打开地址
     * @param context
     * @param cid     渠道号
     */
    public static void openAdDetail(Context context, String cid, String url) {
        if (TextUtils.isEmpty(url) || context == null) {
            return;
        }
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra("url", url + "&issdk=1&sdkver=" + WowanIndex.mStringVer);
        intent.putExtra("cid", cid);
        context.startActivity(intent);
    }

    /**
     * 原生调用打开广告详细页面，通过adid
     *
     * @param context
     * @param cid      渠道号
     * @param cuid     用户标识userid、
     * @param deviceid 设备号
     * @param oaid     移动安全联盟oaid
     * @param key      秘钥
     */
    public static void openAdDetail(Context context, String cid, String cuid, String adid, String deviceid, String oaid, String key, String appid, String appname) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, DetailActivity.class);
        String md5Str = "t=2&cid=" + cid + "&cuid=" + cuid + "&deviceid=" + deviceid + "&unixt="
                + System.currentTimeMillis();
        String keycode = PlayMeUtil.encrypt(md5Str + key);

        String osversion = "";
        String phonemodel = "";
        try {
            osversion = Build.VERSION.RELEASE; // 操作系统版本号
            phonemodel = Build.MODEL; // 手机型号
        } catch (Exception e) {
            e.printStackTrace();
        }
        md5Str = md5Str + "&keycode=" + keycode + "&issdk=1&sdkver=" + WowanIndex.mStringVer + "&oaid=" + oaid + "&osversion=" + osversion + "&phonemodel=" + phonemodel + "&adid=" + adid;
        String mStringUrl = "https://m.playmy.cn/View/Wall_AdInfo.aspx?" + md5Str;
//        String mStringUrl = "http://test.playmy.cn/View/Wall_AdInfo.aspx?" + md5Str;
        if (!TextUtils.isEmpty(appid)) {
            mStringUrl = mStringUrl + "&appid=" + appid;
        }
        if (!TextUtils.isEmpty(appname)) {
            mStringUrl = mStringUrl + "&appname=" + URLEncoder.encode(appname);
        }
        intent.putExtra("url", mStringUrl);
        intent.putExtra("cid", cid);
        context.startActivity(intent);
    }

    /**
     * 原生调用打开广告详细页面，通过adid
     *
     * @param context
     * @param cid      渠道号
     * @param cuid     用户标识userid、
     * @param deviceid 设备号
     * @param oaid     移动安全联盟oaid
     * @param key      秘钥
     */
    public static void openAdDetail(Context context, String cid, String cuid, String adid, String deviceid, String oaid, String key) {
        openAdDetail(context, cid, cuid, adid, deviceid, oaid, key, "", "");
    }


    /**
     * 激活index页面
     */
    public static void openIndex(Context context, String cid, String cuid, String deviceid, String oaid, String key) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, WowanIndex.class);
        //渠道id
        intent.putExtra("cid", cid);
        //用户id
        intent.putExtra("cuid", cuid);
        //手机设备号
        intent.putExtra("deviceid", deviceid);
        //为了android10.0手机数据匹配，这里传入由移动安全联盟sdk提供的oaid值
        intent.putExtra("oaid", oaid);
        //秘钥
        intent.putExtra("key", key);
        context.startActivity(intent);
    }

    /**
     * 激活index页面(如果同cid有多个渠道，传入appid和appname区分数据)
     */
    public static void openIndex(Context context, String cid, String cuid, String deviceid, String oaid, String key, String appid, String appname) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, WowanIndex.class);
        //渠道id
        intent.putExtra("cid", cid);
        //用户id
        intent.putExtra("cuid", cuid);
        //手机设备号
        intent.putExtra("deviceid", deviceid);
        //为了android10.0手机数据匹配，这里传入由移动安全联盟sdk提供的oaid值
        intent.putExtra("oaid", oaid);
        //秘钥
        intent.putExtra("key", key);
        //appid
        intent.putExtra("appid", appid);
        //appname
        intent.putExtra("appname", appname);
        context.startActivity(intent);
    }

    /**
     * 将列表嵌入到某个viewgroup中
     */
    public static WebView addAdList(final Activity activity, ViewGroup viewGroup, final String cid, String cuid, String deviceid, String oaid, String key, String appid, String appname, final OnAddAdListCallBack onAddAdListCallBack) {
        if (activity == null || viewGroup == null || TextUtils.isEmpty(cid) || TextUtils.isEmpty(cuid) || TextUtils.isEmpty(key)) {
            if (onAddAdListCallBack != null) {
                onAddAdListCallBack.onFaile(-1, "参数不合法");
            }
            return null;
        }


        //开始拼接链接
        String md5Str = "t=2&cid=" + cid + "&cuid=" + cuid + "&deviceid=" + deviceid + "&unixt="
                + System.currentTimeMillis();
        String keycode = PlayMeUtil.encrypt(md5Str + key);

        String osversion = "";
        String phonemodel = "";
        try {
            osversion = Build.VERSION.RELEASE; // 操作系统版本号
            phonemodel = Build.MODEL; // 手机型号
        } catch (Exception e) {
            e.printStackTrace();
        }
        md5Str = md5Str + "&keycode=" + keycode + "&issdk=1&sdkver=1.0&oaid=" + oaid + "&osversion=" + osversion + "&phonemodel=" + phonemodel + "&listtype=1";

        String url = "https://m.playmy.cn/View/Wall_AdList.aspx?" + md5Str;
        if (!TextUtils.isEmpty(appid)) {
            url = url + "&appid=" + appid;
        }
        if (!TextUtils.isEmpty(appname)) {
            url = url + "&appname=" + URLEncoder.encode(appname);
        }
        //创建web并加载
        WebView webView = new WebView(activity);
        try {
            webView.setVerticalScrollBarEnabled(false);
            WebSettings webSettings = webView.getSettings();
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

            webView.setWebChromeClient(new WebChromeClient());

            webView.setWebViewClient(new WebViewClient() {

                private boolean hasSuccesed;

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (!hasSuccesed && onAddAdListCallBack != null) {
                        hasSuccesed = true;
                        onAddAdListCallBack.onSuccess();
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
                        PlayMeUtil.openAdDetail(activity, cid, url);
                        return true;
                    } else {
                        return super.shouldOverrideUrlLoading(view, url);
                    }

                }
            });

            webView.addJavascriptInterface(new X5JavaScriptInterface(activity, webView), "android");

            webView.loadUrl(url);

            //将web添加到容器之中
            viewGroup.removeAllViews();
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            webView.setLayoutParams(params);
            viewGroup.addView(webView);

        } catch (Exception e) {
            e.printStackTrace();
            if (onAddAdListCallBack != null) {
                onAddAdListCallBack.onFaile(-2, e.getMessage());
            }
        }

        return webView;


    }


    public static WebView addAdList(Activity activity, ViewGroup viewGroup, String cid, String cuid, String deviceid, String oaid, String key, OnAddAdListCallBack onAddAdListCallBack) {
        return addAdList(activity, viewGroup, cid, cuid, deviceid, oaid, key, "", "", onAddAdListCallBack);
    }

    public interface OnAddAdListCallBack {

        void onSuccess();

        void onFaile(int errorCode, String msg);
    }


}
