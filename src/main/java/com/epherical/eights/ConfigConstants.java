package com.epherical.eights;

public class ConfigConstants {

    private static ConfigConstants INSTANCE = new ConfigConstants();


    public ConfigConstants() {
        // On forge, the constructor is never called, so we use the default INSTANCE
        // but for fabric, we load a config and assign the values to a new configConstants.
        INSTANCE = this;
    }

    private String _comment = "Check the github for more information";
    public boolean useSaveThread = true;
    public double providedMoneyOnFirstLogin = 0.0D;

    // Storage Settings
    public String storageType = "JSON"; // JSON or SQL
    public String databaseUrl = "jdbc:mysql://localhost:3306/minecraft";
    public String databaseUser = "root";
    public String databasePassword = "password";


    public static ConfigConstants getInstance() {
        return INSTANCE;
    }

}