package com.uiot.video.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * UpdateAppUtils 更新版本
 * <p/>
 * 注意需要存储读写权限与联网权限
 *
 * @author 苏三元
 * @date 2016/10/18 10:59
 */
public class UpdateAppUtils {
    private static final String TAG = "UpdateAppUtils";
    private String des = null;// 版本描述
    private String apkurl = null;// 新版本url
    private String version;// 版本
    private Context ct;//上下文
    private boolean isShowProgressbar;//是否需要打圈
    private ProgressDialog progressDialog;//打圈框
    private ProgressDialog progressDialog_update;//升级进度框
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: // 下载完成，点击安装
                    if (progressDialog_update.getProgress() == 100) {
                        dismissUpdateDialog();
                        installAPK((File) msg.obj);
                    }
                    break;
                case 1://下载失败
                    dismissUpdateDialog();
                    showToast("下载失败，请稍后重试");
                    break;
                case 2://正在下载
                    showToast("正在下载中");
                    break;
                case 3://开始下载,下载中进度
                    float p = (float) msg.obj;
                    setDialogProgress((int) p);
                    break;
                case 111://检测更新
                    checkIsUpdate((String) msg.obj);
                    break;

            }
        }
    };
	
	 /**
     * ************调用示例***********
     * 检测更新方法，在需要更新的地方调用该方法
     */
    public static void checkUpdate(Context context) {
        //示例检查更新地址
        String url = "http://localhost:8081/view/app/pub?event=update&platform=android&versionCode=" + UpdateAppUtils.getVerCode(context);
        //实例化更新工具
        UpdateAppUtils versionUtils = new UpdateAppUtils(context, false);
        //检查更新
        versionUtils.checkUpdate(url);
    }

    /**
     * 实例化更新工具类
     *
     * @param context           该页面的上下文
     * @param isShowProgressbar 是否需要显示检查更新打圈
     */
    public UpdateAppUtils(Context context, Boolean isShowProgressbar) {
        this.ct = context;
        this.isShowProgressbar = isShowProgressbar;
    }

    /**
     * 得到应用程序的版本号名 VersionName
     *
     * @param ct Context上下文
     */
    public static String getVersionName(Context ct) {
        try {
            // 管理手机的APP
            PackageManager packageManager = ct.getPackageManager();
            // 得到APP功能清单文件
            PackageInfo info = packageManager.getPackageInfo(ct.getPackageName(), 0);
            return info.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 得到应用程序的版本 code
     *
     * @param ct Context上下文
     */
    public static int getVerCode(Context ct) {
        int verCode = -1;
        try {
            // 管理手机的APP
            PackageManager packageManager = ct.getPackageManager();
            // 得到APP功能清单文件
            PackageInfo info = packageManager.getPackageInfo(ct.getPackageName(), 0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }

    /**
     * 检查更新
     *
     * @param url url 地址
     */
    public void checkUpdate(final String url) {

        if (isShowProgressbar) {
            creatProgressbar();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = checkUpdateConnect(url);

                Message msg = handler.obtainMessage();
                msg.what = 111;
                msg.obj = result;
                handler.sendMessage(msg);
            }
        }).start();
    }

    /**
     * 请求服务
     *
     * @param url url
     * @return String
     */
    private String checkUpdateConnect(String url) {
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setConnectTimeout(5 * 1000);//设置连接超时时间
            httpURLConnection.setReadTimeout(10 * 1000);//设置从主机读取数据超时（单位：毫秒）
            httpURLConnection.setDoInput(true);//打开输入流，以便从服务器获取数据
//            httpURLConnection.setDoOutput(true);//打开输出流，以便向服务器提交数据
            httpURLConnection.setRequestMethod("POST");//设置以Post方式提交数据
            httpURLConnection.setUseCaches(false);//使用Post方式不能使用缓存
            int response = httpURLConnection.getResponseCode(); //获得服务器的响应码
            if (response == HttpURLConnection.HTTP_OK) {//200
                InputStream inptStream = httpURLConnection.getInputStream();
                return dealResponseResult(inptStream);
            } else {
                Log.i(TAG, "httpErr:response：" + response);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 处理服务器的响应结果（将输入流转化成字符串）
     *
     * @param inputStream 服务器的响应输入流
     * @return String
     */
    private String dealResponseResult(InputStream inputStream) {
        String resultData = null; //存储处理结果
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
            byte[] enByte = byteArrayOutputStream.toByteArray();
            resultData = new String(enByte);
            Log.i(TAG, "请求结果为：" + resultData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultData;
    }

    /**
     * 解析结果，判断是否升级
     * <p/>
     * 返回json比如：{"data":{"platform":"android","detail":"版本升级","fileName":null,"url":"http://localhost:8085/app.apk","version":"1.0.1"},"code":1,"msg":"操作成功"}
     *
     * @param result json
     */
    private void checkIsUpdate(String result) {
        if (!TextUtils.isEmpty(result)) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                int code = jsonObject.getInt("code");
                if (code == 1) {
                    if (jsonObject.has("data")) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        if (data != null) {
//                            String platform = data.getString("platform");
//                            String fileName = data.getString("fileName");//文件名
                            if (data.has("detail")) {
                                des = data.getString("detail");//描述
                            }
                            if (data.has("url")) {
                                apkurl = data.getString("url");//下载地址
                            }
                            if (data.has("version")) {
                                version = data.getString("version");//版本
                            }

                            //现在为地址不为空就提示升级
                            if (!TextUtils.isEmpty(apkurl) && !"NULL".equalsIgnoreCase(apkurl)) {
                                showUpdateDialog();
                            } else {
                                showToast("已是最新版本");
                            }

                            //采用比较versoinName
                            /*if (!TextUtils.isEmpty(version)) {
                                String newVersion = version.replaceAll("\\.", "");
                                String currentVersion = getVersionName(ct);

                                Pattern pattern = Pattern.compile("[0-9]*");
                                Matcher isNum = pattern.matcher(newVersion);
                                Matcher isNumCurr = pattern.matcher(currentVersion);

                                if (isNum.matches() && isNumCurr.matches()) {//是全数字
                                    if ( Long.parseLong(currentVersion) < Long.parseLong(newVersion)) {
                                        showUpdateDialog();
                                    }else{
                                     showToast("已是最新版本");
                                    }
                                }else{
                                     showToast("已是最新版本");
                                 }
                                Log.i(TAG, newVersion+" currentVersion：" + currentVersion);
                            }*/

                            //采用code比较
                            //同上取值判断

                        } else {
                            showToast("已是最新版本");
                        }
                    } else {
                        showToast("已是最新版本");
                    }
                } else {
                    showToast("检查更新失败，请稍后再试");
                }
            } catch (Exception e) {
                showToast("检查更新失败，请稍后再试");
                e.printStackTrace();
            }
        } else {
            showToast("检查更新失败，请稍后再试");
        }
        dismissProgressbar();
    }

    /**
     * 显示吐司提示
     */
    private void showToast(String msg) {
        Toast.makeText(ct, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示打圈框
     */
    private void creatProgressbar() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(ct);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("检查更新中...");
            progressDialog.show();
        } else {
            progressDialog.show();
        }
    }

    /**
     * 取消打圈框
     */
    private void dismissProgressbar() {
        if (progressDialog != null && (progressDialog.isShowing())) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示是否升级对话框
     */
    private void showUpdateDialog() {
        if (TextUtils.isEmpty(des)) {
            des = "有新的版本，是否更新？";
        }
        //确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(ct);
        builder.setCancelable(false);
        builder.setTitle("新版本");
        builder.setMessage(des);
        builder.setPositiveButton("立刻升级",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadApp();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton("下次再说",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();

    }

    /**
     * 下载APP
     */
    private void downloadApp() {
        updateDialog();
        startDownloadApp();
    }

    /**
     * 开始下载
     */
    private void startDownloadApp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long totalSize;// 文件总大小
                    long downloadCount = 0;// 已经下载好的大小
                    String path;
                    // 下载apk，并且安装
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        path = ct.getExternalFilesDir(null) + "/apk";
                    } else {
                        path = ct.getFilesDir() + "/apk";
                    }

                    File filePath = new File(path);
                    if (!filePath.exists() || !filePath.isDirectory()) {
                        filePath.mkdirs();
                    }

                    File apkPath = new File(filePath, "app.apk");
                    if (!apkPath.exists()) {
                        apkPath.createNewFile();
                    }

                    URL url = new URL(apkurl);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(10 * 1000);
                    httpURLConnection.setReadTimeout(10 * 1000);
                    httpURLConnection.setDoInput(true);
                    // 获取下载文件的size
                    totalSize = httpURLConnection.getContentLength();
                    int response = httpURLConnection.getResponseCode(); //获得服务器的响应码
                    if (response == HttpURLConnection.HTTP_OK) {//200
                        InputStream inputStream = httpURLConnection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(apkPath, false);// 文件存在则覆盖掉
                        byte buffer[] = new byte[1024];
                        int readsize = 0;
                        while ((readsize = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, readsize);
                            downloadCount += readsize;// 时时获取下载到的大小
                            float progress = downloadCount * 1.0f / totalSize;
                            Message msg = handler.obtainMessage();
                            msg.what = 3;
                            msg.obj = progress * 100;
                            handler.sendMessage(msg);
                            Log.i(TAG, "downloadCount：" + downloadCount);
                        }

                        httpURLConnection.disconnect();
                        outputStream.flush();
                        inputStream.close();
                        outputStream.close();

                        Message msg = handler.obtainMessage();
                        msg.what = 0;
                        msg.obj = apkPath;
                        handler.sendMessage(msg);

                        Log.i(TAG, apkPath + " downloadCount：" + downloadCount);
                    } else {
                        handler.sendEmptyMessage(1);
                        Log.i(TAG, "httpErr:response：" + response);
                    }
                } catch (Exception e) {
                    handler.sendEmptyMessage(1);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 设置进度
     *
     * @param progress 进度值
     */
    private void setDialogProgress(int progress) {
        progressDialog_update.setProgress(progress);
    }

    /**
     * 显示下载升级进度的对话框 updateDialog
     */
    private void updateDialog() {
        progressDialog_update = new ProgressDialog(ct);
        progressDialog_update.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog_update.setMessage("正在升级，请稍后");
        progressDialog_update.setMax(100);
        progressDialog_update.setCancelable(false);
        progressDialog_update.setProgress(0);
        progressDialog_update.show();
    }

    /**
     * 取消进度框
     */
    private void dismissUpdateDialog() {
        if (progressDialog_update != null && (progressDialog_update.isShowing())) {
            progressDialog_update.dismiss();
        }
    }

    /**
     * 安装APP
     *
     * @param t File
     */
    private void installAPK(File t) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(t), "application/vnd.android.package-archive");
        ct.startActivity(intent);
    }
}
