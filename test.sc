import('algorithm', 'insert');
import('global_storage', '__handle_start', '__handle_close');

global_a = 
{
    'xx' -> {
        'actionList' -> [
            {
                'action' -> ' spawn',
                'delay' -> 0
            },
            {
                'action' -> ' kill',
                'delay' -> 100
            }
        ],
        'comment' -> '测试'
    }
};

__config() ->
{
    'commands' -> {
        '<item>' -> _(item) -> print(item);
    },
    'arguments' -> {
        'item' -> {'type' -> 'item'}
    }
};

func_1() ->
{
};

func_2() ->
{
    p = player();
    e = create_marker('xx', p ~ 'pos', 'observer', false);
    modify(e, 'effect', 'glowing', 9999);
};

func_3() ->
{
    p = player();
    lis = [
        ['label', 100, 'pos', p ~ 'pos', 'text', '纸', 'height', 0, 'align', 'right'],
        ['label', 100, 'pos', p ~ 'pos', 'text', 'b', 'height', 0, 'align', 'left']
    ];
    draw_shape(lis);
};

__on_start() ->
{
    __handle_start();
};

__on_close() ->
{
    __handle_close();
};