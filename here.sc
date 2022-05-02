__config() ->
{
    'commands' -> {
        '' -> _() -> modify(player(), 'effect', 'glowing', 300, 0, false),
    }
};