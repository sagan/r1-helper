
package appapis.queryfiles;

import java.io.IOException;
import java.util.HashMap;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
        String json = "{\"error\":0}";
        return json.toString();
    }


    //implement web callback here and access them using method name
}