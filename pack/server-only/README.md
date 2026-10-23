# Server-only mods (not in the client/singleplayer .mrpack)

JAVA: the dedicated server needs Java 21 (Temurin 21), because Ledger 1.3.18 is compiled for Java 21 (class file
version 65) and will not load on Java 17. Clients stay on Java 17 (the client .mrpack does not include Ledger).

Ledger 1.3.18-backport (Modrinth project LVN9ygNV, version 85vYzkP3) is SERVER-ONLY (SPEC S247/S247c).
It is excluded from CouncilCraft-0.4.10.mrpack (the next release's .mrpack replaces it; same rule). For the dedicated server, install it from Modrinth:
https://cdn.modrinth.com/data/LVN9ygNV/versions/85vYzkP3/ledger-1.3.18.jar
sha512 ecf348e94eaac2faf926747f2b5bd466930398be22d35e11483ec3986251f91de35864a2acd7de2dbd74f4f2f6c11b1d8a240d2eab4cf93dc18245194bd70ac5
Requires Fabric Language Kotlin (already in the pack).

Discord Integration 3.0.7-1.20.1 (dcintegration.pw.toml, config/Discord-Integration.toml) is ALSO EXCLUDED from the
client/singleplayer .mrpack (S252 test, 2026-10-19). With no bot token, DCI leaves DiscordIntegration.INSTANCE null and the
server CRASHES on the first player disconnect (NullPointerException in LinkManager.isPlayerLinked from the onPlayerLeave
mixin). In singleplayer, leaving the world is a player disconnect, so it would crash every exit. Reproduced twice on the
MVP dedicated test server (crash-2026-10-19_03.43.21-server.txt). Only install it on a dedicated server that HAS a
valid bot token configured, and re-test a disconnect first.

Immersive Portals (PPM83, 0.4.9): the client gets config/immersive_portals.json only as a yosbr first-install default
(config/yosbr/config/), which a dedicated server never reads. The dedicated server must ship server-only/config/immersive_portals.json
as a FORCED config/immersive_portals.json (overwritten on every update; it carries the only non-default server value,
enableAlternateDimensions=false, plus the same client values). Put it in the server pack or an mrpack server-overrides/ entry.
