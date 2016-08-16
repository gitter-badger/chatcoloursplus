package io.github.redrield.chatcoloursplus;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.Text;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
        id = "chatcoloursplus",
        name = "ChatColoursPlus",
        version = "1.0-SNAPSHOT"
)
public class ChatColoursPlus {

    @Inject
    private Logger logger;

    //Plugin instance
    @Inject
    private PluginContainer plugin;

    //Generating configuration files
    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    private CommentedConfigurationNode config;

    private DataSource dataSource;

    @Inject
    public Logger getLogger() {
        return logger;
    }


    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        Sponge.getAssetManager().getAsset(plugin, "chatcoloursplus.conf").ifPresent(obj -> {
            try {
               if(Files.notExists(defaultConfig, LinkOption.NOFOLLOW_LINKS)) {
                   obj.copyToFile(defaultConfig);
               }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            config = loader.load();
        }catch(IOException e) {
            e.printStackTrace();
        }

        CommandSpec ccp = CommandSpec.builder().description(Text.of("Main command for ChatColoursPlus plugin"))
                .arguments(GenericArguments.optional(GenericArguments.firstParsing(
                        GenericArguments.seq(GenericArguments.player(Text.of("target")), GenericArguments.string(Text.of("colour"))),
                        GenericArguments.string(Text.of("reload")))))
                .executor(new ChatColoursPlusExecutor(this)).build();
        Sponge.getCommandManager().register(plugin, ccp, "ccp");
        Sponge.getEventManager().registerListeners(plugin, new ChatColoursPlusListener(this));
        setupDatabase();
    }

    public void setupDatabase() {
        SqlService sql = Sponge.getServiceManager().provide(SqlService.class).get();

        switch(config.getNode("databaseType").getString().toLowerCase()) {
            case "mysql":
                String ip = config.getNode("mysql", "ip").getString();
                int port = config.getNode("mysql", "port").getInt();
                String database = config.getNode("mysql", "database").getString();
                String username = config.getNode("mysql", "username").getString();
                String password = config.getNode("mysql", "password").getString();
                try {
                    dataSource = sql.getDataSource("jdbc:mysql://" + ip + ":" + port + "/" + database + "?user=" + username + "&password=" + password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            default:
            case "h2":
                String name = config.getNode("h2", "fileName").getString();
                try {
                    dataSource = sql.getDataSource("jdbc:h2:" + "./config/chatcoloursplus/" + name);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
        }

        try {
            dataSource.getConnection().createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS ChatColoursPlus(playerId CHAR(36), colour TEXT, bold TINYINT(1), italic TINYINT(1), magic TINYINT(1));");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    public ConfigurationLoader<CommentedConfigurationNode> getLoader() {
        return loader;
    }

    public void setConfig(CommentedConfigurationNode config) {
        this.config = config;
    }

    public CommentedConfigurationNode getConfig() {
        return config;
    }
}