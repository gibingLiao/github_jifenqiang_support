package jfq.wowan.com.myapplication;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.liulishuo.filedownloader.FileDownloader;

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
     * 原生调用打开广告详细页面
     *
     * @param url     打开地址
     * @param context
     * @param cid  渠道号
     */
    public static void openAdDetail(Context context,String cid, String url) {
        if (TextUtils.isEmpty(url) || context == null) {
            return;
        }
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra("url", url + "&issdk=1&sdkver=" + WowanIndex.mStringVer);
        intent.putExtra("cid", cid);
        context.startActivity(intent);
    }

    /**
     * 初始化sdk
     * @param authorities provider的author
     */
    public static void init(Application application,String authorities) {
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
        sp.edit().putString("authorities", authorities).commit();
    }

    /**
     * 激活index页面
     */
    public static void openIndex(Context context,String cid,String cuid,String deviceid,String oaid,String key){
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
        intent.putExtra("key",key);
        context.startActivity(intent);
    }

    /**
     * 激活index页面(如果同cid有多个渠道，传入appid和appname区分数据)
     */
    public static void openIndex(Context context,String cid,String cuid,String deviceid,String oaid,String key,String appid,String appname){
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
        intent.putExtra("key",key);
        //appid
        intent.putExtra("appid",appid);
        //appname
        intent.putExtra("appname",appname);
        context.startActivity(intent);
    }

}
