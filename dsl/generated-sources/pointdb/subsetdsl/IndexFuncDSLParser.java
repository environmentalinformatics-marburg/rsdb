// Generated from IndexFuncDSL.g4 by ANTLR 4.4
package pointdb.subsetdsl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IndexFuncDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, PARAM_START=2, PARAM_END=3, PARAM_ASSIGNMENT=4, PARAM_SEPARATOR=5, 
		LETTER=6, UNDERSCORE=7, DIGIT=8, SIGN=9, DECIMAL_SEPARATOR=10;
	public static final String[] tokenNames = {
		"<INVALID>", "WS", "'('", "')'", "'='", "';'", "LETTER", "'_'", "DIGIT", 
		"SIGN", "'.'"
	};
	public static final int
		RULE_index_func = 0, RULE_param_sequence = 1, RULE_param = 2, RULE_id = 3, 
		RULE_value = 4, RULE_number = 5;
	public static final String[] ruleNames = {
		"index_func", "param_sequence", "param", "id", "value", "number"
	};

	@Override
	public String getGrammarFileName() { return "IndexFuncDSL.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

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
	public static class Index_funcContext extends ParserRuleContext {
		public IdContext func_name;
		public Param_sequenceContext params;
		public TerminalNode PARAM_START() { return getToken(IndexFuncDSLParser.PARAM_START, 0); }
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public TerminalNode EOF() { return getToken(IndexFuncDSLParser.EOF, 0); }
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public TerminalNode PARAM_END() { return getToken(IndexFuncDSLParser.PARAM_END, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
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
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(12); match(WS);
				}
			}

			setState(15); ((Index_funcContext)_localctx).func_name = id();
			setState(17);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(16); match(WS);
				}
			}

			setState(33);
			_la = _input.LA(1);
			if (_la==PARAM_START) {
				{
				setState(19); match(PARAM_START);
				setState(21);
				switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
				case 1:
					{
					setState(20); match(WS);
					}
					break;
				}
				setState(24);
				_la = _input.LA(1);
				if (_la==LETTER || _la==UNDERSCORE) {
					{
					setState(23); ((Index_funcContext)_localctx).params = param_sequence();
					}
				}

				setState(27);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(26); match(WS);
					}
				}

				setState(29); match(PARAM_END);
				setState(31);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(30); match(WS);
					}
				}

				}
			}

			setState(35); match(EOF);
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

	public static class Param_sequenceContext extends ParserRuleContext {
		public List<TerminalNode> PARAM_SEPARATOR() { return getTokens(IndexFuncDSLParser.PARAM_SEPARATOR); }
		public TerminalNode PARAM_SEPARATOR(int i) {
			return getToken(IndexFuncDSLParser.PARAM_SEPARATOR, i);
		}
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public List<ParamContext> param() {
			return getRuleContexts(ParamContext.class);
		}
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public ParamContext param(int i) {
			return getRuleContext(ParamContext.class,i);
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
			setState(37); param();
			setState(48);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(39);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(38); match(WS);
						}
					}

					setState(41); match(PARAM_SEPARATOR);
					setState(43);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(42); match(WS);
						}
					}

					setState(45); param();
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

	public static class ParamContext extends ParserRuleContext {
		public IdContext param_name;
		public ValueContext param_value;
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode WS(int i) {
			return getToken(IndexFuncDSLParser.WS, i);
		}
		public TerminalNode PARAM_ASSIGNMENT() { return getToken(IndexFuncDSLParser.PARAM_ASSIGNMENT, 0); }
		public List<TerminalNode> WS() { return getTokens(IndexFuncDSLParser.WS); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
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
			setState(51); ((ParamContext)_localctx).param_name = id();
			setState(53);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(52); match(WS);
				}
			}

			setState(55); match(PARAM_ASSIGNMENT);
			setState(57);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(56); match(WS);
				}
			}

			setState(59); ((ParamContext)_localctx).param_value = value();
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

	public static class IdContext extends ParserRuleContext {
		public List<TerminalNode> UNDERSCORE() { return getTokens(IndexFuncDSLParser.UNDERSCORE); }
		public List<TerminalNode> LETTER() { return getTokens(IndexFuncDSLParser.LETTER); }
		public TerminalNode DIGIT(int i) {
			return getToken(IndexFuncDSLParser.DIGIT, i);
		}
		public TerminalNode LETTER(int i) {
			return getToken(IndexFuncDSLParser.LETTER, i);
		}
		public List<TerminalNode> DIGIT() { return getTokens(IndexFuncDSLParser.DIGIT); }
		public TerminalNode UNDERSCORE(int i) {
			return getToken(IndexFuncDSLParser.UNDERSCORE, i);
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
			consume();
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LETTER) | (1L << UNDERSCORE) | (1L << DIGIT))) != 0)) {
				{
				{
				setState(62);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LETTER) | (1L << UNDERSCORE) | (1L << DIGIT))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
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
			setState(68); number();
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

	public static class NumberContext extends ParserRuleContext {
		public TerminalNode DECIMAL_SEPARATOR() { return getToken(IndexFuncDSLParser.DECIMAL_SEPARATOR, 0); }
		public TerminalNode DIGIT(int i) {
			return getToken(IndexFuncDSLParser.DIGIT, i);
		}
		public List<TerminalNode> DIGIT() { return getTokens(IndexFuncDSLParser.DIGIT); }
		public TerminalNode SIGN() { return getToken(IndexFuncDSLParser.SIGN, 0); }
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
			_la = _input.LA(1);
			if (_la==SIGN) {
				{
				setState(70); match(SIGN);
				}
			}

			setState(74); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(73); match(DIGIT);
				}
				}
				setState(76); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DIGIT );
			setState(84);
			_la = _input.LA(1);
			if (_la==DECIMAL_SEPARATOR) {
				{
				setState(78); match(DECIMAL_SEPARATOR);
				setState(80); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(79); match(DIGIT);
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\fY\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\5\2\20\n\2\3\2\3\2\5\2\24\n\2\3"+
		"\2\3\2\5\2\30\n\2\3\2\5\2\33\n\2\3\2\5\2\36\n\2\3\2\3\2\5\2\"\n\2\5\2"+
		"$\n\2\3\2\3\2\3\3\3\3\5\3*\n\3\3\3\3\3\5\3.\n\3\3\3\7\3\61\n\3\f\3\16"+
		"\3\64\13\3\3\4\3\4\5\48\n\4\3\4\3\4\5\4<\n\4\3\4\3\4\3\5\3\5\7\5B\n\5"+
		"\f\5\16\5E\13\5\3\6\3\6\3\7\5\7J\n\7\3\7\6\7M\n\7\r\7\16\7N\3\7\3\7\6"+
		"\7S\n\7\r\7\16\7T\5\7W\n\7\3\7\2\2\b\2\4\6\b\n\f\2\4\3\2\b\t\3\2\b\nc"+
		"\2\17\3\2\2\2\4\'\3\2\2\2\6\65\3\2\2\2\b?\3\2\2\2\nF\3\2\2\2\fI\3\2\2"+
		"\2\16\20\7\3\2\2\17\16\3\2\2\2\17\20\3\2\2\2\20\21\3\2\2\2\21\23\5\b\5"+
		"\2\22\24\7\3\2\2\23\22\3\2\2\2\23\24\3\2\2\2\24#\3\2\2\2\25\27\7\4\2\2"+
		"\26\30\7\3\2\2\27\26\3\2\2\2\27\30\3\2\2\2\30\32\3\2\2\2\31\33\5\4\3\2"+
		"\32\31\3\2\2\2\32\33\3\2\2\2\33\35\3\2\2\2\34\36\7\3\2\2\35\34\3\2\2\2"+
		"\35\36\3\2\2\2\36\37\3\2\2\2\37!\7\5\2\2 \"\7\3\2\2! \3\2\2\2!\"\3\2\2"+
		"\2\"$\3\2\2\2#\25\3\2\2\2#$\3\2\2\2$%\3\2\2\2%&\7\2\2\3&\3\3\2\2\2\'\62"+
		"\5\6\4\2(*\7\3\2\2)(\3\2\2\2)*\3\2\2\2*+\3\2\2\2+-\7\7\2\2,.\7\3\2\2-"+
		",\3\2\2\2-.\3\2\2\2./\3\2\2\2/\61\5\6\4\2\60)\3\2\2\2\61\64\3\2\2\2\62"+
		"\60\3\2\2\2\62\63\3\2\2\2\63\5\3\2\2\2\64\62\3\2\2\2\65\67\5\b\5\2\66"+
		"8\7\3\2\2\67\66\3\2\2\2\678\3\2\2\289\3\2\2\29;\7\6\2\2:<\7\3\2\2;:\3"+
		"\2\2\2;<\3\2\2\2<=\3\2\2\2=>\5\n\6\2>\7\3\2\2\2?C\t\2\2\2@B\t\3\2\2A@"+
		"\3\2\2\2BE\3\2\2\2CA\3\2\2\2CD\3\2\2\2D\t\3\2\2\2EC\3\2\2\2FG\5\f\7\2"+
		"G\13\3\2\2\2HJ\7\13\2\2IH\3\2\2\2IJ\3\2\2\2JL\3\2\2\2KM\7\n\2\2LK\3\2"+
		"\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2\2OV\3\2\2\2PR\7\f\2\2QS\7\n\2\2RQ\3\2"+
		"\2\2ST\3\2\2\2TR\3\2\2\2TU\3\2\2\2UW\3\2\2\2VP\3\2\2\2VW\3\2\2\2W\r\3"+
		"\2\2\2\23\17\23\27\32\35!#)-\62\67;CINTV";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}