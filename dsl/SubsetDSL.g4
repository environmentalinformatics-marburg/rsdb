grammar SubsetDSL;

region_scirpt : WS? region_sequence WS? EOF
              ;

region_sequence : region (WS? SEPERATOR WS? region)*        
         ;
         
region : square
 	   | roi
 	   | bbox	
       ;
       
bbox : 'bbox(' WS? (point_sequence2 | region_sequence) WS? ')'
     ;      
       
square : 'square(' WS? point_sequence WS? ',' WS? edge=number WS? ')'
                ;
              
point_sequence : point (WS? SEPERATOR WS? point)*
               ;
               
point_sequence2 : point WS? SEPERATOR WS? point (WS? SEPERATOR WS? point)*
               ;               
               
number : INT('.'INT)? 
       ;
       
point : poi
      | p 
      ;      
              
poi : 'poi(' (WS? 'group='group=num_id WS? ',')? WS? url_sequence WS? ')'
      ;
      
roi : 'roi(' (WS? 'group='group=num_id WS? ',')? WS? url_sequence WS? ')'
    ;      
      
url_sequence : url (WS? SEPERATOR WS? url)*
      ;                              
                              
url : (group=num_id '/')? name=num_id
    ;
    
p : 'p(' WS? x=constant WS? ',' WS? y=constant WS? ')'
  ;
  
constant : PLUS_MINUS? INT('.'INT)?
         ;                    

num_id : INT 
       | ID 
       | (INT ID)
	   ;
 
ID : ('a'..'z' | 'A' .. 'Z')('a'..'z' | 'A' .. 'Z' | '0'..'9' | '_')* ;

WS : [ \t\r\n]+ ;

INT: ('0'..'9')+ ;

PLUS_MINUS : [+-] ;

SEPERATOR: '&' | '|' | ';';

