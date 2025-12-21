package org.afterlike.moonlight.handshake;

import com.google.gson.*;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lunarclient.apollo.player.v1.EmbeddedCheckoutSupport;
import com.lunarclient.apollo.player.v1.ModMessage;
import com.lunarclient.apollo.player.v1.PlayerHandshakeMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class HandshakeCapturePlugin extends JavaPlugin implements PluginMessageListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void onEnable() {
        getLogger().info("HandshakeCapturePlugin enabled - Ready to capture Lunar Client handshakes!");
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
            if (typeUrl.contains("PlayerHandshakeMessage") ||
                    typeUrl.endsWith("player.v1.PlayerHandshakeMessage") ||
                    typeUrl.contains("/com.lunarclient.apollo.player.v1.PlayerHandshakeMessage")) {
                PlayerHandshakeMessage handshake = PlayerHandshakeMessage.parseFrom(any.getValue().toByteArray());
                captureHandshake(player, handshake);
            }
        } catch (InvalidProtocolBufferException e) {
            getLogger().warning("Failed to parse Any message from " + player.getName() + ": " + e.getMessage());
        } catch (Exception e) {
            getLogger().warning("Unexpected error processing message from " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureHandshake(Player player, PlayerHandshakeMessage handshake) {
        getLogger().info("Captured handshake from: " + player.getName());
        JsonObject capture = new JsonObject();
        capture.addProperty("player", player.getName());
        capture.addProperty("player_uuid", player.getUniqueId().toString());
        capture.addProperty("timestamp", System.currentTimeMillis());
        capture.addProperty("date", DATE_FORMAT.format(new Date()));
        JsonObject mcVersion = new JsonObject();
        mcVersion.addProperty("enum", handshake.getMinecraftVersion().getEnum());
        capture.add("minecraft_version", mcVersion);
        JsonObject lcVersion = new JsonObject();
        lcVersion.addProperty("git_branch", handshake.getLunarClientVersion().getGitBranch());
        lcVersion.addProperty("git_commit", handshake.getLunarClientVersion().getGitCommit());
        lcVersion.addProperty("semver", handshake.getLunarClientVersion().getSemver());
        capture.add("lunar_client_version", lcVersion);
        EmbeddedCheckoutSupport checkoutSupport = handshake.getEmbeddedCheckoutSupport();
        capture.addProperty("embedded_checkout_support", checkoutSupport.name());
        capture.addProperty("embedded_checkout_support_value", checkoutSupport.getNumber());
        JsonArray modsArray = new JsonArray();
        for (ModMessage mod : handshake.getInstalledModsList()) {
            JsonObject modObj = new JsonObject();
            modObj.addProperty("id", mod.getId());
            modObj.addProperty("name", mod.getName());
            modObj.addProperty("version", mod.getVersion());
            modObj.addProperty("type", mod.getType().name());
            modObj.addProperty("type_value", mod.getTypeValue());
            modsArray.add(modObj);

            getLogger().info("  Mod: " + mod.getName() + " (" + mod.getId() + ") v" + mod.getVersion() + " [" + mod.getType().name() + "]");
        }
        capture.add("installed_mods", modsArray);
        capture.addProperty("mod_count", modsArray.size());
        Map<String, ?> modStatus = handshake.getModStatusMap();
        if (!modStatus.isEmpty()) {
            JsonObject modStatusObj = new JsonObject();
            for (Map.Entry<String, ?> entry : modStatus.entrySet()) {
                Object value = entry.getValue();
                modStatusObj.add(entry.getKey(), valueToJson(value));
            }
            capture.add("mod_status", modStatusObj);
            getLogger().info("  Mod Status entries: " + modStatus.size());
        } else {
            capture.add("mod_status", new JsonObject());
            getLogger().info("  Mod Status: (empty)");
        }
        getLogger().info("Minecraft: " + handshake.getMinecraftVersion().getEnum());
        getLogger().info("Lunar Client: " + handshake.getLunarClientVersion().getSemver() + " (" +
                handshake.getLunarClientVersion().getGitBranch() + "/" +
                handshake.getLunarClientVersion().getGitCommit() + ")");
        getLogger().info("Total mods: " + handshake.getInstalledModsList().size());
        getLogger().info("Mod status entries: " + modStatus.size());
        try {
            File captureFile = new File(getDataFolder(), "captures/handshake_" +
                    player.getName() + "_" + DATE_FORMAT.format(new Date()) + ".json");
            try (FileWriter writer = new FileWriter(captureFile)) {
                GSON.toJson(capture, writer);
            }
            getLogger().info("Saved handshake capture to: " + captureFile.getAbsolutePath());
        } catch (IOException e) {
            getLogger().severe("Failed to save handshake capture: " + e.getMessage());
        }
    }

    private JsonElement valueToJson(Object value) {
        try {
            Class<?> valueClass = value.getClass();
            JsonObject obj = new JsonObject();
            Object kindCase = valueClass.getMethod("getKindCase").invoke(value);
            String kindCaseName = kindCase.toString();

            switch (kindCaseName) {
                case "NULL_VALUE":
                    Object nullValue = valueClass.getMethod("getNullValue").invoke(value);
                    obj.addProperty("null_value", nullValue.toString());
                    break;
                case "NUMBER_VALUE":
                    double numberValue = ((Number) valueClass.getMethod("getNumberValue").invoke(value)).doubleValue();
                    obj.addProperty("number_value", numberValue);
                    break;
                case "STRING_VALUE":
                    String stringValue = (String) valueClass.getMethod("getStringValue").invoke(value);
                    obj.addProperty("string_value", stringValue);
                    break;
                case "BOOL_VALUE":
                    boolean boolValue = (Boolean) valueClass.getMethod("getBoolValue").invoke(value);
                    obj.addProperty("bool_value", boolValue);
                    break;
                case "STRUCT_VALUE":
                    Object structValue = valueClass.getMethod("getStructValue").invoke(value);
                    Map<?, ?> fieldsMap = (Map<?, ?>) structValue.getClass().getMethod("getFieldsMap").invoke(structValue);
                    JsonObject struct = new JsonObject();
                    for (Map.Entry<?, ?> entry : fieldsMap.entrySet()) {
                        struct.add(entry.getKey().toString(), valueToJson(entry.getValue()));
                    }
                    obj.add("struct_value", struct);
                    break;
                case "LIST_VALUE":
                    Object listValue = valueClass.getMethod("getListValue").invoke(value);
                    java.util.List<?> valuesList = (java.util.List<?>) listValue.getClass().getMethod("getValuesList").invoke(listValue);
                    JsonArray list = new JsonArray();
                    for (Object listItem : valuesList) {
                        list.add(valueToJson(listItem));
                    }
                    obj.add("list_value", list);
                    break;
                case "KIND_NOT_SET":
                    obj.addProperty("kind", "NOT_SET");
                    break;
                default:
                    obj.addProperty("kind", kindCaseName);
                    obj.addProperty("raw_value", value.toString());
                    break;
            }
            return obj;
        } catch (Exception e) {
            JsonObject obj = new JsonObject();
            obj.addProperty("error", "Failed to parse value: " + e.getMessage());
            obj.addProperty("raw_value", value.toString());
            return obj;
        }
    }
}

