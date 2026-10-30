# Launch (single command, run from console after a panel backup): function cc_lobby:launch
scoreboard players set #active cc_lobby 0
scoreboard players set #launched cc_lobby 1
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] run function cc_lobby:remove_box
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] run spreadplayers ~ ~ 2 8 false @a
execute as @a run function cc_lobby:reset_player
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] positioned over motion_blocking_no_leaves run setworldspawn ~ ~ ~
gamerule spawnRadius 10
team modify cc_lobby friendlyFire true
team empty cc_lobby
team remove cc_lobby
gamerule doDaylightCycle true
gamerule mobGriefing true
gamerule disableRaids true
gamerule doTraderSpawning false
gamerule playersSleepingPercentage 0
difficulty hard
kill @e[type=minecraft:marker,tag=cc_lobby]
ccborder reset
ccborder resume
tellraw @a {"text":"CouncilCraft is live. Good luck!","color":"gold"}
