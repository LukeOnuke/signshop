package com.lukeonuke.service;

import com.lukeonuke.SignShop;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationService {
    private final Properties properties = new Properties();
    private final File configurationFile;

    private static ConfigurationService instance = null;
    private ConfigurationService() {
        configurationFile = FabricLoader.getInstance().getConfigDir().resolve("signshop").resolve("config.properties").toFile();

        if(configurationFile.exists()){
            try(FileReader reader = new FileReader(configurationFile)){
                properties.load(reader);
            }catch (IOException e){
                throw new RuntimeException("Cant read properties!");
            }
        }else{
            SignShop.LOGGER.info("          Generating config dir and file!");
            properties.setProperty(ConfigurationKey.DB_USER, "user");
            properties.setProperty(ConfigurationKey.DB_PASSWORD, "password");
            properties.setProperty(ConfigurationKey.DB_URL, "jdbc:mysql://localhost:3306/signshop");
            properties.setProperty(ConfigurationKey.PREFIX, "§2§lsignshop§r >> ");
            configurationFile.getParentFile().mkdirs();
            try(FileWriter fw = new FileWriter(configurationFile)) {
                properties.store(fw, "SignShop configuration, check out documentation at https://github.com/lukeonuke/signshop \nThis program requires a database connection!");
            }catch (IOException e){
                throw new RuntimeException("Can't write default properties!");
            }
        }
    }

    public static ConfigurationService getInstance(){
        if (instance == null) instance = new ConfigurationService();
        return instance;
    }

    public String getDBUser(){
        return properties.getProperty(ConfigurationKey.DB_USER);
    }

    public String getDBPassword(){
        return properties.getProperty(ConfigurationKey.DB_PASSWORD);
    }

    public String getDBUrl(){
        return properties.getProperty(ConfigurationKey.DB_URL);
    }

    public String getPrefix(){
        return properties.getProperty(ConfigurationKey.PREFIX);
    }

    public MutableText getPrefixAsText(){
        return Text.literal(getPrefix());
    }

    public static class ConfigurationKey{
        public static final String DB_USER = "database.user";
        public static final String DB_PASSWORD = "database.password";
        public static final String DB_URL = "database.url";
        public static final String PREFIX = "prefix";
    }
}
