
package appapis.queryfiles;

import android.content.SharedPreferences;
import android.media.audiofx.AcousticEchoCanceler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import me.sagan.r1helper.AlexaService;
import me.sagan.r1helper.App;
import me.sagan.r1helper.BackgroundService;
import me.sagan.r1helper.StreamGobbler;
import me.sagan.r1helper.Tool;
import me.sagan.r1helper.R;

/**
 *
 * @author cis
 */
public class AppApis {

    public AppApis(){
    }

    public String root(HashMap qparms){
        //demo of simple html webpage from controller method 
        androidhttpweb.TinyWebServer.CONTENT_TYPE="text/plain";
        return "HTTP API 列表：\n" +
                "\n" +
                "* GET /reboot : 重启设备。（需要 root 权限）\n" +
                "* GET /run?cmd=pwd : 在设备上运行一个命令并返回结果。（需要 root 权限）\n" +
                "* GET /status : 返回 app 运行状态、配置、最近日志等信息。\n" +
                "* GET /log : 返回 app 运行日志。\n" +
                "* GET /set?lang=ja-JP : 更改设备设定。可选参数：\n" +
                "    * lang : 设置 Alexa 语音助手使用的语言。可选的 lang 语言值包括：de-DE, en-AU, en-CA, en-GB, en-IN, en-US, es-ES, es-MX, fr-CA, fr-FR, it-IT, ja-JP。\n" +
                "    * mode : 更改 app 运行模式。mode 值：0 - 正常模式(显示 LED 灯和氛围灯效果); 1 - 关闭 LED 灯; 2 - 蓝牙配对模式 (LED灯交替闪烁蓝、白色)。\n" +
                "* GET /config?sensitivity=0.3 : 获取或修改 app (持久化)首选项参数。可选参数：\n" +
                "    * sensitivity : 语音助手唤醒词识别敏感度。范围 [0,1]。数值越大则越容易唤醒，但误唤醒率也会更高。\n" +
                "    * setpassword : 设置 HTTP API 的密码。\n" +
                "    * recordPausing : 毫秒数。> 0 则启用录音时本地语音结束检测（录音至少需已开始这么长时间才可能结束）。不推荐。Alexa服务器云端会自动检测语音结束。\n" +
                "* GET /reset : 重置 Alexa 语音助手状态。如果语音助手一直没反应或不听使唤，可以尝试重置。\n" +
                "\n" +
                "HTTP API 默认无需验证，使用 GET /config?setpassword=123 接口可以设置密码。设置密码以后，以上所有 API 访问时都必须带上 password 参数提供当前密码。";
    }

    // GET /reboot
    public String reboot(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        try {
            String [] setPermissiveCmd={"su","-c","reboot"};
            Runtime.getRuntime().exec(setPermissiveCmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    public String log(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="text/plain";
        return App.log;
    }

    public String start(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        try {
            if(BackgroundService.instance != null ) {
                BackgroundService.instance.startFrontActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    public String set(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        Gson gson = new Gson();
        Map<String,Object> result = new HashMap<String, Object>();
        try {
            if( qparms.containsKey("lang") ) {
                if(AlexaService.running) {
                    String lang = qparms.get("lang").toString();
                    AlexaService.setLanguage(lang);
                    result.put("lang", lang);
                }
            }
            if( qparms.containsKey("mode") ) {
                int mode = Integer.parseInt(qparms.get("mode").toString());
                App.mode = mode;
                result.put("mode", mode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson.toJson(result);
    }

    public String config(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        if( qparms.containsKey("setpassword") ) {
            qparms.put("password", Tool.empty(qparms.get("setpassword"))
                    ? "" : BCrypt.withDefaults().hashToString(10, qparms.get("setpassword").toString().toCharArray()));
            qparms.remove("setpassword");
        }
        Gson gson = new Gson();
        try {
            if( AlexaService.instance != null ) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AlexaService.instance);
                if( qparms.keySet().size() > 0 ) {
                    SharedPreferences.Editor editor = preferences.edit();
                    if( !Tool.empty(qparms.get("_clear")) ) {
                        editor.clear();
                    } else {
                        for( Object key : qparms.keySet() ) {
                            if( !Tool.empty(qparms.get(key)) ) {
                                if( key.toString().equals("recordPausing") ) {
                                    editor.putInt(key.toString(), Tool.parseInt(qparms.get(key)));
                                } else {
                                    editor.putString(key.toString(), qparms.get(key).toString());
                                }
                            } else {
                                editor.remove(key.toString());
                            }
                        }
                    }
                    editor.apply();
                    AlexaService.instance.config();
                }
                Map<String, Object> config = new HashMap<String, Object>(preferences.getAll());
                if( !config.containsKey("recordPausing") ) {
                    config.put("recordPausing", 0);
                }
                if( !config.containsKey("sensitivity") ) {
                    config.put("sensitivity", AlexaService.instance.getString(R.string.default_sensitivity));
                }
                return gson.toJson(config);
            }
        } catch (Exception e) {}
        return "{}";
    }

    public String status(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        Gson gson = new Gson();
        Map<String,Object> result = new HashMap<String, Object>();
        try {
            if(AlexaService.instance != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AlexaService.instance);
                result.put("isLogined", AlexaService.instance.isLogin == 2 ? true : false);
                result.put("config",  gson.fromJson( config(null), Map.class ));
            }
            result.put("mode", App.mode);
            result.put("root", App.permissiive);
            result.put("log", StringUtils.substring(App.log, -1000) );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson.toJson(result);
    }

    public String info(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        Gson gson = new Gson();
        Map<String,Object> result = new HashMap<String, Object>();
        result.put("acousticEchoCancelerAvailable", AcousticEchoCanceler.isAvailable());
        return gson.toJson(result);
    }

    public String reset(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        try {
            if(AlexaService.running) {
                AlexaService.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    public String clear(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        try {
            if(AlexaService.instance != null) {
                AlexaAudioPlayer.trimCache(AlexaService.instance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    public String run(HashMap qparms){
        androidhttpweb.TinyWebServer.CONTENT_TYPE="application/json";
        Gson gson = new Gson();
        Map<String,Object> result = new HashMap<String, Object>();
        try {
            String cmd = "su -c ";
            int exitVal = 0;
            cmd += qparms.get("cmd").toString();
            Process proc = Runtime.getRuntime().exec(cmd);

            StreamGobbler errorGobbler = new
                    StreamGobbler(proc.getErrorStream(), "ERROR");
            // any output?
            StreamGobbler outputGobbler = new
                    StreamGobbler(proc.getInputStream(), "OUTPUT");
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
            exitVal = proc.waitFor();
            result.put("error", exitVal);
            result.put("stdout", outputGobbler.output);
            result.put("stderr", errorGobbler.output);
            result.put("cmd", cmd);
        } catch (Exception e) {
            e.printStackTrace();
           result.put("error", -1);
        }
        return gson.toJson(result);
    }

    //implement web callback here and access them using method name
}