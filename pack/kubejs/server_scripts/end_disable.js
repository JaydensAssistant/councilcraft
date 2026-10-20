// End-disable (S39/S242, audit M10). Delete THIS FILE (and /reload) to open the End.
BlockEvents.rightClicked('minecraft:end_portal_frame', event => {
  if (String(event.item.id) === 'minecraft:ender_eye') {
    event.player.setStatusMessage(Text.darkPurple('The End is sealed for now.'))
    event.cancel()
  }
})
