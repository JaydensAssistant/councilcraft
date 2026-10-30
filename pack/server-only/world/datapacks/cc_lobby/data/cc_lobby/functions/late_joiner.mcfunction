# A pre-launch player who was offline at launch: drop them safely at spawn, then reset them
spreadplayers 1 1 2 8 false @s
function cc_lobby:reset_player
tellraw @s {"text":"CouncilCraft launched while you were away. Good luck!","color":"gold"}
