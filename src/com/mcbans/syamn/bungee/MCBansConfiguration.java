/**
 * MCBansProxy - Package: com.mcbans.syamn.bungee
 * Created: 2012/12/28 18:13:25
 */
package com.mcbans.syamn.bungee;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import net.md_5.bungee.api.plugin.Plugin;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import static net.md_5.bungee.Logger.$;
//import net.md_5.bungee.plugin.JavaPlugin;

/**
 * MCBansConfiguration (MCBansConfiguration.java)
 */
public class MCBansConfiguration {
    private static final String logPrefix = MCBansProxy.logPrefix;
    private Plugin plugin;
    Map<String, Object> config;
    
    MCBansConfiguration(final Plugin plugin){
        this.plugin = plugin;
    }
    
    @SuppressWarnings("unchecked")
    void loadConfig(){
        try{
            File dir = new File("plugins", "MCBansProxy");
            dir.mkdir();
            File file = new File(dir, "config.yml");
            if (!file.exists()){
                extractResource("/config.yml", dir, false);
                $().info(logPrefix + "config.yml not found! Created default config.yml!");
            }
            
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            
            try (InputStream is = new FileInputStream(file)){
                config = (Map<String, Object>) yaml.load(is);
            }
            if (config == null){
                extractResource("/config.yml", dir, true);
                loadConfig();
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    <T> T get(String path, T def){
        if (!config.containsKey(path)){
            config.put(path, def);
        }
        return (T) config.get(path);
    }
    
    private void extractResource(String from, File to, boolean force){
        File of = to;
        
        // If to path is directory, cas to File. return if not file or directory
        if (to.isDirectory()){
            String fileName = new File(from).getName();
            of = new File(to, fileName);
        } else if (!of.isFile()) {
            $().warning(logPrefix + "Not a file: " + of);
            return;
        }
        
        // If file exist, check force flag
        if (of.exists() && !force){
            return;
        }
        
        OutputStream out = null;
        InputStream in = null;
        InputStreamReader reader = null;
        OutputStreamWriter writer = null;
        DataInputStream dis = null;
        
        try {
            // get inside jar resource uri
            URL res = plugin.getClass().getResource(from);
            if (res == null){
                $().warning(logPrefix + "Can't find " + from + " in plugin Jar file");
                return;
            }
            URLConnection resConn = res.openConnection();
            resConn.setUseCaches(false);
            
            // input resource
            in = resConn.getInputStream();
            if (in == null){
                $().warning(logPrefix + "Can't get input stream from " + res);
                return;
            }
            
            // output resource
            out = new FileOutputStream(of);
            byte[] buf = new byte[1024]; // buffer size
            int len = 0;
            while ((len = in.read(buf)) >= 0){
                out.write(buf, 0, len);
            }
            /* // other way
            reader = new InputStreamReader(in, "UTF-8");
            writer = new OutputStreamWriter(new FileOutputStream(of)); // not specify output encode
            int text;
            while ((text = reader.read()) != -1){
                writer.write(text);
            }
            */
        }catch (Exception ex){
            ex.printStackTrace();
        }finally{
            // close stream
            try{
                if (out != null) out.close();
            }catch (Exception ignored){}
            try{
                if (in != null) in.close();
            }catch (Exception ignored){}
            try {
                if (reader != null) reader.close();
            }catch (Exception ignored){}
            try{
                if (writer != null) writer.close();
            }catch (Exception ignored){}
        }
    }
}
