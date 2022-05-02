//algorithm.sc
insert(container, index, value) ->
{
    result = null;
    // print(container);
    // print(value);
    // print(length(container));
    if(type(container) == 'list',
        if(index == length(container),
            result = container;
            result += value;,
            result = [];    
            for(container,
                if(_i == index, result += value);
                result += _;
            );
        );
    );
    // print(result);
    return(result);
};