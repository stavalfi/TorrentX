package main;

import java.util.concurrent.atomic.AtomicInteger;

public class AppConfig {

    private static AppConfig instance = new AppConfig();

    public static AppConfig getInstance() {
        return AppConfig.instance;
    }

    private AppConfig() {
    }


    public String getPeerId() {
        return "-AZ5750-TpkXttZLfpSH";
    }

    private AtomicInteger freePort = new AtomicInteger(9191);

    public int findFreePort() {
        return this.freePort.getAndIncrement();
    }
}
