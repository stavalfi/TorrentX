package main;

import main.downloader.TorrentDownloaders;

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
}
