// CC-132: RU grasses must not be axe-mineable (Inventorio belt tool swap)
ServerEvents.tags('block', event => {
  const grasses = [
    'regions_unexplored:frozen_grass',
    'regions_unexplored:medium_grass',
    'regions_unexplored:bladed_grass',
    'regions_unexplored:sandy_grass',
    'regions_unexplored:sandy_tall_grass',
    'regions_unexplored:steppe_grass',
    'regions_unexplored:steppe_tall_grass',
    'regions_unexplored:bladed_tall_grass',
    'regions_unexplored:windswept_grass'
  ]
  grasses.forEach(id => event.remove('minecraft:mineable/axe', id))
})
