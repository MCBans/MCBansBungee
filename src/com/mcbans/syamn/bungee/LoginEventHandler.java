/**
 * MCBansProxy - Package: com.mcbans.syamn.bungee
 * Created: 2013/01/26 23:24:09
 */
package com.mcbans.syamn.bungee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;

import com.google.common.eventbus.Subscribe;

/**
 * LoginEventHandler (LoginEventHandler.java)
 */
public class LoginEventHandler implements Listener{
    private static final String logPrefix = MCBansProxy.logPrefix;
    private MCBansProxy plugin;
    
    LoginEventHandler(final MCBansProxy plugin){
        this.plugin = plugin;
    }
    
    @Subscribe
    public void onLogin(final LoginEvent event){
        final PendingConnection pc = event.getConnection();
        if (event.isCancelled() || pc == null) return;
        
        if (!plugin.isValidKey){
            ProxyServer.getInstance().getLogger().log(Level.WARNING, "Missing or invalid API Key! Please check config.yml and restart proxy!");
            return;
        }
        
        try{
            final String uriStr = "http://api.mcbans.com/v2/" + plugin.apiKey + "/login/"
                    + URLEncoder.encode(pc.getName(), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(pc.getAddress().getHostName()), "UTF-8");
            final URLConnection conn = new URL(uriStr).openConnection();
            conn.setConnectTimeout(plugin.timeout * 1000);
            conn.setReadTimeout(plugin.timeout * 1000);
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
                if (plugin.failsafe){
                    ProxyServer.getInstance().getLogger().log(Level.WARNING, "Null response! Kicked player: " + pc.getName());
                    event.setCancelled(true);
                    event.setCancelReason("MCBans service unavailable!");
                }else{
                    ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "Null response! Check passed player: " + pc.getName());
                }
                return;
            }
            
            plugin.debug("Response: " + response);
            String[] s = response.split(";");
            if (s.length == 6 || s.length == 7) {
                // check banned
                if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
                    event.setCancelled(true);
                    event.setCancelReason(s[1]);
                    return;
                }
                // check reputation
                else if (plugin.minRep > Double.valueOf(s[2])) {
                    event.setCancelled(true);
                    event.setCancelReason(plugin.minRepMsg);
                    return;
                }
                // check alternate accounts
                else if (plugin.enableMaxAlts && plugin.maxAlts < Integer.valueOf(s[3])) {
                    event.setCancelled(true);
                    event.setCancelReason(plugin.maxAltsMsg);
                    return;
                }
                // check passed, put data to playerCache
                else{
                    if(s[0].equals("b")){
                        ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + pc.getName() + " has previous ban(s)!");
                    }
                    if(Integer.parseInt(s[3])>0){
                        ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + pc.getName() + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
                    }
                    if(s[4].equals("y")){
                        ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + pc.getName() + " is an MCBans.com Staff Member!");
                    }
                    if(Integer.parseInt(s[5])>0){
                        ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + s[5] + " open dispute(s)!");
                    }
                }
                plugin.debug(pc.getName() + " authenticated with " + s[2] + " rep");
            }else{
                if (response.toString().contains("Server Disabled")) {
                    ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "This Server Disabled by MCBans Administration!");
                    return;
                }
                if (plugin.failsafe){
                    ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "Null response! Kicked player: " + pc.getName());
                    event.setCancelled(true);
                    event.setCancelReason(plugin.unavailable);
                }else{
                    ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "Invalid response!(" + s.length + ") Check passed player: " + pc.getName());
                }
                $().info(logPrefix + "Response: " + response);
                return;
            }
        }catch (SocketTimeoutException ex){
            $().info(logPrefix + "Cannot connect MCBans API server: timeout");
            if (plugin.failsafe){
                event.setCancelled(true);
                event.setCancelReason(plugin.unavailable);
            }
        }catch (Exception ex){
            ProxyServer.getInstance().getLogger().log(Level.INFO, logPrefix + "Cannot connect MCBans API server!");
            if (plugin.failsafe){
                event.setCancelled(true);
                event.setCancelReason(plugin.unavailable);
            }
            if (plugin.isDebug) ex.printStackTrace();
        }
    }
    
}
