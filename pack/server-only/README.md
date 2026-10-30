# Server notes (this folder is NOT exported; nothing in it reaches a .mrpack)

JAVA: the dedicated server needs Java 21 (Temurin 21), because Ledger 1.3.18 is compiled for Java 21 (class file
version 65) and will not load on Java 17. Switch the host to Java 21 BEFORE installing or updating to 0.4.14 or later.
Clients stay on Java 17 (the client side of the .mrpack does not include Ledger).

Server-side mods in the main pack (0.4.14, PT6/PT8): Ledger 1.3.18-backport (ledger.pw.toml), Discord Integration
3.0.7-1.20.1 (dcintegration.pw.toml) and spark 1.10.53 (spark.pw.toml) are side = "server" packwiz entries, so the .mrpack
lists them as env client "unsupported", server "required". A Modrinth server import (and every pack update) installs
them; clients and singleplayer never get them. Nothing to add by hand.

Discord Integration: the pack does NOT ship config/Discord-Integration.toml (it holds the bot token and channel ids; an
override would wipe them on every update). DCI writes its own config on first start; set the bot token and channel ids
there once. config/Discord-Integration.toml in this folder is a reference copy of our settings only.
WARNING (S252): with no bot token DCI leaves DiscordIntegration.INSTANCE null and the server CRASHES on the first player
disconnect (NullPointerException in LinkManager.isPlayerLinked from the onPlayerLeave mixin). The server boots fine
without a token; set the token before players join.

Simple Voice Chat (PT9): config/voicechat/voicechat-server.properties is not shipped (SVC writes its defaults on first
start). Set port= to the host's voice chat allocation once; pack updates no longer reset it.

Immersive Portals (PT2): config/immersive_portals.json ships as a direct override for both sides (no yosbr copy, no
server-only copy).

Pre-launch lobby (CC-78/CC-79, 2026-10-30): world/datapacks/cc_lobby/ is the source of the live server's
world/datapacks/cc_lobby.zip (zip this folder's contents with pack.mcmeta at the zip root). Setup was run once with
`execute positioned 0 0 0 run function cc_lobby:setup` (Peaceful kept). Launch is the single console command
`function cc_lobby:launch` (take a panel backup first). Pre-launch players who were offline at launch are put on the
ground at spawn and reset on their next login. Server-only changes are tagged server-YYYY-MM-DD (CC-80).
