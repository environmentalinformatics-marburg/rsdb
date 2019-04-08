// Generated from SubsetDSL.g4 by ANTLR 4.7
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
public class SubsetDSLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, ID=11, WS=12, INT=13, PLUS_MINUS=14, SEPERATOR=15;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "ID", "WS", "INT", "PLUS_MINUS", "SEPERATOR"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'bbox('", "')'", "'square('", "','", "'.'", "'poi('", "'group='", 
		"'roi('", "'/'", "'p('"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, "ID", 
		"WS", "INT", "PLUS_MINUS", "SEPERATOR"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public SubsetDSLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "SubsetDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\21`\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3"+
		"\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\13"+
		"\3\13\3\13\3\f\3\f\7\fN\n\f\f\f\16\fQ\13\f\3\r\6\rT\n\r\r\r\16\rU\3\16"+
		"\6\16Y\n\16\r\16\16\16Z\3\17\3\17\3\20\3\20\2\2\21\3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21\3\2\7\4\2C\\"+
		"c|\6\2\62;C\\aac|\5\2\13\f\17\17\"\"\4\2--//\5\2((==~~\2b\2\3\3\2\2\2"+
		"\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2"+
		"\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2"+
		"\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\3!\3\2\2\2\5\'\3\2\2\2\7)\3\2"+
		"\2\2\t\61\3\2\2\2\13\63\3\2\2\2\r\65\3\2\2\2\17:\3\2\2\2\21A\3\2\2\2\23"+
		"F\3\2\2\2\25H\3\2\2\2\27K\3\2\2\2\31S\3\2\2\2\33X\3\2\2\2\35\\\3\2\2\2"+
		"\37^\3\2\2\2!\"\7d\2\2\"#\7d\2\2#$\7q\2\2$%\7z\2\2%&\7*\2\2&\4\3\2\2\2"+
		"\'(\7+\2\2(\6\3\2\2\2)*\7u\2\2*+\7s\2\2+,\7w\2\2,-\7c\2\2-.\7t\2\2./\7"+
		"g\2\2/\60\7*\2\2\60\b\3\2\2\2\61\62\7.\2\2\62\n\3\2\2\2\63\64\7\60\2\2"+
		"\64\f\3\2\2\2\65\66\7r\2\2\66\67\7q\2\2\678\7k\2\289\7*\2\29\16\3\2\2"+
		"\2:;\7i\2\2;<\7t\2\2<=\7q\2\2=>\7w\2\2>?\7r\2\2?@\7?\2\2@\20\3\2\2\2A"+
		"B\7t\2\2BC\7q\2\2CD\7k\2\2DE\7*\2\2E\22\3\2\2\2FG\7\61\2\2G\24\3\2\2\2"+
		"HI\7r\2\2IJ\7*\2\2J\26\3\2\2\2KO\t\2\2\2LN\t\3\2\2ML\3\2\2\2NQ\3\2\2\2"+
		"OM\3\2\2\2OP\3\2\2\2P\30\3\2\2\2QO\3\2\2\2RT\t\4\2\2SR\3\2\2\2TU\3\2\2"+
		"\2US\3\2\2\2UV\3\2\2\2V\32\3\2\2\2WY\4\62;\2XW\3\2\2\2YZ\3\2\2\2ZX\3\2"+
		"\2\2Z[\3\2\2\2[\34\3\2\2\2\\]\t\5\2\2]\36\3\2\2\2^_\t\6\2\2_ \3\2\2\2"+
		"\6\2OUZ\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}