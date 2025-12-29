# Apollo Handshake Capture Plugin

A simple Bukkit/Spigot plugin that captures and logs all data from Apollo player handshakes.

## Building

```bash
cd handshake-capture-plugin
./gradlew build
```

The JAR will be in `build/libs/HandshakeCapture-1.0.0.jar`

## Installation

1. Place the JAR in your server's `plugins/` directory
2. Restart your server
3. Connect with Lunar Client - the plugin will automatically capture Apollo handshakes

## Output

The plugin will:
- Log all handshake data to console
- Save detailed JSON captures to `plugins/HandshakeCapture/captures/`

Each capture file contains:
- Minecraft version
- Lunar Client version (git branch, commit, semver)
- Complete list of installed mods (ID, name, version, type)
- Mod status/settings map
- Timestamp

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
