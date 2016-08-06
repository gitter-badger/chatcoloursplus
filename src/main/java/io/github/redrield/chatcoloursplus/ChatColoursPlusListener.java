package io.github.redrield.chatcoloursplus;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ChatColoursPlusListener {

    private ChatColoursPlus plugin;

    public ChatColoursPlusListener(ChatColoursPlus plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join e, @Getter("getTargetEntity") Player p) {
        try(Connection con = plugin.getDataSource().getConnection(); PreparedStatement select = con.prepareStatement("SELECT * FROM ChatColoursPlus WHERE playerId=?")) {
            select.setString(1, p.getUniqueId().toString());
            ResultSet res = select.executeQuery();
            if(!res.next()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO ChatColoursPlus VALUES(?, ?, ?, ?, ?);");
                ps.setString(1, p.getUniqueId().toString());
                ps.setString(2, "none");
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setInt(5, 0);
                ps.executeUpdate();
            }
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Listener
    public void onPlayerChat(MessageChannelEvent.Chat event, @Getter("getFormatter")MessageEvent.MessageFormatter format) {
        Optional<Player> p = event.getCause().get("Source", Player.class);

        p.ifPresent(pl -> {
            try(Connection con = plugin.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement("SELECT * FROM ChatColoursPlus WHERE PlayerId=?")) {
                ps.setString(1, pl.getUniqueId().toString());
                ResultSet res = ps.executeQuery();
                if(res.next()) {
                    TextColor color = Sponge.getRegistry().getType(TextColor.class, res.getString("colour")).orElse(TextColors.NONE);
                    TextStyle bold = res.getInt("bold") == 1 ? TextStyles.BOLD : TextStyles.NONE;
                    TextStyle italic = res.getInt("italic") == 1 ? TextStyles.ITALIC : TextStyles.NONE;
                    TextStyle magic = res.getInt("magic") == 1 ? TextStyles.OBFUSCATED : TextStyles.NONE;
                    format.setBody(Text.of(color, bold, italic, magic, event.getRawMessage()));
                }
                res.close();
            }catch(SQLException ex) {
                ex.printStackTrace();
            }
        });
    }
}
