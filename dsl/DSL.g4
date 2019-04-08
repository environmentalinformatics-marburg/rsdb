grammar DSL;

expression :  term (WS? plus_minus=PLUS_MINUS WS? term)*
           ;
           
term : entity (WS? mul_div=MUL_DIV WS? entity)*
     ;               
         
entity : '(' WS? expression WS? ')'
       | seq
       | constant
       | function 
       ;

seq : '[' WS? seq_element (WS? ',' WS? seq_element)*  WS? ']'
    ;
    
constant : PLUS_MINUS? INT('.'INT)?
		 ;           
       
function : ID ( '(' WS? (expression (WS? ',' WS? expression)*)? WS? ')' )?
		 ;
		 
seq_element : expression
            | range
            ;
		 
range : min=ID WS? ':' WS? max=ID
      ;

INT : ('0'..'9')+ ;

WS : [ \t\r\n]+ ;

ID : 'a'..'z' ('a'..'z' | '_' | '0'..'9')* ;

PLUS_MINUS : [+-] ;

MUL_DIV : [*/] ;