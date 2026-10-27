// PT44 test prelude: just enough of KubeJS's globals for CouncilCraft's scripts to load. Each registration is
// recorded (events.<name> = handler) and never run unless a test calls it. Runs through the pack's real Rhino jar.
var events = {}
function ccReg(name) { return function () { events[name] = arguments[arguments.length - 1] } }
var EntityEvents = { spawned: ccReg('entity.spawned') }
var PlayerEvents = { tick: ccReg('player.tick') }
var ServerEvents = { commandRegistry: ccReg('server.commandRegistry'), loaded: ccReg('server.loaded'), tick: ccReg('server.tick'),
  recipes: ccReg('server.recipes'), tags: ccReg('server.tags') }
var ItemEvents = { tooltip: ccReg('item.tooltip') }
var Text = {}
;['gold', 'white', 'red', 'green', 'gray', 'darkGray', 'yellow', 'aqua', 'of'].forEach(function (c) { Text[c] = function (s) { return { color: c, text: String(s) } } })
var javaClasses = {}
var Java = { loadClass: function (name) { return javaClasses[name] } }
var failures = []
function check(cond, msg) { if (!cond) failures.push(msg) }
function checkEq(actual, expected, msg) { if (JSON.stringify(actual) !== JSON.stringify(expected)) failures.push(msg + ': expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual)) }
