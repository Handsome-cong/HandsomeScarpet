//trading_tracker.sc
import('translation', '__translation_handle_start', '__translation_handle_close', 'get_translation', 'show_translation');
import('math', '_euclidean_sq');
global_range = 32;
global_interval = 10;
global_duration = 10;
global_active = false;

global_switchs = {
    'gossips_track' -> false,
    'offers_track' -> {
        'items_sell' -> [['_id', '_tag']],
        'items_buy' -> [],
        'index' -> {
            '_uuid' -> ['_index']
        },
        'uses_track' -> false,
        'demand_track' -> false,
        'item_track' -> false,
    }
};

__config() ->
{
    'commands' -> {
        'gossips' -> ['__toggle', 'gossips_track'],
        'uses' -> ['__toggle', ['offers_track', 'uses_track']],
        'demand' -> ['__toggle', ['offers_track', 'demand_track']],
        'item' -> ['__toggle', ['offers_track', 'item_track']],
        'track villager <uuid> <index>' -> '__villager_track',
        'track villager nearest <index>' -> _(index) -> __villager_track(null, index);,
        'track item <item> buy' -> ['__item_track', true],
        'track item <item> sell' -> ['__item_track', false],
    },
    'arguments' -> {
        'uuid' -> {'type' -> 'uuid'},
        'index' -> {
            'type' -> 'term',
            'suggester' -> _(args) -> (
                parsed_data = parse_nbt(entity_id(args:0));
                len = length(parsed_data:'Offers':'Recipes');
                lis = [];
                for(range(len), lis += _i);
                lis
            )
        },
        'item' -> {'type' -> 'item'}
    }
};

__on_start() ->
{
    __translation_handle_start();
};

__on_close() ->
{
    __translation_handle_close();
};

__get_track_labels(entity) ->
{
    parsed_data = parse_nbt(entity ~ 'nbt');
    eid = entity ~ 'uuid';
    pid = parse_nbt(player() ~ 'nbt'):'UUID';
    labels = [];
    ut = global_switchs:'offers_track':'uses_track';
    dt = global_switchs:'offers_track':'demand_track';
    it = global_switchs:'offers_track':'item_track';
    offers = parsed_data:'Offers':'Recipes';
    //print('xx');
    if(ut || dt || it,
        lis = global_switchs:'offers_track':'index':eid;
        //print(lis);
        len = length(lis);
        cur = 0;
        for(offers,
            match = false;
            isBuy = _:'sell':'id' == 'minecraft:emerald';
            if(isBuy,
                item = [_:'buy':'id', if(_:'buy':'tag', _:'buy':'tag', {})];,
                item = [_:'sell':'id', if(_:'sell':'tag', _:'sell':'tag', {})];
            );
            if(cur < len && lis:cur == _i,
                cur += 1;
                match = true;,
                lisName = if(isBuy, 'items_buy', 'items_sell');
                items = global_switchs:'offers_track':lisName;
                //print('item: '+ item);
                for(items,
                    //print(_);
                    if(_ == item, 
                        match = true;
                        break();
                    );
                );
            );
            if(match,
                annotation = '';
                value = '';
                if(ut,
                    annotation += 'uses';
                    value += format('r ' + _:'uses' + '/' + _:'maxUses');
                );
                if(dt,
                    annotation += ' demand';
                    value += ' ' + format('e ' + _:'demand');
                );
                if(it,
                    if(_:'sell':'id' == 'minecraft:emerald',
                        annotation += ' buy';
                        id = get_translation(_:'buy':'id' - 'minecraft:');
                        //print(format('t ' + id));
                        value += ' ' + format('t ' + id);,
                        // value += ' ' + format('t ' + _:'buy':'id');,
                        annotation += ' sell';
                        id = get_translation(_:'sell':'id' - 'minecraft:');
                        value += ' ' + format('t ' + id);
                        // value += ' ' + format('t ' + _:'sell':'id');
                    );
                );
                annotation += ':';
                put(labels, 0, ['offer_' + _, annotation, value], 'insert');
            );
        );
    );
    if(global_switchs:'gossips_track',
        annotation = 'Gossips & reputation:';
        value = [0, 0, 0, 0, 0, 0];
        for(parsed_data:'Gossips',
            if(_:'Target' == pid,
                if(_:'Type' == 'major_negative', value:0 = _:'Value'; continue(););
                if(_:'Type' == 'minor_negative', value:1 = _:'Value'; continue(););
                if(_:'Type' == 'minor_positive', value:2 = _:'Value'; continue(););
                if(_:'Type' == 'major_positive', value:3 = _:'Value'; continue(););
                if(_:'Type' == 'trading', value:4 = _:'Value'; continue(););
            );
        );
        value:5 = (-5 * value:0) + (-1 * value:1) + value:2 + (5 * value:3) + value:4;
        text = format('r ' + value:0 + ' ', 'y ' + value:1 + ' ', 'c ' + value:2 + ' ', 'e ' + value:3 + ' ', 'w ' + value:4);
        labels += ['gossips reputation:', annotation, text];
    );
    return(labels);
};

__villager_track(uuid, index) ->
{
    index = number(index);
    lis = global_switchs:'offers_track':'index';
    if(uuid == null,
        nearest = null;
        p = player();
        pos = p ~ 'pos';
        in_dimension(p,
            for(entity_area('valid', p, global_range, global_range, global_range),
                if(_ ~ 'type' == 'villager' && (!nearest || _euclidean_sq(_ ~ 'pos', pos) < _euclidean_sq(nearest ~ 'pos', pos)),
                    nearest = _;
                );
            );
        );
        // for(entity_area('valid', p, global_range, global_range, global_range),
        // );
        print(nearest);
        if(nearest, uuid = nearest ~ 'uuid', return());
    );
    if(!has(lis:uuid), put(lis, uuid, []));
    found = false;
    for(lis:uuid,
        if(_:0 == index,
            found = true;
            delete(lis:uuid:_i);
            break();
        );
    );
    if(!found,
        lis:uuid += index;
        lis:uuid = sort(lis:uuid);
    );
    global_switchs:'offers_track':'index' = lis;
};

__item_track(item, isBuy) ->
{
    // print(type(item:2));
    // print(item);
    id = 'minecraft:' + item:0;
    tag = if(item:2, parse_nbt(item:2), {});
    lisName = if(isBuy, 'items_buy', 'items_sell');
    items = global_switchs:'offers_track':lisName;
    found = false;
    for(items,
        if([_:0, _:1] == [id, tag],
            delete(items:_i);
            found = true;
            break();
        );
    );
    if(!found, items += [id, tag]);
    global_switchs:'offers_track':lisName = items;
    return;
};

__toggle(feature) ->
{
    if(type(feature) == 'list',
        [f1, f2] = feature;
        global_switchs:f1:f2 = !global_switchs:f1:f2;,
        global_switchs:feature = !global_switchs:feature;
    );
    if(!global_active, __tick_tracker());
};

__tick_tracker() ->
{
    need_track = false;
    need_track = 
        global_switchs:'gossips_track' || 
        global_switchs:'offers_track':'uses_track' || 
        global_switchs:'offers_track':'demand_track' || 
        global_switchs:'offers_track':'item_track';
    //print(need_track);
    global_active = need_track;
    if(!need_track, return());
    p = player();
    in_dimension(p,
        for(entity_area('valid', p, global_range, global_range, global_range),
            __handle_entity(_);
        );
    );
    schedule(global_interval, '__tick_tracker');
};

__handle_entity(entity) ->
{
    if(entity ~ 'type' != 'villager', return());
    shapes_to_display = [];
    labels_to_add = __get_track_labels(entity);
    base_pos = [0.5, entity ~ 'height' + 0.3, 0.5];
    base_height = 0;
    for(labels_to_add,
        [label, annot, value] = _;
        shapes_to_display += [
            'label', global_duration, 'text', format('gi ' + annot), 'pos', base_pos,
            'follow', entity, 'height', base_height, 'align', 'right', 'indent', -0.2, 'snap', 'dxydz'];
        shapes_to_display += [
            'label', global_duration, 'text', label,'value', value, 'pos', base_pos,
            'follow', entity, 'height', base_height, 'align', 'left', 'snap', 'dxydz'];
        base_height += 1;
    );
    draw_shape(shapes_to_display);
};