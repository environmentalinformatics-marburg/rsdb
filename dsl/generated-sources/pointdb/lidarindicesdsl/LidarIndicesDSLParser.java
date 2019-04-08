// Generated from LidarIndicesDSL.g4 by ANTLR 4.7
package pointdb.lidarindicesdsl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LidarIndicesDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ID=1, WS=2, SEPERATOR=3;
	public static final int
		RULE_index_scirpt = 0, RULE_index_sequence = 1, RULE_index = 2;
	public static final String[] ruleNames = {
		"index_scirpt", "index_sequence", "index"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "ID", "WS", "SEPERATOR"
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

	@Override
	public String getGrammarFileName() { return "LidarIndicesDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public LidarIndicesDSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class Index_scirptContext extends ParserRuleContext {
		public Index_sequenceContext index_sequence() {
			return getRuleContext(Index_sequenceContext.class,0);
		}
		public TerminalNode EOF() { return getToken(LidarIndicesDSLParser.EOF, 0); }
		public List<TerminalNode> WS() { return getTokens(LidarIndicesDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(LidarIndicesDSLParser.WS, i);
		}
		public Index_scirptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_scirpt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LidarIndicesDSLVisitor ) return ((LidarIndicesDSLVisitor<? extends T>)visitor).visitIndex_scirpt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Index_scirptContext index_scirpt() throws RecognitionException {
		Index_scirptContext _localctx = new Index_scirptContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_index_scirpt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(7);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(6);
				match(WS);
				}
			}

			setState(9);
			index_sequence();
			setState(11);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(10);
				match(WS);
				}
			}

			setState(13);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Index_sequenceContext extends ParserRuleContext {
		public List<IndexContext> index() {
			return getRuleContexts(IndexContext.class);
		}
		public IndexContext index(int i) {
			return getRuleContext(IndexContext.class,i);
		}
		public List<TerminalNode> SEPERATOR() { return getTokens(LidarIndicesDSLParser.SEPERATOR); }
		public TerminalNode SEPERATOR(int i) {
			return getToken(LidarIndicesDSLParser.SEPERATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(LidarIndicesDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(LidarIndicesDSLParser.WS, i);
		}
		public Index_sequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_sequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LidarIndicesDSLVisitor ) return ((LidarIndicesDSLVisitor<? extends T>)visitor).visitIndex_sequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Index_sequenceContext index_sequence() throws RecognitionException {
		Index_sequenceContext _localctx = new Index_sequenceContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_index_sequence);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(15);
			index();
			setState(26);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(17);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(16);
						match(WS);
						}
					}

					setState(19);
					match(SEPERATOR);
					setState(21);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(20);
						match(WS);
						}
					}

					setState(23);
					index();
					}
					} 
				}
				setState(28);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IndexContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(LidarIndicesDSLParser.ID, 0); }
		public IndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LidarIndicesDSLVisitor ) return ((LidarIndicesDSLVisitor<? extends T>)visitor).visitIndex(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexContext index() throws RecognitionException {
		IndexContext _localctx = new IndexContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_index);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\5\"\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\3\2\5\2\n\n\2\3\2\3\2\5\2\16\n\2\3\2\3\2\3\3\3\3\5\3\24\n\3"+
		"\3\3\3\3\5\3\30\n\3\3\3\7\3\33\n\3\f\3\16\3\36\13\3\3\4\3\4\3\4\2\2\5"+
		"\2\4\6\2\2\2#\2\t\3\2\2\2\4\21\3\2\2\2\6\37\3\2\2\2\b\n\7\4\2\2\t\b\3"+
		"\2\2\2\t\n\3\2\2\2\n\13\3\2\2\2\13\r\5\4\3\2\f\16\7\4\2\2\r\f\3\2\2\2"+
		"\r\16\3\2\2\2\16\17\3\2\2\2\17\20\7\2\2\3\20\3\3\2\2\2\21\34\5\6\4\2\22"+
		"\24\7\4\2\2\23\22\3\2\2\2\23\24\3\2\2\2\24\25\3\2\2\2\25\27\7\5\2\2\26"+
		"\30\7\4\2\2\27\26\3\2\2\2\27\30\3\2\2\2\30\31\3\2\2\2\31\33\5\6\4\2\32"+
		"\23\3\2\2\2\33\36\3\2\2\2\34\32\3\2\2\2\34\35\3\2\2\2\35\5\3\2\2\2\36"+
		"\34\3\2\2\2\37 \7\3\2\2 \7\3\2\2\2\7\t\r\23\27\34";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}