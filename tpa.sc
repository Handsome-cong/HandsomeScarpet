// tpa.sc
global_requester = null;
__config() -> {
    'commands' -> {
        '<player>' -> _(to) -> signal_event('tp_request', to, player()),
      'accept' -> _() -> if(global_requester, 
         run('tp '+global_requester~'command_name'); 
         global_requester = null
      )
    },
   'arguments' -> {
      'player' -> {'type' -> 'players', 'single' -> true}
   }
};
handle_event('tp_request', _(req) -> (
   global_requester = req;
   print(player(), format(
      'w '+req+' requested to teleport to you. Click ',
      'yb here', '^yb here', '!/tpa accept',
      'w  to accept it.'
   ));
));