package io.github.redrield.chatcoloursplus;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ChatColoursPlusExecutor implements CommandExecutor {

    private ChatColoursPlus plugin;

    public ChatColoursPlusExecutor(ChatColoursPlus plugin) {
        this.plugin = plugin;
    }

        @NonnullByDefault
        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

            if(args.<String>getOne("reload").isPresent()) {
                if(args.<String>getOne("reload").get().equalsIgnoreCase("reload")) {
                    if(src.hasPermission("ccp.reload")) {
                        try {
                            plugin.setConfig(plugin.getLoader().load());
                            plugin.setupDatabase();
                        }catch(IOException e) {
                            e.printStackTrace();
                        }
                        src.sendMessage(Text.of(TextColors.GREEN, "The config has been reloaded!"));
                    }else {
                        src.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
                    }
                    return CommandResult.success();
                }else {
                    throw new CommandException(Text.of(TextColors.RED, "Unknown usage of command!"));
                }
            }

            Optional<Player> targetOpt = args.getOne("target");
            if(targetOpt.isPresent()) {
                Player target = targetOpt.get();
                String colour = args.<String>getOne("colour").orElseThrow(() -> new CommandException(Text.of(TextColors.RED, "You need to supply a colour!")));
                if (target.hasPermission("ccp.override")) {
                    throw new CommandException(Text.of(TextColors.RED, "You cannot change the chat colour of that player!"));
                }
                if (!src.hasPermission("ccp.colour." + colour.toLowerCase()) && !src.hasPermission("ccp.colour.*")) {
                    throw new CommandException(Text.of(TextColors.RED, "You do not have permission to use this colour"));
                }
                if(colour.equalsIgnoreCase("none") || colour.equalsIgnoreCase("reset")) {
                    try(Connection con = plugin.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE ChatColoursPlus SET colour=?, bold=?, italic=?, magic=? WHERE PlayerId=?")) {
                        ps.setString(1, "none");
                        ps.setInt(2, 0);
                        ps.setInt(3, 0);
                        ps.setInt(4, 0);
                        ps.setString(5, target.getUniqueId().toString());
                        ps.executeUpdate();
                        src.sendMessage(Text.of(TextColors.GREEN, "That player's colour has been changed!"));
                        return CommandResult.success();
                    }catch(SQLException e) {
                        e.printStackTrace();
                    }
                }
                Optional<TextColor> colourOpt = Sponge.getRegistry().getType(TextColor.class, colour);
                Optional<TextStyle.Base> styleOpt = Sponge.getRegistry().getType(TextStyle.Base.class, colour);
                if(!colourOpt.isPresent()) {
                    TextStyle.Base style = styleOpt.orElseThrow(() -> new CommandException(Text.of(TextColors.RED, "You need to input a valid colour")));
                    try(Connection con = plugin.getDataSource().getConnection(); PreparedStatement select = con.prepareStatement("SELECT * FROM ChatColoursPlus WHERE playerId=?")) {
                        select.setString(1, target.getUniqueId().toString());
                        ResultSet res = select.executeQuery();
                        if(res.next()) {
                            PreparedStatement insert = con.prepareStatement("UPDATE ChatColoursPlus SET bold=?, italic=?, magic=? WHERE playerId=?");
                            switch(style.getName().toLowerCase()) {
                                case "bold":
                                    insert.setInt(1, res.getInt("bold") == 1 ? 0 : 1);
                                    insert.setInt(2, res.getInt("italic"));
                                    insert.setInt(3, res.getInt("magic"));
                                    break;
                                case "italic":
                                    insert.setInt(1, res.getInt("bold"));
                                    insert.setInt(2, res.getInt("italic") == 1 ? 0 : 1);
                                    insert.setInt(3, res.getInt("magic"));
                                    break;
                                case "obfuscated":
                                    insert.setInt(1, res.getInt("bold"));
                                    insert.setInt(2, res.getInt("italic"));
                                    insert.setInt(3, res.getInt("magic") == 1 ? 0 : 1);
                                    break;
                            }
                            insert.setString(4, target.getUniqueId().toString());
                            insert.executeUpdate();
                            insert.close();
                        }
                        res.close();
                    }catch(SQLException e) {
                        e.printStackTrace();
                    }
                }else {
                    try(Connection con = plugin.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE ChatColoursPlus SET colour=? WHERE playerId=?")) {
                        ps.setString(1, colourOpt.get().getName());
                        ps.setString(2, target.getUniqueId().toString());
                        ps.executeUpdate();
                    }catch(SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                src.sendMessage(Text.of(TextColors.GREEN, target.getName() + "'s chat colour has been changed!"));
            }
            return CommandResult.success();
        }
}
