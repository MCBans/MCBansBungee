/**
 * BungeeCordMCBans - Package: com.mcbans.syamn.bungee
 * Created: 2012/12/28 16:20:37
 */
package com.mcbans.syamn.bungee;

import static net.md_5.bungee.Logger.$;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.md_5.bungee.plugin.JavaPlugin;
import net.md_5.bungee.plugin.LoginEvent;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * MCBansProxy (MCBansProxy.java)
 */
public class MCBansProxy extends JavaPlugin{
    private static final String logPrefix = "[MCBansProxy] ";
    
    // config
    private String apiKey;
    private boolean isDebug;
    private int minRep;
    private int timeout;
    private boolean failsafe;
    
    
    @Override
    public void onEnable(){
        loadConfig();
        $().info(logPrefix + "MCBansProxy plugin enabled!");
    }
    
    private void loadConfig(){
        try{
            File dir = new File("plugins", "MCBansProxy");
            dir.mkdir();
            File file = new File(dir, "config.yml");
            if (!file.exists()){
                createConfig(file);
            }
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            Map<String, Object> config;
            try (InputStream is = new FileInputStream(file)){
                config = (Map) yaml.load(is);
            }
            if (config == null){
                throw new IllegalStateException("null config map!");
            }
            
            apiKey = config.get("apiKey").toString();
            isDebug = "true".equals(config.get("isDebug").toString().toLowerCase(Locale.ENGLISH));
            minRep = Integer.parseInt(config.get("minRep").toString());
            timeout = Integer.parseInt(config.get("timeout").toString());
            failsafe = "true".equals(config.get("failsafe").toString().toLowerCase(Locale.ENGLISH));
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void createConfig(final File file){
        try{
            Map def = new LinkedHashMap<String, Object>();
            def.put("apiKey", "");
            def.put("isDebug", "false");
            def.put("minRep", "3");
            def.put("timeout", "10");
            def.put("failsafe", "false");
            
            file.createNewFile();
            try (FileWriter wr = new FileWriter(file)){
                (new Yaml()).dump(def, wr);
            }
            $().info(logPrefix + "config.yml not found! Created default config.yml!");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void debug(final String msg){
        if (isDebug){
            $().info(logPrefix + "[DEBUG] " + msg);
        }
    }
    
    @Override
    public void onLogin(final LoginEvent event){
        if (event.isCancelled()) return;
        
        try{
            final String uriStr = "http://api.mcbans.com/v2/" + apiKey + "/login/"
                    + URLEncoder.encode(event.getUsername(), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(event.getAddress().getHostAddress()), "UTF-8");
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
                    $().info("Null response! Kicked player: " + event.getUsername());
                    event.setCancelled(true);
                    event.setCancelReason("MCBans service unavailable!");
                }else{
                    $().info(logPrefix + "Null response! Check passed player: " + event.getUsername());
                }
                return;
            }
            
            debug("Response: " + response);
            String[] s = response.split(";");
            if (s.length == 6 || s.length == 7) {
                // check banned
                if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
                    event.setCancelled(true);
                    event.setCancelReason(s[1]);
                    return;
                }
                // check reputation
                else if (minRep > Double.valueOf(s[2])) {
                    event.setCancelled(true);
                    event.setCancelReason("Too Low Rep!");
                    return;
                }
                // check alternate accounts
                else if (false && 5 < Integer.valueOf(s[3])) {// TODO config.isEnableMaxAlts() && config.getMaxAlts() < Integer.valueOf(s[3])
                    event.setCancelled(true);
                    event.setCancelReason("Too Many Alt Accounts!");
                    return;
                }
                // check passed, put data to playerCache
                else{
                    if(s[0].equals("b")){
                        $().info(logPrefix + event.getUsername() + " has previous ban(s)!");
                    }
                    if(Integer.parseInt(s[3])>0){
                        $().info(logPrefix + event.getUsername() + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
                    }
                    if(s[4].equals("y")){
                        $().info(logPrefix + event.getUsername() + " is an MCBans.com Staff Member!");
                    }
                    if(Integer.parseInt(s[5])>0){
                        $().info(logPrefix + s[5] + " open dispute(s)!");
                    }
                }
                debug(event.getUsername() + " authenticated with " + s[2] + " rep");
            }else{
                if (response.toString().contains("Server Disabled")) {
                    $().info(logPrefix + "This Server Disabled by MCBans Administration!");
                    return;
                }
                if (failsafe){
                    $().info(logPrefix + "Null response! Kicked player: " + event.getUsername());
                    event.setCancelled(true);
                    event.setCancelReason("MCBans service unavailable!");
                }else{
                    $().info(logPrefix + "Invalid response!(" + s.length + ") Check passed player: " + event.getUsername());
                }
                $().info(logPrefix + "Response: " + response);
                return;
            }
        }catch (SocketTimeoutException ex){
            $().info(logPrefix + "Cannot connect MCBans API server: timeout");
            if (failsafe){
                event.setCancelled(true);
                event.setCancelReason("MCBans service unavailable!");
            }
        }catch (Exception ex){
            $().info(logPrefix + "Cannot connect MCBans API server!");
            if (failsafe){
                event.setCancelled(true);
                event.setCancelReason("MCBans service unavailable!");
            }
            if (isDebug) ex.printStackTrace();
        }
    }
}
