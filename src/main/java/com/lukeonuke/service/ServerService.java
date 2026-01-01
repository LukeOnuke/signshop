package com.lukeonuke.service;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;

public class ServerService {
    private ServerService(){

    }
    private static ServerService instance = null;

    public static ServerService getInstance(){
        if(instance == null) instance = new ServerService();
        return instance;
    }

    @Getter
    @Setter
    private MinecraftServer server;
}
