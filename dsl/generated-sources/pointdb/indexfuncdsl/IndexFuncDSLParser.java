// Generated from grammars/IndexFuncDSL.g4 by ANTLR 4.13.2
package pointdb.indexfuncdsl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class IndexFuncDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, PARAM_START=2, PARAM_END=3, PARAM_ASSIGNMENT=4, PARAM_SEPARATOR=5, 
		LETTER=6, UNDERSCORE=7, DIGIT=8, SIGN=9, DECIMAL_SEPARATOR=10;
	public static final int
		RULE_index_func = 0, RULE_param_sequence = 1, RULE_param = 2, RULE_id = 3, 
		RULE_value = 4, RULE_number = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"index_func", "param_sequence", "param", "id", "value", "number"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'('", "')'", "'='", "';'", null, "'_'", null, null, "'.'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "PARAM_START", "PARAM_END", "PARAM_ASSIGNMENT", "PARAM_SEPARATOR", 
			"LETTER", "UNDERSCORE", "DIGIT", "SIGN", "DECIMAL_SEPARATOR"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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
	public String getGrammarFileName() { return "IndexFuncDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public IndexFuncDSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Index_funcContext extends ParserRuleContext {
		public IdContext func_name;
		public Param_sequenceContext params;
		public TerminalNode EOF() { return getToken(IndexFuncDSLParser.EOF, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public TerminalNode PARAM_START() { return getToken(IndexFuncDSLParser.PARAM_START, 0); }
		public TerminalNode PARAM_END() { return getToken(IndexFuncDSLParser.PARAM_END, 0); }
		public Param_sequenceContext param_sequence() {
			return getRuleContext(Param_sequenceContext.class,0);
		}
		public Index_funcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_func; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitIndex_func(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Index_funcContext index_func() throws RecognitionException {
		Index_funcContext _localctx = new Index_funcContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_index_func);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(13);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(12);
				match(WS);
				}
			}

			setState(15);
			((Index_funcContext)_localctx).func_name = id();
			setState(17);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(16);
				match(WS);
				}
			}

			setState(33);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PARAM_START) {
				{
				setState(19);
				match(PARAM_START);
				setState(21);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
				case 1:
					{
					setState(20);
					match(WS);
					}
					break;
				}
				setState(24);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LETTER || _la==UNDERSCORE) {
					{
					setState(23);
					((Index_funcContext)_localctx).params = param_sequence();
					}
				}

				setState(27);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(26);
					match(WS);
					}
				}

				setState(29);
				match(PARAM_END);
				setState(31);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(30);
					match(WS);
					}
				}

				}
			}

			setState(35);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Param_sequenceContext extends ParserRuleContext {
		public List<ParamContext> param() {
			return getRuleContexts(ParamContext.class);
		}
		public ParamContext param(int i) {
			return getRuleContext(ParamContext.class,i);
		}
		public List<TerminalNode> PARAM_SEPARATOR() { return getTokens(IndexFuncDSLParser.PARAM_SEPARATOR); }
		public TerminalNode PARAM_SEPARATOR(int i) {
			return getToken(IndexFuncDSLParser.PARAM_SEPARATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public Param_sequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param_sequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitParam_sequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Param_sequenceContext param_sequence() throws RecognitionException {
		Param_sequenceContext _localctx = new Param_sequenceContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_param_sequence);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			param();
			setState(48);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(39);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(38);
						match(WS);
						}
					}

					setState(41);
					match(PARAM_SEPARATOR);
					setState(43);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(42);
						match(WS);
						}
					}

					setState(45);
					param();
					}
					} 
				}
				setState(50);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ParamContext extends ParserRuleContext {
		public IdContext param_name;
		public ValueContext param_value;
		public TerminalNode PARAM_ASSIGNMENT() { return getToken(IndexFuncDSLParser.PARAM_ASSIGNMENT, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public ParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamContext param() throws RecognitionException {
		ParamContext _localctx = new ParamContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_param);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			((ParamContext)_localctx).param_name = id();
			setState(53);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(52);
				match(WS);
				}
			}

			setState(55);
			match(PARAM_ASSIGNMENT);
			setState(57);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(56);
				match(WS);
				}
			}

			setState(59);
			((ParamContext)_localctx).param_value = value();
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdContext extends ParserRuleContext {
		public List<TerminalNode> LETTER() { return getTokens(IndexFuncDSLParser.LETTER); }
		public TerminalNode LETTER(int i) {
			return getToken(IndexFuncDSLParser.LETTER, i);
		}
		public List<TerminalNode> UNDERSCORE() { return getTokens(IndexFuncDSLParser.UNDERSCORE); }
		public TerminalNode UNDERSCORE(int i) {
			return getToken(IndexFuncDSLParser.UNDERSCORE, i);
		}
		public List<TerminalNode> DIGIT() { return getTokens(IndexFuncDSLParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(IndexFuncDSLParser.DIGIT, i);
		}
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_id);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(61);
			_la = _input.LA(1);
			if ( !(_la==LETTER || _la==UNDERSCORE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 448L) != 0)) {
				{
				{
				setState(62);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 448L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(67);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ValueContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_value);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			number();
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

	@SuppressWarnings("CheckReturnValue")
	public static class NumberContext extends ParserRuleContext {
		public TerminalNode SIGN() { return getToken(IndexFuncDSLParser.SIGN, 0); }
		public List<TerminalNode> DIGIT() { return getTokens(IndexFuncDSLParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(IndexFuncDSLParser.DIGIT, i);
		}
		public TerminalNode DECIMAL_SEPARATOR() { return getToken(IndexFuncDSLParser.DECIMAL_SEPARATOR, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IndexFuncDSLVisitor ) return ((IndexFuncDSLVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SIGN) {
				{
				setState(70);
				match(SIGN);
				}
			}

			setState(74); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(73);
				match(DIGIT);
				}
				}
				setState(76); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DIGIT );
			setState(84);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DECIMAL_SEPARATOR) {
				{
				setState(78);
				match(DECIMAL_SEPARATOR);
				setState(80); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(79);
					match(DIGIT);
					}
					}
					setState(82); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DIGIT );
				}
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

	public static final String _serializedATN =
		"\u0004\u0001\nW\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0003\u0000\u000e\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u0000\u0012\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000\u0016"+
		"\b\u0000\u0001\u0000\u0003\u0000\u0019\b\u0000\u0001\u0000\u0003\u0000"+
		"\u001c\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000 \b\u0000\u0003\u0000"+
		"\"\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003\u0001"+
		"(\b\u0001\u0001\u0001\u0001\u0001\u0003\u0001,\b\u0001\u0001\u0001\u0005"+
		"\u0001/\b\u0001\n\u0001\f\u00012\t\u0001\u0001\u0002\u0001\u0002\u0003"+
		"\u00026\b\u0002\u0001\u0002\u0001\u0002\u0003\u0002:\b\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0005\u0003@\b\u0003\n\u0003\f\u0003"+
		"C\t\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0003\u0005H\b\u0005\u0001"+
		"\u0005\u0004\u0005K\b\u0005\u000b\u0005\f\u0005L\u0001\u0005\u0001\u0005"+
		"\u0004\u0005Q\b\u0005\u000b\u0005\f\u0005R\u0003\u0005U\b\u0005\u0001"+
		"\u0005\u0000\u0000\u0006\u0000\u0002\u0004\u0006\b\n\u0000\u0002\u0001"+
		"\u0000\u0006\u0007\u0001\u0000\u0006\ba\u0000\r\u0001\u0000\u0000\u0000"+
		"\u0002%\u0001\u0000\u0000\u0000\u00043\u0001\u0000\u0000\u0000\u0006="+
		"\u0001\u0000\u0000\u0000\bD\u0001\u0000\u0000\u0000\nG\u0001\u0000\u0000"+
		"\u0000\f\u000e\u0005\u0001\u0000\u0000\r\f\u0001\u0000\u0000\u0000\r\u000e"+
		"\u0001\u0000\u0000\u0000\u000e\u000f\u0001\u0000\u0000\u0000\u000f\u0011"+
		"\u0003\u0006\u0003\u0000\u0010\u0012\u0005\u0001\u0000\u0000\u0011\u0010"+
		"\u0001\u0000\u0000\u0000\u0011\u0012\u0001\u0000\u0000\u0000\u0012!\u0001"+
		"\u0000\u0000\u0000\u0013\u0015\u0005\u0002\u0000\u0000\u0014\u0016\u0005"+
		"\u0001\u0000\u0000\u0015\u0014\u0001\u0000\u0000\u0000\u0015\u0016\u0001"+
		"\u0000\u0000\u0000\u0016\u0018\u0001\u0000\u0000\u0000\u0017\u0019\u0003"+
		"\u0002\u0001\u0000\u0018\u0017\u0001\u0000\u0000\u0000\u0018\u0019\u0001"+
		"\u0000\u0000\u0000\u0019\u001b\u0001\u0000\u0000\u0000\u001a\u001c\u0005"+
		"\u0001\u0000\u0000\u001b\u001a\u0001\u0000\u0000\u0000\u001b\u001c\u0001"+
		"\u0000\u0000\u0000\u001c\u001d\u0001\u0000\u0000\u0000\u001d\u001f\u0005"+
		"\u0003\u0000\u0000\u001e \u0005\u0001\u0000\u0000\u001f\u001e\u0001\u0000"+
		"\u0000\u0000\u001f \u0001\u0000\u0000\u0000 \"\u0001\u0000\u0000\u0000"+
		"!\u0013\u0001\u0000\u0000\u0000!\"\u0001\u0000\u0000\u0000\"#\u0001\u0000"+
		"\u0000\u0000#$\u0005\u0000\u0000\u0001$\u0001\u0001\u0000\u0000\u0000"+
		"%0\u0003\u0004\u0002\u0000&(\u0005\u0001\u0000\u0000\'&\u0001\u0000\u0000"+
		"\u0000\'(\u0001\u0000\u0000\u0000()\u0001\u0000\u0000\u0000)+\u0005\u0005"+
		"\u0000\u0000*,\u0005\u0001\u0000\u0000+*\u0001\u0000\u0000\u0000+,\u0001"+
		"\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-/\u0003\u0004\u0002\u0000"+
		".\'\u0001\u0000\u0000\u0000/2\u0001\u0000\u0000\u00000.\u0001\u0000\u0000"+
		"\u000001\u0001\u0000\u0000\u00001\u0003\u0001\u0000\u0000\u000020\u0001"+
		"\u0000\u0000\u000035\u0003\u0006\u0003\u000046\u0005\u0001\u0000\u0000"+
		"54\u0001\u0000\u0000\u000056\u0001\u0000\u0000\u000067\u0001\u0000\u0000"+
		"\u000079\u0005\u0004\u0000\u00008:\u0005\u0001\u0000\u000098\u0001\u0000"+
		"\u0000\u00009:\u0001\u0000\u0000\u0000:;\u0001\u0000\u0000\u0000;<\u0003"+
		"\b\u0004\u0000<\u0005\u0001\u0000\u0000\u0000=A\u0007\u0000\u0000\u0000"+
		">@\u0007\u0001\u0000\u0000?>\u0001\u0000\u0000\u0000@C\u0001\u0000\u0000"+
		"\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000B\u0007\u0001"+
		"\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000DE\u0003\n\u0005\u0000E\t"+
		"\u0001\u0000\u0000\u0000FH\u0005\t\u0000\u0000GF\u0001\u0000\u0000\u0000"+
		"GH\u0001\u0000\u0000\u0000HJ\u0001\u0000\u0000\u0000IK\u0005\b\u0000\u0000"+
		"JI\u0001\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000"+
		"\u0000LM\u0001\u0000\u0000\u0000MT\u0001\u0000\u0000\u0000NP\u0005\n\u0000"+
		"\u0000OQ\u0005\b\u0000\u0000PO\u0001\u0000\u0000\u0000QR\u0001\u0000\u0000"+
		"\u0000RP\u0001\u0000\u0000\u0000RS\u0001\u0000\u0000\u0000SU\u0001\u0000"+
		"\u0000\u0000TN\u0001\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000U\u000b"+
		"\u0001\u0000\u0000\u0000\u0011\r\u0011\u0015\u0018\u001b\u001f!\'+059"+
		"AGLRT";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}