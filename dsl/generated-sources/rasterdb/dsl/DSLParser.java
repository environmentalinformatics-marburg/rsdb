// Generated from DSL.g4 by ANTLR 4.7
package rasterdb.dsl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, INT=8, WS=9, ID=10, 
		PLUS_MINUS=11, MUL_DIV=12;
	public static final int
		RULE_expression = 0, RULE_term = 1, RULE_entity = 2, RULE_seq = 3, RULE_constant = 4, 
		RULE_function = 5, RULE_seq_element = 6, RULE_range = 7;
	public static final String[] ruleNames = {
		"expression", "term", "entity", "seq", "constant", "function", "seq_element", 
		"range"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'['", "','", "']'", "'.'", "':'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, "INT", "WS", "ID", "PLUS_MINUS", 
		"MUL_DIV"
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
	public String getGrammarFileName() { return "DSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ExpressionContext extends ParserRuleContext {
		public Token plus_minus;
		public List<TermContext> term() {
			return getRuleContexts(TermContext.class);
		}
		public TermContext term(int i) {
			return getRuleContext(TermContext.class,i);
		}
		public List<TerminalNode> PLUS_MINUS() { return getTokens(DSLParser.PLUS_MINUS); }
		public TerminalNode PLUS_MINUS(int i) {
			return getToken(DSLParser.PLUS_MINUS, i);
		}
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			term();
			setState(27);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(18);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(17);
						match(WS);
						}
					}

					setState(20);
					((ExpressionContext)_localctx).plus_minus = match(PLUS_MINUS);
					setState(22);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(21);
						match(WS);
						}
					}

					setState(24);
					term();
					}
					} 
				}
				setState(29);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
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

	public static class TermContext extends ParserRuleContext {
		public Token mul_div;
		public List<EntityContext> entity() {
			return getRuleContexts(EntityContext.class);
		}
		public EntityContext entity(int i) {
			return getRuleContext(EntityContext.class,i);
		}
		public List<TerminalNode> MUL_DIV() { return getTokens(DSLParser.MUL_DIV); }
		public TerminalNode MUL_DIV(int i) {
			return getToken(DSLParser.MUL_DIV, i);
		}
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public TermContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_term; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitTerm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TermContext term() throws RecognitionException {
		TermContext _localctx = new TermContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_term);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			entity();
			setState(41);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(32);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(31);
						match(WS);
						}
					}

					setState(34);
					((TermContext)_localctx).mul_div = match(MUL_DIV);
					setState(36);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(35);
						match(WS);
						}
					}

					setState(38);
					entity();
					}
					} 
				}
				setState(43);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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

	public static class EntityContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public SeqContext seq() {
			return getRuleContext(SeqContext.class,0);
		}
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public EntityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entity; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitEntity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityContext entity() throws RecognitionException {
		EntityContext _localctx = new EntityContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_entity);
		int _la;
		try {
			setState(57);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				match(T__0);
				setState(46);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(45);
					match(WS);
					}
				}

				setState(48);
				expression();
				setState(50);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(49);
					match(WS);
					}
				}

				setState(52);
				match(T__1);
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				seq();
				}
				break;
			case INT:
			case PLUS_MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(55);
				constant();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 4);
				{
				setState(56);
				function();
				}
				break;
			default:
				throw new NoViableAltException(this);
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

	public static class SeqContext extends ParserRuleContext {
		public List<Seq_elementContext> seq_element() {
			return getRuleContexts(Seq_elementContext.class);
		}
		public Seq_elementContext seq_element(int i) {
			return getRuleContext(Seq_elementContext.class,i);
		}
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public SeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_seq; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SeqContext seq() throws RecognitionException {
		SeqContext _localctx = new SeqContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_seq);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(T__2);
			setState(61);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(60);
				match(WS);
				}
			}

			setState(63);
			seq_element();
			setState(74);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(65);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(64);
						match(WS);
						}
					}

					setState(67);
					match(T__3);
					setState(69);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(68);
						match(WS);
						}
					}

					setState(71);
					seq_element();
					}
					} 
				}
				setState(76);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			}
			setState(78);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(77);
				match(WS);
				}
			}

			setState(80);
			match(T__4);
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

	public static class ConstantContext extends ParserRuleContext {
		public List<TerminalNode> INT() { return getTokens(DSLParser.INT); }
		public TerminalNode INT(int i) {
			return getToken(DSLParser.INT, i);
		}
		public TerminalNode PLUS_MINUS() { return getToken(DSLParser.PLUS_MINUS, 0); }
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_constant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS_MINUS) {
				{
				setState(82);
				match(PLUS_MINUS);
				}
			}

			setState(85);
			match(INT);
			setState(88);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(86);
				match(T__5);
				setState(87);
				match(INT);
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

	public static class FunctionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DSLParser.ID, 0); }
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_function);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(ID);
			setState(115);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(91);
				match(T__0);
				setState(93);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(92);
					match(WS);
					}
					break;
				}
				setState(109);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << INT) | (1L << ID) | (1L << PLUS_MINUS))) != 0)) {
					{
					setState(95);
					expression();
					setState(106);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
					while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1 ) {
							{
							{
							setState(97);
							_errHandler.sync(this);
							_la = _input.LA(1);
							if (_la==WS) {
								{
								setState(96);
								match(WS);
								}
							}

							setState(99);
							match(T__3);
							setState(101);
							_errHandler.sync(this);
							_la = _input.LA(1);
							if (_la==WS) {
								{
								setState(100);
								match(WS);
								}
							}

							setState(103);
							expression();
							}
							} 
						}
						setState(108);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
					}
					}
				}

				setState(112);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(111);
					match(WS);
					}
				}

				setState(114);
				match(T__1);
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

	public static class Seq_elementContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public RangeContext range() {
			return getRuleContext(RangeContext.class,0);
		}
		public Seq_elementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_seq_element; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitSeq_element(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Seq_elementContext seq_element() throws RecognitionException {
		Seq_elementContext _localctx = new Seq_elementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_seq_element);
		try {
			setState(119);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(117);
				expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(118);
				range();
				}
				break;
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

	public static class RangeContext extends ParserRuleContext {
		public Token min;
		public Token max;
		public List<TerminalNode> ID() { return getTokens(DSLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(DSLParser.ID, i);
		}
		public List<TerminalNode> WS() { return getTokens(DSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(DSLParser.WS, i);
		}
		public RangeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_range; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DSLVisitor ) return ((DSLVisitor<? extends T>)visitor).visitRange(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RangeContext range() throws RecognitionException {
		RangeContext _localctx = new RangeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_range);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			((RangeContext)_localctx).min = match(ID);
			setState(123);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(122);
				match(WS);
				}
			}

			setState(125);
			match(T__6);
			setState(127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(126);
				match(WS);
				}
			}

			setState(129);
			((RangeContext)_localctx).max = match(ID);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\16\u0086\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\5\2\25"+
		"\n\2\3\2\3\2\5\2\31\n\2\3\2\7\2\34\n\2\f\2\16\2\37\13\2\3\3\3\3\5\3#\n"+
		"\3\3\3\3\3\5\3\'\n\3\3\3\7\3*\n\3\f\3\16\3-\13\3\3\4\3\4\5\4\61\n\4\3"+
		"\4\3\4\5\4\65\n\4\3\4\3\4\3\4\3\4\3\4\5\4<\n\4\3\5\3\5\5\5@\n\5\3\5\3"+
		"\5\5\5D\n\5\3\5\3\5\5\5H\n\5\3\5\7\5K\n\5\f\5\16\5N\13\5\3\5\5\5Q\n\5"+
		"\3\5\3\5\3\6\5\6V\n\6\3\6\3\6\3\6\5\6[\n\6\3\7\3\7\3\7\5\7`\n\7\3\7\3"+
		"\7\5\7d\n\7\3\7\3\7\5\7h\n\7\3\7\7\7k\n\7\f\7\16\7n\13\7\5\7p\n\7\3\7"+
		"\5\7s\n\7\3\7\5\7v\n\7\3\b\3\b\5\bz\n\b\3\t\3\t\5\t~\n\t\3\t\3\t\5\t\u0082"+
		"\n\t\3\t\3\t\3\t\2\2\n\2\4\6\b\n\f\16\20\2\2\2\u0099\2\22\3\2\2\2\4 \3"+
		"\2\2\2\6;\3\2\2\2\b=\3\2\2\2\nU\3\2\2\2\f\\\3\2\2\2\16y\3\2\2\2\20{\3"+
		"\2\2\2\22\35\5\4\3\2\23\25\7\13\2\2\24\23\3\2\2\2\24\25\3\2\2\2\25\26"+
		"\3\2\2\2\26\30\7\r\2\2\27\31\7\13\2\2\30\27\3\2\2\2\30\31\3\2\2\2\31\32"+
		"\3\2\2\2\32\34\5\4\3\2\33\24\3\2\2\2\34\37\3\2\2\2\35\33\3\2\2\2\35\36"+
		"\3\2\2\2\36\3\3\2\2\2\37\35\3\2\2\2 +\5\6\4\2!#\7\13\2\2\"!\3\2\2\2\""+
		"#\3\2\2\2#$\3\2\2\2$&\7\16\2\2%\'\7\13\2\2&%\3\2\2\2&\'\3\2\2\2\'(\3\2"+
		"\2\2(*\5\6\4\2)\"\3\2\2\2*-\3\2\2\2+)\3\2\2\2+,\3\2\2\2,\5\3\2\2\2-+\3"+
		"\2\2\2.\60\7\3\2\2/\61\7\13\2\2\60/\3\2\2\2\60\61\3\2\2\2\61\62\3\2\2"+
		"\2\62\64\5\2\2\2\63\65\7\13\2\2\64\63\3\2\2\2\64\65\3\2\2\2\65\66\3\2"+
		"\2\2\66\67\7\4\2\2\67<\3\2\2\28<\5\b\5\29<\5\n\6\2:<\5\f\7\2;.\3\2\2\2"+
		";8\3\2\2\2;9\3\2\2\2;:\3\2\2\2<\7\3\2\2\2=?\7\5\2\2>@\7\13\2\2?>\3\2\2"+
		"\2?@\3\2\2\2@A\3\2\2\2AL\5\16\b\2BD\7\13\2\2CB\3\2\2\2CD\3\2\2\2DE\3\2"+
		"\2\2EG\7\6\2\2FH\7\13\2\2GF\3\2\2\2GH\3\2\2\2HI\3\2\2\2IK\5\16\b\2JC\3"+
		"\2\2\2KN\3\2\2\2LJ\3\2\2\2LM\3\2\2\2MP\3\2\2\2NL\3\2\2\2OQ\7\13\2\2PO"+
		"\3\2\2\2PQ\3\2\2\2QR\3\2\2\2RS\7\7\2\2S\t\3\2\2\2TV\7\r\2\2UT\3\2\2\2"+
		"UV\3\2\2\2VW\3\2\2\2WZ\7\n\2\2XY\7\b\2\2Y[\7\n\2\2ZX\3\2\2\2Z[\3\2\2\2"+
		"[\13\3\2\2\2\\u\7\f\2\2]_\7\3\2\2^`\7\13\2\2_^\3\2\2\2_`\3\2\2\2`o\3\2"+
		"\2\2al\5\2\2\2bd\7\13\2\2cb\3\2\2\2cd\3\2\2\2de\3\2\2\2eg\7\6\2\2fh\7"+
		"\13\2\2gf\3\2\2\2gh\3\2\2\2hi\3\2\2\2ik\5\2\2\2jc\3\2\2\2kn\3\2\2\2lj"+
		"\3\2\2\2lm\3\2\2\2mp\3\2\2\2nl\3\2\2\2oa\3\2\2\2op\3\2\2\2pr\3\2\2\2q"+
		"s\7\13\2\2rq\3\2\2\2rs\3\2\2\2st\3\2\2\2tv\7\4\2\2u]\3\2\2\2uv\3\2\2\2"+
		"v\r\3\2\2\2wz\5\2\2\2xz\5\20\t\2yw\3\2\2\2yx\3\2\2\2z\17\3\2\2\2{}\7\f"+
		"\2\2|~\7\13\2\2}|\3\2\2\2}~\3\2\2\2~\177\3\2\2\2\177\u0081\7\t\2\2\u0080"+
		"\u0082\7\13\2\2\u0081\u0080\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0083\3"+
		"\2\2\2\u0083\u0084\7\f\2\2\u0084\21\3\2\2\2\34\24\30\35\"&+\60\64;?CG"+
		"LPUZ_cgloruy}\u0081";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}