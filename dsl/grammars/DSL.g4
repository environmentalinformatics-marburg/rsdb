grammar DSL;

expression :  term (plus_minus=PLUS_MINUS term)*
           ;
           
term : factor (mul_div=MUL_DIV factor)*
     ;
     
factor : base=entity (POW exponent=entity)?
       ;                    
         
entity : '(' expression ')'
       | seq
       | constant
       | function 
       ;

seq : '[' seq_element (',' seq_element)* ']'
    ;
    
constant : PLUS_MINUS? INT('.'INT)?
		 ;           
       
function : ID ( '(' (expression ( ',' expression)*)? ')' )?
		 ;
		 
seq_element : expression
            | range
            ;
		 
range : min=ID ':' max=ID
      ;

INT : ('0'..'9')+ ;

ID : 'a'..'z' ('a'..'z' | '_' | '0'..'9')* ;

PLUS_MINUS : [+-] ;

MUL_DIV : [*/] ;

POW : '^' ;

WS : (' ' | '\t' | '\r' | 'n') -> skip ;