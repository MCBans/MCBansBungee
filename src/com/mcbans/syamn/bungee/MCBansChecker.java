package com.mcbans.syamn.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MCBansChecker extends Thread {
	
	private String checkPlayerName;
	private String checkHostName;
	
	MCBansChecker(String checkPlayerName, String checkHostName) {
		this.checkPlayerName = checkPlayerName;
		this.checkHostName = checkHostName;
	}

	@Override
	public void run() {
		if (checkPlayerName == null || checkHostName == null) {
			System.out.println("MCBansChecker needs to be innitialized with a playername and hostname");
			return;
		}
		
        String cancelReason = MCBansProxy.checkMCBansForPlayer(checkPlayerName, checkPlayerName);
    	System.out.println("ServerConnectEvent - cancelreason: " + cancelReason);
        if (cancelReason != null) {
        	ProxiedPlayer player = ProxyServer.getInstance().getPlayer(checkPlayerName);
        	if (player != null) {
        		player.disconnect(cancelReason);
        	}
        	
        }
		
	}
	
}