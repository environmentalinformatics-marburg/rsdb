grammar IndexFuncDSL;

index_func : WS? func_name=id WS? ( PARAM_START WS? params=param_sequence? WS? PARAM_END WS? )? EOF
              ;

param_sequence : param (WS? PARAM_SEPARATOR WS? param)*        
         ;
         
param : param_name=id WS? PARAM_ASSIGNMENT WS? param_value=value
      ;         
         
id : (LETTER | UNDERSCORE) ( LETTER | DIGIT | UNDERSCORE )* 
   ;
   
value : number
   ;
   
number : SIGN? DIGIT+ (DECIMAL_SEPARATOR DIGIT+)?
       ;   

WS : [ \t\r\n]+ ;

PARAM_START: '(';

PARAM_END: ')';

PARAM_ASSIGNMENT : '=';

PARAM_SEPARATOR: ';';

LETTER : 'a'..'z' | 'A' .. 'Z';

UNDERSCORE : '_';

DIGIT : '0'..'9';

SIGN : '+' | '-';

DECIMAL_SEPARATOR : '.';

