package com.assureapi.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {


    private static final Properties properties = new Properties();


    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")){
            if(input == null){
                throw new RuntimeException("config.properties não encontrado");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar config.properties", e);
        }
    }


    public static String getBaseUrl() {
        return System.getProperty("base.uri", properties.getProperty("base.uri"));
    }

}
