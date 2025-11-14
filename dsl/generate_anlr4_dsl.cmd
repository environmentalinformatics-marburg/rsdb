@echo Generate java source code with antlr4 from g4 grammar files in grammars folder with generated files in generated-source folder.

java -jar antlr-4.13.2-complete.jar -no-listener -visitor -encoding UTF-8 -package rasterdb.dsl -o generated-sources/rasterdb/dsl grammars/DSL.g4
java -jar antlr-4.13.2-complete.jar -no-listener -visitor -encoding UTF-8 -package pointdb.lidarindicesdsl -o generated-sources/pointdb/lidarindicesdsl grammars/LidarIndicesDSL.g4
java -jar antlr-4.13.2-complete.jar -no-listener -visitor -encoding UTF-8 -package pointdb.subsetdsl -o generated-sources/pointdb/subsetdsl grammars/SubsetDSL.g4
java -jar antlr-4.13.2-complete.jar -no-listener -visitor -encoding UTF-8 -package pointdb.indexfuncdsl -o generated-sources/pointdb/indexfuncdsl grammars/IndexFuncDSL.g4
