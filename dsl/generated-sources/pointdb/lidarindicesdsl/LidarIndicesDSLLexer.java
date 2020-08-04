// Generated from LidarIndicesDSL.g4 by ANTLR 4.4
package pointdb.lidarindicesdsl;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LidarIndicesDSLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ID=1, WS=2, SEPERATOR=3;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"'\\u0000'", "'\\u0001'", "'\\u0002'", "'\\u0003'"
	};
	public static final String[] ruleNames = {
		"ID", "WS", "SEPERATOR"
	};


	public LidarIndicesDSLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "LidarIndicesDSL.g4"; }

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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\5\27\b\1\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\3\2\3\2\7\2\f\n\2\f\2\16\2\17\13\2\3\3\6\3\22\n\3\r\3"+
		"\16\3\23\3\4\3\4\2\2\5\3\3\5\4\7\5\3\2\6\4\2C\\c|\6\2\62;C\\aac|\5\2\13"+
		"\f\17\17\"\"\5\2((==~~\30\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\3\t\3\2"+
		"\2\2\5\21\3\2\2\2\7\25\3\2\2\2\t\r\t\2\2\2\n\f\t\3\2\2\13\n\3\2\2\2\f"+
		"\17\3\2\2\2\r\13\3\2\2\2\r\16\3\2\2\2\16\4\3\2\2\2\17\r\3\2\2\2\20\22"+
		"\t\4\2\2\21\20\3\2\2\2\22\23\3\2\2\2\23\21\3\2\2\2\23\24\3\2\2\2\24\6"+
		"\3\2\2\2\25\26\t\5\2\2\26\b\3\2\2\2\5\2\r\23\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}