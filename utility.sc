//utility.sc
__on_player_interacts_with_entity(player, entity, hand) ->
{
    data = entity ~ 'nbt';
    delete_file('villager_data','nbt');
    write_file('villager_data','nbt',data);
    //print(data:'Offers':'Recipes');
    parsed_data = parse_nbt(data);
    parsed_data:'Offers':'Recipes':0:'specialPrice' = 10;
    data = encode_nbt(parsed_data);
    print(parsed_data:'Gossips');
    return ;
    // modify(entity, 'nbt', data);
    // modify(entity, 'nbt_merge', '{Offers:{Recipes:[9]}}') ;
};

__on_player_trades(player, entity, buy_left, buy_right, sell) ->
{
    data = entity ~ 'nbt';
    delete_file('villager_data','nbt');
    write_file('villager_data','nbt',data);
    //print(data:'Offers':'Recipes');
    parsed_data = parse_nbt(data);
    print(parsed_data:'Offers':'Recipes':0:'sell');
    print(type(sell:2));
    print(sell);
    return ;
};