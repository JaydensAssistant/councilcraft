scoreboard objectives add cc_lobby dummy
execute if score #active cc_lobby matches 1 run team add cc_lobby
execute if score #active cc_lobby matches 1 run team modify cc_lobby friendlyFire false
