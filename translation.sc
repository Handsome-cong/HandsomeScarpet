//translation.sc
import('global_storage', '__storage_handle_start', '__storage_handle_close', 'get_data', 'set_shared_data', 'save_data', 'load_data');

global_version = 0;

__config() ->
{
    'commands' -> {
        'set <source> <translation>' -> 'set_translation',
        'delete <source>' -> ['set_translation', null],
        'query <source>' -> _(src) -> print(get_translation(src));,
        '' -> 'help',
    },
    'arguments' -> {
        'source' -> {
            'type' -> 'term',
            'suggester' -> _(args) -> (
                keys(get_data('translation', {}));
            )
        },
        'translation' -> {'type' -> 'text'}
    }
};

help() ->
{
    print(format('l set ', 't <source> ', '^t 源文本', 't <translation>', '^t 对应的翻译文本'));
    print(format('l delete ', 't <source> ', '^t 需要删除的翻译的对应源文本'));
    print(format('l query ', 't <source> ', '^t 需要查询的翻译内容的对应源文本'));
};

__translation_handle_start() ->
{
    __storage_handle_start('translation_shared_files_type');
};

__translation_handle_close() ->
{
    __storage_handle_close();
};

get_translation(src) ->
{
    __update_version();
    trans = get_data('translation', {});
    if(has(trans:src), return(trans:src), return(src));
};

set_translation(src, value) ->
{
    __update_version();
    trans = get_data('translation', {});
    value = value - '"';
    if(has(trans:src), trans:src = value, put(trans, src, value));
    if(type(value) != 'string', delete(trans:src));
    set_shared_data('translation', trans);
    global_version += 1;
    save_data();
    system_variable_set('translation_version', global_version);
};

__update_version() ->
{
    ver = system_variable_get('translation_version', 0);
    if(ver > global_version,
        global_version = ver;
        load_data();
    );
};

show_translation() ->
{
    print(get_data('translation', {}));
};