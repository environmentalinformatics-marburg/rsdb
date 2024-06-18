// Generated from IndexFuncDSL.g4 by ANTLR 4.4
package pointdb.subsetdsl;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IndexFuncDSLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, PARAM_START=2, PARAM_END=3, PARAM_ASSIGNMENT=4, PARAM_SEPARATOR=5, 
		LETTER=6, UNDERSCORE=7, DIGIT=8, SIGN=9, DECIMAL_SEPARATOR=10;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"'\\u0000'", "'\\u0001'", "'\\u0002'", "'\\u0003'", "'\\u0004'", "'\\u0005'", 
		"'\\u0006'", "'\\u0007'", "'\b'", "'\t'", "'\n'"
	};
	public static final String[] ruleNames = {
		"WS", "PARAM_START", "PARAM_END", "PARAM_ASSIGNMENT", "PARAM_SEPARATOR", 
		"LETTER", "UNDERSCORE", "DIGIT", "SIGN", "DECIMAL_SEPARATOR"
	};


	public IndexFuncDSLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "IndexFuncDSL.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\f.\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\3\2\6\2\31\n\2\r\2\16\2\32\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7"+
		"\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\2\2\f\3\3\5\4\7\5\t\6\13\7\r\b\17\t"+
		"\21\n\23\13\25\f\3\2\5\5\2\13\f\17\17\"\"\4\2C\\c|\4\2--//.\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3"+
		"\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\3\30\3\2\2\2\5\34\3\2\2"+
		"\2\7\36\3\2\2\2\t \3\2\2\2\13\"\3\2\2\2\r$\3\2\2\2\17&\3\2\2\2\21(\3\2"+
		"\2\2\23*\3\2\2\2\25,\3\2\2\2\27\31\t\2\2\2\30\27\3\2\2\2\31\32\3\2\2\2"+
		"\32\30\3\2\2\2\32\33\3\2\2\2\33\4\3\2\2\2\34\35\7*\2\2\35\6\3\2\2\2\36"+
		"\37\7+\2\2\37\b\3\2\2\2 !\7?\2\2!\n\3\2\2\2\"#\7=\2\2#\f\3\2\2\2$%\t\3"+
		"\2\2%\16\3\2\2\2&\'\7a\2\2\'\20\3\2\2\2()\4\62;\2)\22\3\2\2\2*+\t\4\2"+
		"\2+\24\3\2\2\2,-\7\60\2\2-\26\3\2\2\2\4\2\32\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}