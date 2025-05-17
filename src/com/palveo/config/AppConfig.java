package com.palveo.config;

import java.io.File;

public class AppConfig {

    public static final String DB_URL = "jdbc:mysql://localhost:3306/palveo_db?serverTimezone=UTC";
    public static final String DB_USER = "palveo_user";
    public static final String DB_PASSWORD = "P@lve0";

    public static final String DEFAULT_AVATAR_PATH = "/images/default_avatar.png";
    public static final String DEFAULT_EVENT_IMAGE_PATH = "/images/default_event_image.png";
    
    public static final String USER_DATA_BASE_DIR = System.getProperty("user.home") + File.separator + ".palveo_app_data";
    public static final String USER_AVATARS_SUBDIR = "avatars";


    public static final String APP_NAME = "Palveo";
    public static final String APP_VERSION = "1.0.0 - Final Code Submission";

    private AppConfig() {}

    public static String getUserAvatarsDir() {
        return USER_DATA_BASE_DIR + File.separator + USER_AVATARS_SUBDIR;
    }
    
}