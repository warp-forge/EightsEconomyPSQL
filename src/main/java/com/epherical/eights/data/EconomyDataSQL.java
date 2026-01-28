package com.epherical.eights.data;

import com.epherical.eights.ConfigConstants;
import com.epherical.eights.EightsEconomyProvider;
import com.epherical.eights.exception.EconomyException;
import com.epherical.eights.user.NPCUser;
import com.epherical.eights.user.PlayerUser;
import com.epherical.octoecon.api.Currency;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyDataSQL extends EconomyData {

    private final HikariDataSource dataSource;
    private final EightsEconomyProvider provider;

    public EconomyDataSQL(EightsEconomyProvider provider) {
        super(provider);
        this.provider = provider;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ConfigConstants.getInstance().databaseUrl);
        config.setUsername(ConfigConstants.getInstance().databaseUser);
        config.setPassword(ConfigConstants.getInstance().databasePassword);
        config.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        initTables();
    }

    private void initTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS eights_economy_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance DOUBLE NOT NULL DEFAULT 0.0" +
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        super.close();
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public PlayerUser loadUser(UUID uuid) throws IOException {
        Currency defaultCurrency = provider.getDefaultCurrency();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT balance FROM eights_economy_users WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double balance = resultSet.getDouble("balance");
                    Map<Currency, Double> balances = new HashMap<>();
                    balances.put(defaultCurrency, balance);
                    return new PlayerUser(uuid, uuid.toString(), balances);
                }
            }
        } catch (SQLException e) {
            throw new IOException("Failed to load user from SQL: " + e.getMessage(), e);
        }
        
        Map<Currency, Double> balances = new HashMap<>();
        balances.put(defaultCurrency, ConfigConstants.getInstance().providedMoneyOnFirstLogin);
        return new PlayerUser(uuid, uuid.toString(), balances);
    }

    @Override
    public NPCUser loadUser(ResourceLocation name) throws IOException {
        Currency defaultCurrency = provider.getDefaultCurrency();
        Map<Currency, Double> balances = new HashMap<>();
        balances.put(defaultCurrency, 0.0);
        return new NPCUser(name, balances);
    }

    @Override
    public boolean userExists(ResourceLocation name) throws EconomyException {
        return false;
    }

    @Override
    public boolean userExists(UUID uuid) throws EconomyException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM eights_economy_users WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new EconomyException("Database error: " + e.getMessage());
        }
    }

    @Override
    public boolean userExists(String name, boolean player) throws EconomyException {
        return false;
    }

    @Override
    public boolean saveUser(PlayerUser user, boolean setBalance) throws EconomyException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO eights_economy_users (uuid, balance) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE balance = ?")) {
            statement.setString(1, user.getUserID().toString());
            // Save only default currency for now
            Currency defaultCurrency = provider.getDefaultCurrency();
            double balance = user.getBalance(defaultCurrency);
            
            statement.setDouble(2, balance);
            statement.setDouble(3, balance);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new EconomyException("Failed to save user to SQL: " + e.getMessage());
        }
    }

    @Override
    public boolean saveUser(NPCUser user, boolean setBalance) throws EconomyException {
        return true;
    }
}