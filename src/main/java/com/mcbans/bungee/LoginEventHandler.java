/**
 * MCBansProxy - Package: com.mcbans.syamn.bungee
 * Created: 2013/01/26 23:24:09
 */
package com.mcbans.bungee;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;

import com.mcbans.client.BanStatusClient;
import com.mcbans.client.Client;
import com.mcbans.client.ConnectionPool;
import com.mcbans.client.response.BanResponse;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.event.EventHandler;

/**
 * LoginEventHandler (LoginEventHandler.java)
 */
public class LoginEventHandler implements Listener {
  private static final String logPrefix = MCBansProxy.logPrefix;
  private MCBansProxy plugin;
  public final String apiRequestSuffix = "4.3.4";

  LoginEventHandler(final MCBansProxy plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onLogin(final LoginEvent event) {
    final PendingConnection pc = event.getConnection();
    if (event.isCancelled() || pc == null) return;

    if (!plugin.isValidKey) {
      ProxyServer.getInstance().getLogger().warning("Missing or invalid API Key! Please check config.yml and restart proxy!");
      return;
    }

    try {

      Client client = ConnectionPool.getConnection(plugin.apiKey);
      String ip = pc.getAddress().getAddress().toString().replaceAll("/", "");
      BanResponse banResponse = BanStatusClient.cast(client).banStatusByPlayerUUID(
        pc.getUniqueId().toString().toLowerCase().replaceAll("-", ""),
        ip,
        true
      );
      ConnectionPool.release(client);

      if (banResponse.getBan() != null) {
        event.setCancelled(true);
        event.setCancelReason(new TextComponent(ChatColor.YELLOW + "Reason: " + ChatColor.RED + banResponse.getBan().getReason() + "\n" + ChatColor.YELLOW + "Ban from: " + ChatColor.WHITE + ((banResponse.getBan().getAdmin() != null) ? banResponse.getBan().getAdmin().getName() : "") + "\n" + ChatColor.YELLOW + "Ban Type: " + ((banResponse.getBan().getType().equalsIgnoreCase("global")) ? ChatColor.GOLD : ChatColor.GRAY) + banResponse.getBan().getType() + "\n" + ChatColor.AQUA + "http://mcbans.com/ban/" + banResponse.getBan().getId()));
        return;
      }

      if (plugin.minRep > Double.valueOf(banResponse.getReputation())) {
        event.setCancelled(true);
        event.setCancelReason(new TextComponent(plugin.minRepMsg));
        return;
      }
      /*if(Integer.parseInt(s[3]) > 0){
        if (plugin.enableMaxAlts && plugin.maxAlts < Integer.valueOf(s[3])) {
              event.setCancelled(true);
              event.setCancelReason(plugin.maxAltsMsg);
              return;
          }
      }*/
      if (banResponse.getBans() != null && banResponse.getBans().size() > 0) {
        ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " has previous ban(s)!");
      }
      /*if(Integer.parseInt(s[3])>0){
          ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
      }*/
      if (banResponse.isMCBansStaff()) {
        ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " is an MCBans.com Staff Member!");
      }
      /*if(Integer.parseInt(s[5])>0){
          ProxyServer.getInstance().getLogger().info(logPrefix + s[5] + " open dispute(s)!");
      }*/
      plugin.debug(pc.getName() + " authenticated with " + banResponse.getReputation() + " rep");
    } catch (Exception ex) {
      ex.printStackTrace();
      ProxyServer.getInstance().getLogger().info(logPrefix + "Cannot connect MCBans API server: timeout");
      if (plugin.failsafe) {
        event.setCancelled(true);
        event.setCancelReason(new TextComponent(plugin.unavailable));
      }
    }
  }
}
