/**
 * BungeeCordMCBans - Package: com.mcbans.syamn.bungee
 * Created: 2012/12/28 16:20:37
 */
package com.mcbans.syamn.bungee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * MCBansProxy (MCBansProxy.java)
 */
public class MCBansProxy extends Plugin{
    public static final String logPrefix = "[MCBansProxy] ";
    private MCBansConfiguration confManager;
    
    boolean isValidKey = false;
    
    // config
    String apiKey, minRepMsg, maxAltsMsg, unavailable;
    int minRep, maxAlts, timeout;
    boolean failsafe, isDebug, enableMaxAlts;
    List<String> checkForServersOnly;
    
    public static MCBansProxy plugin;
    
    @Override
    public void onEnable(){
    	MCBansProxy.plugin = this;
    	
        confManager = new MCBansConfiguration(this);
        confManager.loadConfig();
        
        getConfigs();
        
        ProxyServer.getInstance().getPluginManager().registerListener(this,new LoginEventHandler(this));
        ProxyServer.getInstance().getLogger().info(logPrefix + "MCBansProxy plugin enabled!");
    }
    
    private void getConfigs(){
        apiKey = confManager.get("apiKey", "").trim();
        minRep = confManager.get("minRep", 3);
        minRepMsg = confManager.get("minRepMessage", "Your reputation is below this servers threshold!");
        enableMaxAlts = confManager.get("enableMaxAlts", false);
        maxAlts = confManager.get("maxAlts", 2);
        maxAltsMsg = confManager.get("maxAltsMessage", "You have more alt accounts than this server allows!");
        timeout = confManager.get("timeout", 3);
        unavailable = confManager.get("unavailableMessage", "Unavailable MCBans Service! Please try again later!");
        isDebug = confManager.get("isDebug", false);
        failsafe = confManager.get("failsafe", false);
        checkForServersOnly = confManager.get("checkForServersOnly", null);
        
        // check API key
        if (apiKey.length() != 40){
            isValidKey = false;
            ProxyServer.getInstance().getLogger().warning("Missing or invalid API Key! Please check config.yml and restart proxy!");
        }else{
            isValidKey = true;
        }
    }
    
    static void debug(final String msg){
        if (MCBansProxy.plugin.isDebug){
        	ProxyServer.getInstance().getLogger().info(logPrefix + "[DEBUG] " + msg);
        }
        
    }
    
    public static String checkMCBansForPlayer(String playername, String playerHostname) {
        boolean isValidKey = MCBansProxy.plugin.isValidKey;
    	boolean failsafe = MCBansProxy.plugin.failsafe;
    	boolean enableMaxAlts = MCBansProxy.plugin.enableMaxAlts;
    	boolean isDebug = MCBansProxy.plugin.isDebug;

    	int timeout = MCBansProxy.plugin.timeout;
    	int minRep = MCBansProxy.plugin.minRep;
    	int maxAlts = MCBansProxy.plugin.maxAlts;

        String apiKey = MCBansProxy.plugin.apiKey;
    	String minRepMsg = MCBansProxy.plugin.minRepMsg;
    	String maxAltsMsg = MCBansProxy.plugin.maxAltsMsg;
    	String unavailable = MCBansProxy.plugin.unavailable;
    	
        if (!isValidKey){
            ProxyServer.getInstance().getLogger().warning("Missing or invalid API Key! Please check config.yml and restart proxy!");
            return "Server temporarily unavailable. Contact the server administrator.";
        }
        
        try{
            final String uriStr = "http://api.mcbans.com/v2/" + apiKey + "/login/"
                    + URLEncoder.encode(playername, "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(playerHostname), "UTF-8");
            final URLConnection conn = new URL(uriStr).openConnection();
            conn.setConnectTimeout(timeout * 1000);
            conn.setReadTimeout(timeout * 1000);
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
                if (failsafe){
                    ProxyServer.getInstance().getLogger().info("Null response! Kicked player: " + playername);
                    return "MCBans service unavailable!";
                }else{
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Null response! Check passed player: " + playername);
                    return null;
                }
            }
            
            debug("Response: " + response);
            String[] s = response.split(";");
            if (s.length == 6 || s.length == 7) {
                // check banned
                if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
                    return s[1];
                }
                // check reputation
                else if (minRep > Double.valueOf(s[2])) {
                    return minRepMsg;
                }
                // check alternate accounts
                else if (enableMaxAlts && maxAlts < Integer.valueOf(s[3])) {
                    return maxAltsMsg;
                }
                // check passed, put data to playerCache
                else{
                    if(s[0].equals("b")){
                        ProxyServer.getInstance().getLogger().info(logPrefix + playername + " has previous ban(s)!");
                    }
                    if(Integer.parseInt(s[3])>0){
                        ProxyServer.getInstance().getLogger().info(logPrefix + playername + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
                    }
                    if(s[4].equals("y")){
                        ProxyServer.getInstance().getLogger().info(logPrefix + playername + " is an MCBans.com Staff Member!");
                    }
                    if(Integer.parseInt(s[5])>0){
                        ProxyServer.getInstance().getLogger().info(logPrefix + s[5] + " open dispute(s)!");
                    }
                }
                debug(playername + " authenticated with " + s[2] + " rep");
            }else{
                if (response.toString().contains("Server Disabled")) {
                    ProxyServer.getInstance().getLogger().info(logPrefix + "This Server was disabled by MCBans Administration!");
                    return null;
                }
                if (failsafe){
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Null response! Kicked player: " + playername);
                    return unavailable;
                }else{
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Invalid response!(" + s.length + ") Check passed player: " + playername);
                }
                ProxyServer.getInstance().getLogger().info(logPrefix + "Response: " + response);
                return null;
            }
        }catch (SocketTimeoutException ex){
            ProxyServer.getInstance().getLogger().info(logPrefix + "Cannot connect MCBans API server: timeout");
            if (failsafe){
                return unavailable;
            }
        }catch (Exception ex){
            ProxyServer.getInstance().getLogger().info(logPrefix + "Cannot connect MCBans API server!");
            if (failsafe){
                return unavailable;
            }
            if (isDebug) ex.printStackTrace();
        }
        
        return null;
    }
}
