# Runs as one player at launch (or on first login after launch if they were offline)
gamemode survival @s
clear @s
xp set @s 0 levels
xp set @s 0 points
advancement revoke @s everything
effect clear @s
effect give @s minecraft:resistance 10 4 true
effect give @s minecraft:instant_health 1 10 true
effect give @s minecraft:saturation 1 20 true
tag @s remove cc_prelaunch
tag @s remove cc_in_box
