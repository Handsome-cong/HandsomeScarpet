//lazy_trading.sc


global_items_buy =
[
    {
        'id' -> 'minecraft:paper',
        'tag' -> {
        }
    }
];

global_items_sell =
[
    {
        'id' -> 'minecraft:enchanted_book',
        'tag' -> {
            'StoredEnchantments' -> [
                {'id' -> 'minecraft:infinity', 'lvl' -> 1}
            ]
        }
    },
    {
        'id' -> 'minecraft:enchanted_book',
        'tag' -> {
            'StoredEnchantments' -> [
                {'id' -> 'minecraft:luck_of_the_sea', 'lvl' -> 1}
            ]
        }
    },
    {
        'id' -> 'minecraft:bookshelf',
        'tag' -> {
        }
    }
];

global_trade_mode = 'all';

set_item(item_info, mode) ->
{
    lis = if(mode == 'buy', global_items_buy, global_items_sell);
    item = {
        'id' -> ('minecraft:' + item_info:0),
        'tag' -> parse_nbt(item_info:2)
    };
    //existed
};

__on_player_interacts_with_entity(player, entity, hand) ->
{
    if(entity ~ 'type' == 'villager',
        uuid = parse_nbt(player ~ 'nbt'):'UUID';
        parsed_data = __check_demand(entity);
        reputation = 0;
        for(parsed_data:'Gossips',
            if(_:'Target' != uuid, continue());
            print(_:'Target');
            val = _:'Value';
            t = _:'Type';
            if(t == 'major_negative', val = val * -5);
            if(t == 'minor_negative', val = val * -1);
            if(t == 'major_positive', val = val * 5);
            reputation += val;
        );

        price_factor = 1;
        buff = query(player, 'effect', 'hero_of_the_village');
        if(buff, price_factor = 0.7 - 0.0625 * buff:0);

        for(parsed_data:'Offers':'Recipes',
            offer = _;
            index = _i;
            if(global_trade_mode == 'all',
                trade_count = offer:'maxUses' - offer:'uses';,
                //trade_count = min(1, trade_count);,//DEBUG
                trade_count = offer:'maxUses' / 2 - offer:'uses';
                trade_count = max(0, trade_count);

                //trade_count = min(1, trade_count);//DEBUG

            );
            print('recipe: ' + _i);
            if(offer:'buy':'id' == 'minecraft:emerald',
                for(global_items_sell,
                    if(__match([_:'id', _:'tag'], [offer:'sell':'id', offer:'sell':'tag']),
                        [offer, parsed_data] = __trade(player, entity, index, offer, parsed_data, trade_count, price_factor, reputation);
                        // print('1 '+_i);
                        break();
                    );
                );,
                for(global_items_buy,
                    if(__match([_:'id', null], [offer:'buy':'id', null]),
                        [offer, parsed_data] = __trade(player, entity, index, offer, parsed_data, trade_count, price_factor, reputation);
                        // print('2 '+_i);
                        break();
                    );
                );
            );
            parsed_data:'Offers':'Recipes':_i = offer;
        );
        modify(entity, 'nbt', encode_nbt(parsed_data));
    );
};

__trade(player, entity, index, offer, parsed_data, trade_count, price_factor, reputation) ->
{
    loop(trade_count,
        pMul = offer:'priceMultiplier';
        price = offer:'buy':'Count';
        price = price - reputation * pMul + price * pMul * max(0, offer:'demand');
        price = price * price_factor;
        price = max(0, min(64, floor(price)));
        item_buy_1 = [offer:'buy':'id', price];
        item_buy_2 = null;
        if(offer:'buyB':'id' != 'minecraft:air',
            item_buy_2 = [offer:'buyB':'id', offer:'buyB':'Count'];
            flag = __query_items_count(item_buy_2:0) >= item_buy_2:1;,
            flag = true;
        );
        item_sell = [offer:'sell':'id', offer:'sell':'Count', offer:'sell':'tag'];

        if(__query_items_count(item_buy_1:0) >= item_buy_1:1 && flag,
            inventory_remove(player, item_buy_1:0, item_buy_1:1);
            if(item_buy_2,inventory_remove(player, item_buy_2:0, item_buy_2:1));
            // print('give ' + (player ~ 'command_name') + ' ' + item_sell:0 + item_sell:2 + ' ' + item_sell:1);
            // print(encode_nbt(item_sell:2));
            // print('vv');
            run('give ' + (player ~ 'command_name') + ' ' + item_sell:0 + if(item_sell:2!=null, encode_nbt(item_sell:2), '') + ' ' + item_sell:1);
            // offer:'demand' = offer:'demand' + 1;
            __push_demand(entity, index, 1);
            offer:'uses' = offer:'uses' + 1;
            parsed_data:'Xp' = parsed_data:'Xp' + offer:'xp';
            found = false;
            uuid = parse_nbt(player ~ 'nbt'):'UUID';
            for(parsed_data:'Gossips',
                if(_:'Type' == 'trading' && _:'Target' == uuid,
                    parsed_data:'Gossips':_i:'Value' = min(25, parsed_data:'Gossips':_i:'Value' + 2);
                    found = true;
                    break();
                );
            );
            if(!found, parsed_data:'Gossips' += {'Type' -> 'trading', 'Value' -> 2, 'Target' -> uuid});
            print('maxUses: ' + offer:'maxUses');
            print('uses: ' + offer:'uses');
            print('demand: ' + offer:'demand');
            run('summon minecraft:experience_orb ' + (entity ~ 'x') + ' ' + (entity ~ 'y') + ' ' + (entity ~ 'z') + ' {Value:' + offer:'xp' + '}');
        );
    );
    return([offer, parsed_data]);
};

__match(item_model, item_target) ->
{
    result = true;
    id_model = (item_model:0) - 'minecraft:';
    id_target = (item_target:0) - 'minecraft:';
    if(id_model == id_target,
        if(id_target == 'enchanted_book',
            tag_model = item_model:1;
            tag_target = item_target:1;
            for(tag_model:'StoredEnchantments',
                enchantment_model = _;
                flag = false;
                for(tag_target:'StoredEnchantments',
                    if(_:'id' == enchantment_model:'id' && _:'lvl' == enchantment_model:'lvl',
                        flag = true;
                        break();
                    );
                );
                if(!flag,
                    result = false;
                    break();
                );
            );
        );,
        result = false;
    );
    return(result);
};

__query_items_count(id) ->
{
    slot = 0;
    p = player();
    count = 0;
    while(slot <= 41 && (slot = inventory_find(p, id, slot)) != null, 41,
        item_info = inventory_get(p, slot);
        count += item_info:1;
        slot += 1;
    );
    return(count);
};

// {
//     '_uuid' -> {
//         _index -> _demand
//     }
// }

__check_demand(entity) ->
{
    demand_buffer = system_variable_get('DEMAND_BUFFER', {});
    parsed_data = parse_nbt(entity ~ 'nbt');
    //remove extra buffer
    for(pairs(demand_buffer),
        if(entity_id(_:0) ~ 'removed', delete(demand_buffer:(_:0)));
    );
    //set demand
    uuid = entity ~ 'uuid';
    if(has(demand_buffer:uuid),
        replenished = true;
        for(parsed_data:'Offers':'Recipes',
            if(_:'uses' > 0,
                replenished = false;
                break();
            );
        );
        if(replenished,
            for(pairs(demand_buffer:uuid),
                parsed_data:'Offers':'Recipes':(_:0):'demand' += _:1;
            );
            delete(demand_buffer:uuid);
        );
    );
    system_variable_set('DEMAND_BUFFER', demand_buffer);
    return(parsed_data);
};

__push_demand(entity, index, demand) ->
{
    uuid = entity ~ 'uuid';
    demand_buffer = system_variable_get('DEMAND_BUFFER', {});
    if(!has(demand_buffer:uuid), put(demand_buffer, uuid, {}));
    if(!has(demand_buffer:uuid:index), put(demand_buffer:uuid, index, 0));
    demand_buffer:uuid:index =demand_buffer:uuid:index + demand;
    system_variable_set('DEMAND_BUFFER', demand_buffer);
};