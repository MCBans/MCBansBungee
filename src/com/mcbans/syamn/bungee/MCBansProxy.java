/**
 * BungeeCordMCBans - Package: com.mcbans.syamn.bungee
 * Created: 2012/12/28 16:20:37
 */
package com.mcbans.syamn.bungee;

import java.util.logging.Level;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * MCBansProxy (MCBansProxy.java)
 */
public class MCBansProxy extends Plugin{
    public static final String logPrefix = "[MCBansProxy] ";
    private MCBansProxy plugin;
    private MCBansConfiguration confManager;
    
    boolean isValidKey = false;
    
    // config
    String apiKey, minRepMsg, maxAltsMsg, unavailable;
    int minRep, maxAlts, timeout;
    boolean failsafe, isDebug, enableMaxAlts;
    
    public void onEnable(){
        confManager = new MCBansConfiguration(this);
        confManager.loadConfig();
        getConfigs();
        registerLoginListner();
        ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "MCBansProxy plugin enabled!");
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
        
        // check API key
        if (apiKey.length() != 40){
            isValidKey = false;
            ProxyServer.getInstance().getLogger().log(Level.WARNING, "Missing or invalid API Key! Please check config.yml and restart proxy!");
        }else{
            isValidKey = true;
        }
    }
    
    void debug(final String msg){
        if (isDebug){
            ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "[DEBUG] " + msg);
        }
        
    }
    
    public void registerLoginListner(){
    	ProxyServer.getInstance().getPluginManager().registerListener(this.plugin, new LoginEventHandler(this));
    }
}
