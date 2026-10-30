# Run positioned at the world spawn column: execute positioned <X> 0 <Z> run function cc_lobby:setup
# `positioned over` already lands on the column center (x+0.5, z+0.5); do not add another 0.5.
kill @e[type=minecraft:marker,tag=cc_lobby]
execute positioned over motion_blocking_no_leaves run summon minecraft:marker ~ ~31 ~ {Tags:["cc_lobby"]}
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] run function cc_lobby:build_box
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] run setworldspawn ~ ~ ~
gamerule spawnRadius 0
gamerule doDaylightCycle false
time set day
scoreboard players set #active cc_lobby 1
team add cc_lobby
team modify cc_lobby friendlyFire false
