tag @a add cc_prelaunch
team join cc_lobby @a[team=!cc_lobby]
gamemode adventure @a[gamemode=!adventure,tag=!cc_lobby_exempt]
tag @a remove cc_in_box
execute at @e[type=minecraft:marker,tag=cc_lobby,limit=1] positioned ~-2 ~-1 ~-2 run tag @a[dx=3,dy=2,dz=3] add cc_in_box
execute as @a[tag=!cc_in_box,tag=!cc_lobby_exempt] at @e[type=minecraft:marker,tag=cc_lobby,limit=1] run tp @s ~ ~ ~
