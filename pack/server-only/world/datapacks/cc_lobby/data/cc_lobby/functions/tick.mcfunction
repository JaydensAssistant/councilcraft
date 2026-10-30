execute if score #active cc_lobby matches 1 if entity @e[type=minecraft:marker,tag=cc_lobby] run function cc_lobby:enforce
execute if score #launched cc_lobby matches 1 as @a[tag=cc_prelaunch] at @s run function cc_lobby:late_joiner
