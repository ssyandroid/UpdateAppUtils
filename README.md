# UpdateAppUtils
APP检测升级与下载APP并安装的Android原生API的封装工具类

>- 联网HttpURLConnection

>- json解析JSONObject

>- 以及进度对话框ProgressDialog

如果需要 更改自己的定制联网方法与解析可以在相应方法进行更改替换
如果需要自定义对话框与进度框，同样可以更改替换

需要注意引用v7兼容包，当然可以不引用，需要把import android.support.v7.app.AlertDialog;改为import android.app.AlertDialog;

调用方法为:

<pre>
  /**
 	* ************调用示例***********
 	* 检测更新方法，在需要更新的地方调用该方法
 	*/
	public static void checkUpdate(Context context) {
	  //示例检查更新地址
	  String url = "http://localhost:8081/view/app/pub?event=update&platform=android&versionCode=" +    UpdateAppUtils.getVerCode(context);
	  //实例化更新工具
	  UpdateAppUtils versionUtils = new UpdateAppUtils(context, false);
	  //检查更新
 	  versionUtils.checkUpdate(url);
  }
 </pre>

