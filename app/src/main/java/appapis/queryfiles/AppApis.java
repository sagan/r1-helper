
package appapis.queryfiles;

import java.io.IOException;
import java.util.HashMap;

import me.sagan.r1helper.BackgroundService;
import me.sagan.r1helper.StreamGobbler;
import me.sagan.r1helper.Tool;

/**
 *
 * @author cis
 */
public class AppApis {

    public AppApis(){
    }

    public String helloworld(HashMap qparms){
        //demo of simple html webpage from controller method 
        androidhttpweb.TinyWebServer.CONTENT_TYPE="text/html";
        return "<html><head><title>Simple HTML and Javascript Demo</title>\n" +
                "  <script>\n" +
                "  \n" +
                "</script>\n" +
                "  \n" +
                "  </head><body style=\"text-align:center;margin-top: 5%;\" cz-shortcut-listen=\"true\" class=\"\">\n" +
                "    <h3>Say Hello !</h3>\n" +
                "<div style=\"text-align:center;margin-left: 29%;\">\n" +
                "<div id=\"c1\" style=\"width: 100px;height: 100px;color: gray;background: gray;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c2\" style=\"width: 100px;height: 100px;color: gray;background: yellow;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c3\" style=\"width: 100px;height: 100px;color: gray;background: skyblue;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c4\" style=\"width: 100px;height: 100px;color: gray;background: yellowgreen;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c5\" style=\"width: 100px;height: 100px;color: gray;background: red;border-radius: 50%;position: ;position: ;float: left;\" class=\"\"></div></div>\n" +
                "  </body></html>";
    }

    // GET /reboot
    public String reboot(HashMap qparms){
        try {
            String [] setPermissiveCmd={"su","-c","reboot"};
            Runtime.getRuntime().exec(setPermissiveCmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = "{\"error\":0}";
        return json.toString();
    }

    public String start(HashMap qparms){
        try {
            if(BackgroundService.instance != null ) {
                BackgroundService.instance.startFrontActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = "{\"error\":0}";
        return json.toString();
    }

    public String run(HashMap qparms){
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
            String json = "{" +
                    "\"error\":" + exitVal + "," +
                    "\"stdout\": \"" + Tool.escapeJsonSpecial(outputGobbler.output) + "\"," +
                    "\"stderr\": \"" + Tool.escapeJsonSpecial(errorGobbler.output) + "\"," +
                    "\"cmd\": \"" + Tool.escapeJsonSpecial(cmd) + "\"" +
                    "}";
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":-1}";
        }
    }

    //implement web callback here and access them using method name
}