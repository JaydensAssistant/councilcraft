# CouncilCraft packwiz project (0.2.0-mvp)
- Rebuild configs: `python3 tools/mkconfigs.py <generated-default-config-dir> config` (NOTE: it also writes Discord-Integration.toml; that file belongs in server-only/config/ since DCI is excluded from the MVP; move it after a rerun).
- Rebuild data: `python3 tools/mkdatapack.py <extracted-jars-dir> <vanilla-data-dir> moonlight-global-datapacks/councilcraft-data`
- Export: `packwiz refresh && packwiz modrinth export -o ../dist/CouncilCraft-0.2.0-mvp.mrpack`
- server-only/: Ledger + Discord Integration (not in the .mrpack; see server-only/README.md).
