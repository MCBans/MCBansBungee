/**
 * MCBansProxy - Package: com.mcbans.syamn.bungee
 * Created: 2013/01/26 23:24:09
 */
package com.mcbans.syamn.bungee;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;

import com.google.common.eventbus.Subscribe;

/**
 * LoginEventHandler (LoginEventHandler.java)
 */
public class LoginEventHandler implements Listener{
    private MCBansProxy plugin;
    
    LoginEventHandler(final MCBansProxy plugin){
        this.plugin = plugin;
    }
    
    @Subscribe
    public void onServerConnect(ServerConnectEvent event) {
    	System.out.println("ServerConnectEvent: " + event.getPlayer().getName() + " -> " + event.getTarget().getName());
    	if (plugin.checkForServersOnly == null) return;
    	
    	String remoteServerName = event.getTarget().getName();
    	if (plugin.checkForServersOnly.contains(remoteServerName)) {
        	System.out.println("ServerConnectEvent: needs check");
        	String playerName = event.getPlayer().getName();
        	String playerAddress = event.getPlayer().getAddress().getHostName();
        	
            String cancelReason = plugin.checkMCBansForPlayer(playerName, playerAddress);
        	System.out.println("ServerConnectEvent - cancelreason: " + cancelReason);
            if (cancelReason != null) {
            	event.getPlayer().disconnect(cancelReason);
            }
    	}
    }
    
    @Subscribe
    public void onLogin(final LoginEvent event){
    	if (plugin.checkForServersOnly != null) return;

        final PendingConnection pc = event.getConnection();
        if (event.isCancelled() || pc == null) return;

    	System.out.println("LoginEvent: " + pc.getName());

        String cancelReason = plugin.checkMCBansForPlayer(pc.getName(), pc.getAddress().getHostName());
    	System.out.println("LoginEvent - cancelreason: " + cancelReason);
        if (cancelReason != null) {
            event.setCancelled(true);
            event.setCancelReason(cancelReason);
        }
    }
    
}
