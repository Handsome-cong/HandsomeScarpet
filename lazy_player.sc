//lazy_player.sc
import('algorithm', 'insert');

global_run_cmd = true;
global_fake_players = null;

__config() ->
{
    'commands' -> {
        'load' -> 'load_players_data',
        'invoke <storedName>' -> 'start_action',
        'clear <storedName>' -> ['__clear_data', null],
        'clear <storedName> <index>' -> '__clear_data',
        'data' -> 'print_player',
        'data <storedName>' -> 'print_player_info',
        'edit <storedName> delay <index> <tick>' -> '__player_editDelay',
        'edit <storedName> comment <text>' -> '__player_editComment',
        'doActions <toggle>' -> _(toggle) -> global_run_cmd = toggle;,
        'moveAction <storedName> <from> <to>' -> '__player_move_action',
        'help' -> 'show_helpInfo',
        '<name> spawn' -> ['__player_spawn', null, null, null],
        '<name> spawn at <location>' -> ['__player_spawn', null, null],
        '<name> spawn at <location> facing <direction>' -> ['__player_spawn', null],
        '<name> spawn at <location> facing <direction> in <dimension>' -> '__player_spawn',
        '<name> kill' -> '__player_kill',
        '<name> look at <location>' -> ['__player_look', null],
        '<name> look at <location> interval <tick>' -> ['__player_look'],
        '<name> look <innerDir>' -> ['__player_look', null],
        '<name> <simpleAct> <mode>' -> ['__player_simpleAct', null],
        '<name> <simpleAct> interval <tick>' -> _(name, act, tick) -> __player_simpleAct(name, act, 'interval', tick);,
        '<name> do <simpleCtr>' -> '__player_simpleCtr',
        '<name> say <text>' -> '__player_say',
    },
    'arguments' -> {
        'name' -> {
            'type' -> 'term', 
            'suggester' -> _(args) -> (
                nameset = {'Alex', 'Steve'};
                for(player('all'), nameset += _);
                keys(nameset)
            )
        },
        'storedName' -> {
            'type' -> 'term', 
            'suggester' -> _(args) -> (
                pull_player_data();
                keys(global_fake_players)
            )
        },
        'from' -> {'type' -> 'int', 'min' -> 0},
        'to' -> {'type' -> 'int', 'min' -> 0},
        'toggle' -> {'type' -> 'bool'},
        'index' -> {'type' -> 'int', 'min' -> 0},
        'location' -> {'type' -> 'location'},
        'direction' -> {'type' -> 'rotation'},
        'dimension' -> {'type' -> 'dimension'},
        'innerDir' -> {'type' -> 'term', 'options' -> ['up', 'down', 'west', 'east', 'north', 'south']},
        'tick' -> {'type' -> 'int', 'min' -> 1},
        'simpleAct' -> {'type' -> 'term', 'options' -> ['use', 'attack', 'jump', 'swapHands']},
        'mode' -> {'type' -> 'term', 'options' -> ['once', 'continuous']},
        'simpleCtr' -> {'type' -> 'term', 'options' -> ['sneak', 'unsneak', 'unsprint', 'sprint', 'stop']},
        'text' -> {'type' -> 'text'},
    }
};

show_helpInfo() ->
{
    print('------------使用说明------------');
    print('懒人player v0.1');
    print('这是一个把carpet的player再封装后的脚本，可以记录操作并调用。');
    print('基本操作和carpet的player一样，但是只封装了部分功能。');
    print('目前实现的有：' + format(
        'c [spawn][kill][look][use][attack][jump][swapHands][sneak][unsneak][unsprint][sprint][stop][say]'
    ));
    print(format('c [say] ') + '使用方法与player指令一致，模拟假人发言');
    print(format('d load') + '加载数据');
    print(format('d invoke <storedName>') + '执行指定名称假人的行动队列');
    print(format('d clear <storedName> <index>') + '删除指定名称假人的行动数据，index指定元素位置，不指定则删除假人全部数据');
    print(format('d data <storedName>') + '列出所有假人，指定storedName则列出指定假人详细数据，若行动队列开头为spawn指令，则会将其中的坐标输出');
    print(format('d edit <storedName> delay <index> <tick>') + '编辑指定假人的指定行动队列元素的延迟');
    print(format('d edit <storedName> comment <text>') + '编辑指定假人的注释');
    print(format('d doActions <toggle>') + '是否在记录操作的同时执行对应player指令');
    print(format('d moveAction <storedName> <from> <to>') + '移动指定假人的指定行动队列元素至指定位置');
    print(format('d help') + '显示帮助信息');
    print(format('r 由于player指令有时会将name全部转换为小写，有时又不会，使用已经存在的name时请确保与当初输入的一致'));
};

//从系统变量中读取数据
pull_player_data() ->
{
    global_fake_players = system_variable_get('FAKEPLAYERS', {});
};

//将本地数据提交至系统变量并持久化存储
push_player_data() ->
{
    system_variable_set('FAKEPLAYERS', global_fake_players);
    delete_file('fake_players_data', 'json');
    write_file('fake_players_data', 'json', global_fake_players);
};

//从文件中读取数据并提交
load_players_data() ->
{
    global_fake_players = read_file('fake_players_data', 'json');
    //print(global_fake_players);
    system_variable_set('FAKEPLAYERS', global_fake_players);
    return ;
};

//开始执行指定假人的对应动作列表
start_action(name) ->
{
    pull_player_data();
    actList = global_fake_players:name:'actionList';
    if(actList,
        delay = 0;
        for(actList,
            delay += _:'delay';
            schedule(delay, '__do_action', _:'action');
        );
    );
};

//执行指定操作
__do_action(action) ->
{
    run(action);
    return;
};

//存入数据
__put_data(name, action, delay) -> 
{
    if(!has(global_fake_players:name),
        global_fake_players:name = {
            'actionList' -> [],
            'comment' -> ''
        }
    );
    global_fake_players:name:'actionList' += {
        'action' -> action,
        'delay' -> delay
    };
    push_player_data();
    if(global_run_cmd,
        __do_action(action);
    );
    return;
};

__player_editDelay(name, index, tick) ->
{
    if(has(global_fake_players:name:'actionList'),
        global_fake_players:name:'actionList':index:'delay' = tick;
        print(name + '\'s action_delay ' + index + ' has been set to ' + tick + '.');
    );
    push_player_data();
};

__player_editComment(name, text) ->
{
    if(has(global_fake_players:name),
        global_fake_players:name:'comment' = text;
        print(name + '\'s comment has been changed.');
    );
    push_player_data();
};

//移动动作列表中的元素
//scarpet的insert操作在列表元素为map时，因奇怪的bug而无法使用
//这里用自己写的insert函数做替代，大概也许可能不会出别的bug 
__player_move_action(name, from, to) ->
{
    if(has(global_fake_players:name:'actionList':from) && has(global_fake_players:name:'actionList':to),
        tmp = global_fake_players:name:'actionList':from;
        delete(global_fake_players:name:'actionList':from);
        global_fake_players:name:'actionList' = insert(global_fake_players:name:'actionList', to, tmp);
        print(name + '\'s action ' + from + ' has been moved to ' + to + '.');
    );
    push_player_data();
};

//清除数据
__clear_data(name, index) ->
{
    if(index != null,
        delete(global_fake_players:name:'actionList':index);
        print(name + '\'s action ' + index +' deleted.');,
        delete(global_fake_players:name);
        print(name + ' deleted.');
    );
    push_player_data();
};

print_player() ->
{
    //print(pairs(global_fake_players));
    for(pairs(global_fake_players),
        [name, actLis, cmt] = [_:0, _:1:'actionList', _:1:'comment'];
        len = length(name);
        print(format(
            'lu ' + name + ' ', '^di ' + cmt, 'w ' + '-' * (23 - len),
            'yb [info]', '^y click to show details', '!/lazy_player data ' + name,
            'yb [invoke]', '^y click to invoke ' + name , '!/lazy_player invoke ' + name
        ));
    );
};

print_player_info(name) ->
{
    if(has(global_fake_players:name),
        [actLis, cmt] = [global_fake_players:name:'actionList', global_fake_players:name:'comment'];
        print(format(
            'lu ' + name + ' ', 'w ' + '-' * (9 - len),
            'db [edit]', '^d click to edit comment', '?/lazy_player edit ' + name + ' comment ',
            'mb [clear]', '^m click to clear ' + name, '!/lazy_player clear ' + name,
            'yb [invoke]', '^y click to invoke ' + name, '!/lazy_player invoke ' + name
        ));
        if(length(actLis)>=1,
            act = actLis:0:'action';
            tmpl = split('\\ ', act);
            if(tmpl:2 == 'spawn',
                posStr = 
                    '[name:SpawnPoint_' + name + 
                    ', x:' + round(number(tmpl:4)) + 
                    ', y:' + round(number(tmpl:5)) + 
                    ', z:' + round(number(tmpl:6)) + 
                    ', dim:minecraft:' + tmpl:11 + ']';
                print(posStr);
            );
        );
        print(cmt);
        for(actLis,
            print(_:'action');
            print(
                format('c delay') + ': ' + _:'delay' + ' ' * (8 - length(_:'delay')) + 
                format(
                    'nb [clear]', '^n click to clear this action', 
                    '!/lazy_player clear ' + name + ' ' + _i,
                    'pb [edit]', '^p click to paste edit commend',
                    '?/lazy_player edit ' + name + ' delay ' + _i + ' ',
                    'eb [up]', '^e click to move up',
                    '!/lazy_player moveAction ' + name + ' ' + _i + ' ' + (_i - 1),
                    'eb [down]', '^e click to move down',
                    '!/lazy_player moveAction ' + name + ' ' + _i + ' ' + (_i + 1),
                );
            );
        );
    );
};

__player_spawn(name, location, direction, dimension) ->
{
    action = 'player'+ ' ' + name +' spawn';
    p = player();
    if(location,
        [x, y, z] = location,
        [x, y, z] = p ~ 'pos';
    );
    action += ' at ' + x + ' ' + y + ' ' + z;
    if(direction,
        [yaw, pitch] = direction;,
        pitch = p ~ 'pitch';
        yaw = p ~ 'yaw';
    );
    action += ' facing ' + yaw + ' ' + pitch;
    if(!dimension,
        dimension = p ~ 'dimension';
    );
    action += ' in ' + dimension;
    __put_data(name, action, 0);
};

__player_kill(name) ->
{
    action = 'player'+ ' ' + name +' kill';
    __put_data(name, action, 20);
};

__player_look(name, pos, interval) ->
{
    action = 'player'+ ' ' + name +' look ';
    if(type(pos) == 'string',
        action += pos,
        [x, y, z] = pos;
        action += 'at ' + x + ' ' + y + ' ' + z;
        if(interval, action += ' interval ' + interval);
    );
    __put_data(name, action, 20);
};

__player_simpleAct(name, act, mode, tick) ->
{
    action = 'player'+ ' ' + name +' ' + act + ' ' + mode;
    if(tick, action += ' ' + tick);
    __put_data(name, action, 20);
};

__player_simpleCtr(name, ctr) ->
{
    action = 'player'+ ' ' + name +' ' + ctr;
    __put_data(name, action, 20);
};

__player_say(name, text) ->
{
    action = 'tellraw @a ["<",{"selector":"@a[name=' + name + ']"},"> ' + text + '"]';
    __put_data(name, action, 20);
};