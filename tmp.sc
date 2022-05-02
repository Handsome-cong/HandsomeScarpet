
__config() ->
{
    'commands' -> {
        'structure <structure>' -> _(s) -> __toggle(s, 'structures'),
        'slime_chunks' -> ['__toggle', 'slime_chunks', 'chunks'],
        'portal coordinates' -> ['__toggle', 'coords', 'portals'],
        'portal links' -> ['__toggle', 'links', 'portals'],
        '<shape> <radius> following <entities>' -> ['display_shape', [null,0xffffffff], true],
        '<shape> <radius> at <entities>' -> ['display_shape', [null,0xffffffff], false],
        '<shape> <radius> following <entities> <color>' -> ['display_shape', true],
        '<shape> <radius> at <entities> <color>' -> ['display_shape', false],
        '<shape> clear' -> 'clear_shape',
        'clear' -> 'clear',
    },
    'arguments' -> {
        'structure' -> {'type' -> 'term', 'suggest' -> plop():'structures' },
        'radius' -> {'type' -> 'int', 'min' -> 0, 'max' -> 1024, 'suggest' -> [128, 24, 32]},
        'shape' -> {'type' -> 'term', 'options' -> keys(global_shapes) },
        'color' -> {'type' -> 'teamcolor'}
    }
};