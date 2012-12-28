/**
 * BungeeCordMCBans - Package: com.mcbans.syamn.bungee
 * Created: 2012/12/28 16:20:37
 */
package com.mcbans.syamn.bungee;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;

import net.md_5.bungee.plugin.JavaPlugin;
import net.md_5.bungee.plugin.LoginEvent;

/**
 * MCBansProxy (MCBansProxy.java)
 */
public class MCBansProxy extends JavaPlugin{
    @Override
    public void onEnable(){
        System.out.println("MCBansProxy plugin enabled!");
        //File dir = new File("MCBansProxy");
    }
    
    @Override
    public void onLogin(final LoginEvent event){
        if (event.isCancelled()) return;
        
        try{
            /*final String uriStr = "http://" + plugin.apiServer + "/v2/" + config.getApiKey() + "/login/"
                    + URLEncoder.encode(event.getName(), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(event.getAddress().getHostAddress()), "UTF-8");
                    */
            final String uriStr = "http://api.mcbans.com/v2/your_api_key_here/login/"
                    + URLEncoder.encode(event.getUsername(), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(event.getAddress().getHostAddress()), "UTF-8");
            final URLConnection conn = new URL(uriStr).openConnection();
            conn.setConnectTimeout(10 * 1000); //config.getTimeoutInSec() * 1000
            conn.setReadTimeout(10 * 1000); //config.getTimeoutInSec() * 1000
            conn.setUseCaches(false);
            
            BufferedReader br = null;
            String response = null;
            try{
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                response = br.readLine();
            }finally{
                if (br != null) br.close();
            }
            if (response == null){
                System.out.println("Null response! Check passed player: " + event.getUsername());
                return;
            }
            
            String[] s = response.split(";");
            if (s.length == 6 || s.length == 7) {
                // check banned
                if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
                    event.setCancelled(true);
                    event.setCancelReason(s[1]);
                    return;
                }
                // check reputation
                else if (3 > Double.valueOf(s[2])) { //config.getMinRep()
                    event.setCancelled(true);
                    event.setCancelReason("Too Low Rep!");
                    return;
                }
                // check alternate accounts
                else if (5 < Integer.valueOf(s[3])) {// config.isEnableMaxAlts() && config.getMaxAlts() < Integer.valueOf(s[3])
                    event.setCancelled(true);
                    event.setCancelReason("Too Many Alt Accounts!");
                    return;
                }
                // check passed, put data to playerCache
                else{
                    HashMap<String, String> tmp = new HashMap<String, String>();
                    if(s[0].equals("b")){
                        System.out.println(event.getUsername() + " has previous ban(s)!");
                    }
                    if(Integer.parseInt(s[3])>0){
                        System.out.println(event.getUsername() + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
                    }
                    if(s[4].equals("y")){
                        System.out.println(event.getUsername() + " is an MCBans.com Staff Member!");
                    }
                    if(Integer.parseInt(s[5])>0){
                        System.out.println(s[5] + " open dispute(s)!");
                    }
                }
                System.out.println(event.getUsername() + " authenticated with " + s[2] + " rep");
            }else{
                if (response.toString().contains("Server Disabled")) {
                    System.out.println("This Server Disabled by MCBans Administration!");
                    return;
                }
                System.out.println("Invalid response!(" + s.length + ") Check passed player: " + event.getUsername());
                System.out.println("Response: " + response);
                return;
            }
            
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
