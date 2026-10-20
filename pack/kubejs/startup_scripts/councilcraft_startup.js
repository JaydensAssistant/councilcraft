// CouncilCraft startup (0.4.2).
// PM43 1c / PM44: Supplementaries only lists the Heave Ho disc while its cannon is enabled (cannon stays off),
// so put the disc back in the vanilla Tools & Utilities tab (where music discs live). The cannon is NOT re-enabled.
StartupEvents.modifyCreativeTab('minecraft:tools_and_utilities', event => {
  event.add('supplementaries:music_disc_heave_ho')
})
