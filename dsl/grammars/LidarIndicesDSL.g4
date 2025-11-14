grammar LidarIndicesDSL;

index_scirpt : WS? index_sequence WS? EOF
              ;

index_sequence : index (WS? SEPERATOR WS? index)*        
         ;
         
index : ID
      ;         
         
ID : ('a'..'z' | 'A' .. 'Z')('a'..'z' | 'A' .. 'Z' | '0'..'9' | '_')* ;

WS : [ \t\r\n]+ ;

SEPERATOR: '&' | '|' | ';';

