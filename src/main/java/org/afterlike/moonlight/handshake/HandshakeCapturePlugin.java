package org.afterlike.moonlight.handshake;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lunarclient.apollo.player.v1.PlayerHandshakeMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HandshakeCapturePlugin extends JavaPlugin implements PluginMessageListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void onEnable() {
        getLogger().info("HandshakeCapturePlugin is ready");
        getServer().getMessenger().registerIncomingPluginChannel(this, "lunar:apollo", this);
        File capturesDir = new File(getDataFolder(), "captures");
        if (!capturesDir.exists()) {
            capturesDir.mkdirs();
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "lunar:apollo");
        getLogger().info("HandshakeCapturePlugin disabled");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!channel.equals("lunar:apollo")) {
            return;
        }

        try {
            Any any = Any.parseFrom(data);
            String typeUrl = any.getTypeUrl();
            if (typeUrl.contains("PlayerHandshakeMessage")) {
                PlayerHandshakeMessage handshake = PlayerHandshakeMessage.parseFrom(any.getValue().toByteArray());
                captureHandshake(player.getName(), handshake);
            }
        } catch (InvalidProtocolBufferException e) {
            getLogger().warning("Failed to parse handshake from " + player.getName() + ": " + e.getMessage());
        }
    }

    private void captureHandshake(String playerName, PlayerHandshakeMessage handshake) {
        try {
            JsonObject json = ProtoUtil.toJson(handshake);

            File out = new File(
                    getDataFolder(),
                    "captures/handshake_" + playerName + "_" +
                            DATE_FORMAT.format(new Date()) + ".json"
            );

            try (FileWriter writer = new FileWriter(out)) {
                writer.write(GSON.toJson(json));
            }

        } catch (IOException e) {
            getLogger().severe("Failed to write handshake JSON: " + e.getMessage());
        }
    }
}

