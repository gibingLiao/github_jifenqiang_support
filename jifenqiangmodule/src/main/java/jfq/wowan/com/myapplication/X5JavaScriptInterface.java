package jfq.wowan.com.myapplication;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.RemoteViews;
import android.widget.Toast;

@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class X5JavaScriptInterface {

    private Activity mActivity;

    private WebView mWebView;

    private Handler mHandler = new Handler();


    public X5JavaScriptInterface(Activity activity, WebView webview) {
        // TODO Auto-generated constructor stub
        this.mActivity = activity;
        this.mWebView = webview;
    }

    /**
     * 判断应用示是否安装
     *
     * @param packageName 应用包名
     * @return true 已经安装，false 未安装
     */
    @JavascriptInterface
    public void CheckAppIsInstall(final String packageName) {
        if (null == mActivity || null == mWebView) {
            return;
        }
        boolean flag = false;
        PackageManager pm = mActivity.getPackageManager();
        List<PackageInfo> pakageinfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo pi : pakageinfos) {
            if (packageName.equalsIgnoreCase(pi.packageName)) {
                flag = true;
            }
        }
        if (flag) {// 表示已经安装了
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    // CheckAppCallback(BoundleId,Version,Sign);
                    String BoundleId = packageName;
                    mWebView.loadUrl("javascript:CheckAppCallback('" + BoundleId + "')");
                }
            });
        } else {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:CheckAppNoInstall()");
                }
            });
        }
    }

    /**
     * @param packageName 指定打开应用的包名
     */
    @JavascriptInterface
    public void startAnotherApp(String packageName) {
        try {
            if (mActivity == null || TextUtils.isEmpty(packageName)) {
                return;
            }

            if (!isAPKinstall(mActivity, packageName)) {
                return;
            }

            // 打开指定包名的应用
            startAnotherApp(mActivity, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断应用示是否安装
     *
     * @param packageName 应用包名
     * @return true 已经安装，false 未安装
     */
    public static boolean isAPKinstall(Context context, String packageName) {
        boolean flag = false;
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pakageinfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo pi : pakageinfos) {
            if (packageName.equalsIgnoreCase(pi.packageName)) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                packageInfo = null;
                e.printStackTrace();
            }
            if (packageInfo == null) {
                flag = false;
            } else {
                flag = true;
            }
        }

        return flag;
    }

    /**
     * 根据包名打开APP
     *
     * @param packageName
     */
    public static void startAnotherApp(Context context, String packageName) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(pi.packageName);
            List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(resolveIntent, 0);
            ResolveInfo ri = apps.iterator().next();
            if (ri != null) {
                packageName = ri.activityInfo.packageName;
                String className = ri.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName(packageName, className);
                intent.setComponent(cn);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件保存地址
     *
     * @return
     */
    public static String getDownPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
        } else {
            return "";
        }
        return sdDir.toString() + "/wowansdk";
    }

    /**
     * 通过隐式意图调用系统安装程序安装APK
     */
    public void install(Context context, File file) {

        try {

            boolean haveInstallPermission;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 先判断是否有安装未知来源应用的权限
                haveInstallPermission = mActivity.getPackageManager().canRequestPackageInstalls();
                if (!haveInstallPermission) {
                    // 弹框提示用户手动打开
                    Uri packageURI = Uri.parse("package:" + mActivity.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                    intent.putExtra("path", file.getPath());
                    mActivity.startActivityForResult(intent, 101);
                    if (mActivity instanceof DetailActivity) {
                        DetailActivity detailActivity = (DetailActivity) mActivity;
                        detailActivity.setmStringApkPath(file.getPath());
                    }
                    return;
                }
            }

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


    private FileDownloadListener mDownloadListener;
//	private NotificationManager notificationManager;
    // 取消下载
//	public static String ACTION_CANCEL_DOWNLOAD_APK = "ACTION_CANCEL_DOWNLOAD_APK";

    //	private List<BaseDownloadTask> mDownloadTaskList = new ArrayList<>();
    // 权限sd卡
    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    public static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * 给js的多任务下载的方法 isInstall 是否自动安装 0 -- 不自动安装 1--自动安装
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @JavascriptInterface
    public void downloadApkFile(int whichTask, int isInstall, String url) {
        try {
            if (TextUtils.isEmpty(url)) {
                return;
            }
            // 如果下载地址是一个重定向地址 就得先取最终的地址
            if (!url.contains(".apk")) {
                url = redirectPath(url);
            }
            if (mActivity == null || mWebView == null) {
                return;
            }
            String path = "";
            int sdkVersion = mActivity.getApplicationInfo().targetSdkVersion;
            if (sdkVersion >= 29) {
                //适配android10沙盒话路径,无需申请权限，直接获取沙盒路径
                File externalFilesDir = mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (externalFilesDir == null) {
                    return;
                }
                path = externalFilesDir.getPath();
            } else {
                //低于9.0，获取sdc权限，并拿到路径
                // Check if we have write permission
                int permission = ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(mActivity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    return;
                }
                path = getDownPath();
            }


            if (TextUtils.isEmpty(path)) {
                Toast.makeText(mActivity, "请插入SD卡", Toast.LENGTH_LONG).show();
                return;
            }

            File localdir = new File(path);
            if (!localdir.exists()) {
                localdir.mkdir();
            }


            //cid_adid.apk
            String mApkName = whichTask + ".apk";
            if (mActivity instanceof DetailActivity) {
                DetailActivity activity = (DetailActivity) mActivity;
                String cid = activity.getmStringCid();
                mApkName = cid + "_" + mApkName;
            }

            path = path + "/" + mApkName;


            mDownloadListener = createLis(whichTask, isInstall);
            FileDownloader.getImpl().create(url).setPath(path).setListener(mDownloadListener).setAutoRetryTimes(1)
                    .setTag(whichTask).asInQueueTask().enqueue();
            FileDownloader.getImpl().start(mDownloadListener, false);

            if (DetailActivity.mWebViewSingleInstance != null) {
                DetailActivity.mWebViewSingleInstance.setTag(whichTask);//给要下载的web设置一个tag
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // 获取重定向后最终地址
    public String redirectPath(String str) {
        URL url = null;
        String realURL = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(str);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "PacificHttpClient");
            conn.setInstanceFollowRedirects(true);
            conn.getResponseCode();// trigger server redirect
            realURL = conn.getURL().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return str;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return realURL;
    }

    private FileDownloadListener createLis(final int whichTask, final int mIsInstall) {
        return new FileDownloadListener() {

            private int isInstall = mIsInstall;
            private int which = whichTask;
            private boolean isCancle = false;

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes,
                                     int totalBytes) {

                super.connected(task, etag, isContinue, soFarBytes, totalBytes);

            }

            @Override
            protected void progress(final BaseDownloadTask task, int soFarBytes, int totalBytes) {
                final int percent = (int) ((double) soFarBytes / (double) totalBytes * 100);

                try {

                    if (DetailActivity.mWebViewSingleInstance != null && (int) task.getTag() == (int) DetailActivity.mWebViewSingleInstance.getTag()) {
                        DetailActivity.mWebViewSingleInstance.post(new Runnable() {
                            @Override
                            public void run() {
                                DetailActivity.mWebViewSingleInstance.loadUrl("javascript:downloadApkFileProcessListener("
                                        + task.getTag() + "," + percent + ")");
                                Log.e("onResume", "onResume: " + DetailActivity.mWebViewSingleInstance + "   " + percent);
                            }
                        });
                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {

            }

            @Override
            protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
                super.retry(task, ex, retryingTimes, soFarBytes);

            }

            @Override
            protected void completed(final BaseDownloadTask task) {

                try {
                    if (DetailActivity.mWebViewSingleInstance != null && (int) task.getTag() == (int) DetailActivity.mWebViewSingleInstance.getTag()) {
                        DetailActivity.mWebViewSingleInstance.post(new Runnable() {

                            @Override
                            public void run() {
                                DetailActivity.mWebViewSingleInstance.loadUrl("javascript:downloadApkFileFinishListener("
                                        + task.getTag() + ",'" + task.getPath() + "')");

                            }
                        });
                    }

                    if (1 == isInstall) {
                        // 需要自动安装
                        install(mActivity, new File(task.getPath()));
                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

            }

            @Override
            protected void error(final BaseDownloadTask task, final Throwable e) {

                try {
                    if (DetailActivity.mWebViewSingleInstance != null && (int) task.getTag() == (int) DetailActivity.mWebViewSingleInstance.getTag()) {
                        DetailActivity.mWebViewSingleInstance.post(new Runnable() {

                            @Override
                            public void run() {
                                DetailActivity.mWebViewSingleInstance.loadUrl("javascript:downloadApkFileErrorListener("
                                        + task.getTag() + ",'" + e.toString() + "')");

                            }
                        });
                    }

                    // 下载失败，删除文件，告知js
                    File localFile = new File(task.getPath());
                    if (localFile.exists()) {
                        localFile.delete();
                    }

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

            @Override
            protected void warn(BaseDownloadTask task) {


            }
        };
    }

    /**
     * 给js的安装apk的方法 //id：apk标识， path: 本地apk路径 InstallApkListener(int id,int
     * status,String msg){} //id：apk标识，status:安装状态（0唤起安装失败，1唤起安装成功）， msg: 消息
     */

    @JavascriptInterface
    public void InstallApk(final int whichTask, String path) {
        if (TextUtils.isEmpty(path)) {
            if (mWebView != null) {
                mWebView.post(new Runnable() {

                    @Override
                    public void run() {
                        mWebView.loadUrl(
                                "javascript:InstallApkListener(" + whichTask + "," + 0 + ",'" + "安装路径为空" + "')");

                    }
                });
            }
            return;
        }
        File localFile = new File(path);
        if (!localFile.exists()) {
            if (mWebView != null) {
                mWebView.post(new Runnable() {

                    @Override
                    public void run() {
                        mWebView.loadUrl(
                                "javascript:InstallApkListener(" + whichTask + "," + 0 + ",'" + "安装路径不存在" + "')");

                    }
                });
            }
            return;
        }

        install(mActivity, localFile);
        if (mWebView != null) {
            mWebView.post(new Runnable() {

                @Override
                public void run() {
                    mWebView
                            .loadUrl("javascript:InstallApkListener(" + whichTask + "," + 1 + ",'" + "唤起安装成功" + "')");

                }
            });
        }

    }

    /**
     * 根据指定的包名卸载apk
     */
    @JavascriptInterface
    public void uninstallApk(String packName) {
        if (TextUtils.isEmpty(packName) || null == mActivity) {
            return;
        }
        Uri packageURI = Uri.parse("package:" + packName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        mActivity.startActivity(uninstallIntent);
    }

    /**
     * 刷新当前页面
     */
    @JavascriptInterface
    public void RefreshWeb() {
        if (null == mHandler || null == mWebView)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String subUrl = mWebView.getUrl();
                if (!TextUtils.isEmpty(subUrl)) {
                    mWebView.loadUrl(subUrl);
                }
            }
        });
    }

    /**
     * 外部浏览器打开指定链接url
     */
    @JavascriptInterface
    public void Browser(String url) {
        if (null == mActivity)
            return;
        // 跳转外部浏览器
        try {
            Uri content_url = Uri.parse(url);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(content_url);
            if (mActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                mActivity.startActivity(intent);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 复制内容到剪切板里
     *
     * @param officialaccount
     */
    @JavascriptInterface
    public void copyContent(String officialaccount, String tips) {
        try {
            if (officialaccount == null) {
                return;
            }

            if (mActivity == null) {
                return;
            }

            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(officialaccount);

            if (TextUtils.isEmpty(tips)) {
                // MyToast.Show(activity, "已拷贝到剪切板");
            } else {
                Toast.makeText(mActivity, tips, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取剪切板内容
     */
    @JavascriptInterface
    public void GetCopyContent() {
        final String content = getCopyBoradContent();
        if (mWebView != null) {
            mWebView.post(new Runnable() {

                @Override
                public void run() {
                    mWebView.loadUrl("javascript:APPReturnClipboard('" + content + "')");

                }
            });
        }
    }

    /**
     * 获取剪切板内容
     */
    public String getCopyBoradContent() {
        try {

            if (mActivity == null) {
                return "";
            }

            ClipboardManager clipboardManager = (ClipboardManager) mActivity
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    || clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                ClipData cd = clipboardManager.getPrimaryClip();
                Item item = cd.getItemAt(0);
                String content = "";
                try {
                    content = item.getText().toString();
                } catch (Exception e) {
                    content = "";
                }

                // 内容为空 返回null
                if (TextUtils.isEmpty(content)) {
                    return "";
                }
                return content;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * web页面调用打开广告详细页面
     *
     * @param url 打开地址
     */
    @JavascriptInterface
    public void openAdDetail(String url) {
        if (TextUtils.isEmpty(url) || mActivity == null) {
            return;
        }
        Intent intent = new Intent(mActivity, DetailActivity.class);
        intent.putExtra("url", url + "&issdk=1&sdkver=" + WowanIndex.mStringVer);
        mActivity.startActivity(intent);
    }

    /**
     * 当前web加载某个地址
     */
    @JavascriptInterface
    public void loadWebUrl(String url) {
        if (TextUtils.isEmpty(url) || mWebView == null) {
            return;
        }
        mWebView.loadUrl(url);
    }


    /**
     * web页面调用打开广告详细页面
     *
     * @param url 打开地址
     * @param cid 渠道号
     */
    @JavascriptInterface
    public void openAdDetailWithCid(String url, String cid) {
        if (TextUtils.isEmpty(url) || mActivity == null || TextUtils.isEmpty(cid)) {
            return;
        }
        Intent intent = new Intent(mActivity, DetailActivity.class);
        intent.putExtra("url", url + "&issdk=1&sdkver=" + WowanIndex.mStringVer);
        intent.putExtra("cid", cid);
        mActivity.startActivity(intent);
    }


    /**
     * 关闭当前的activity
     */
    @JavascriptInterface
    public void finishCurrentPage() {
        if (mActivity == null) {
            return;
        }
        AppManager.getInstance().finishActivity(mActivity);
    }


}
