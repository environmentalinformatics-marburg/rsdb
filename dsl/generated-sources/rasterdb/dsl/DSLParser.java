// Generated from DSL.g4 by ANTLR 4.4
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
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__6=1, T__5=2, T__4=3, T__3=4, T__2=5, T__1=6, T__0=7, INT=8, ID=9, PLUS_MINUS=10, 
		MUL_DIV=11, WS=12;
	public static final String[] tokenNames = {
		"<INVALID>", "'('", "')'", "':'", "'['", "','", "']'", "'.'", "INT", "ID", 
		"PLUS_MINUS", "MUL_DIV", "WS"
	};
	public static final int
		RULE_expression = 0, RULE_term = 1, RULE_entity = 2, RULE_seq = 3, RULE_constant = 4, 
		RULE_function = 5, RULE_seq_element = 6, RULE_range = 7;
	public static final String[] ruleNames = {
		"expression", "term", "entity", "seq", "constant", "function", "seq_element", 
		"range"
	};

	@Override
	public String getGrammarFileName() { return "DSL.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

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
			enterOuterAlt(_localctx, 1);
			{
			setState(16); term();
			setState(21);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PLUS_MINUS) {
				{
				{
				setState(17); ((ExpressionContext)_localctx).plus_minus = match(PLUS_MINUS);
				setState(18); term();
				}
				}
				setState(23);
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

	public static class TermContext extends ParserRuleContext {
		public Token mul_div;
		public EntityContext entity(int i) {
			return getRuleContext(EntityContext.class,i);
		}
		public List<TerminalNode> MUL_DIV() { return getTokens(DSLParser.MUL_DIV); }
		public List<EntityContext> entity() {
			return getRuleContexts(EntityContext.class);
		}
		public TerminalNode MUL_DIV(int i) {
			return getToken(DSLParser.MUL_DIV, i);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(24); entity();
			setState(29);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==MUL_DIV) {
				{
				{
				setState(25); ((TermContext)_localctx).mul_div = match(MUL_DIV);
				setState(26); entity();
				}
				}
				setState(31);
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

	public static class EntityContext extends ParserRuleContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public SeqContext seq() {
			return getRuleContext(SeqContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
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
		try {
			setState(39);
			switch (_input.LA(1)) {
			case T__6:
				enterOuterAlt(_localctx, 1);
				{
				setState(32); match(T__6);
				setState(33); expression();
				setState(34); match(T__5);
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 2);
				{
				setState(36); seq();
				}
				break;
			case INT:
			case PLUS_MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(37); constant();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 4);
				{
				setState(38); function();
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
			enterOuterAlt(_localctx, 1);
			{
			setState(41); match(T__3);
			setState(42); seq_element();
			setState(47);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(43); match(T__2);
				setState(44); seq_element();
				}
				}
				setState(49);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(50); match(T__1);
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
		public TerminalNode INT(int i) {
			return getToken(DSLParser.INT, i);
		}
		public TerminalNode PLUS_MINUS() { return getToken(DSLParser.PLUS_MINUS, 0); }
		public List<TerminalNode> INT() { return getTokens(DSLParser.INT); }
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
			setState(53);
			_la = _input.LA(1);
			if (_la==PLUS_MINUS) {
				{
				setState(52); match(PLUS_MINUS);
				}
			}

			setState(55); match(INT);
			setState(58);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(56); match(T__0);
				setState(57); match(INT);
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
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(60); match(ID);
			setState(73);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(61); match(T__6);
				setState(70);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__6) | (1L << T__3) | (1L << INT) | (1L << ID) | (1L << PLUS_MINUS))) != 0)) {
					{
					setState(62); expression();
					setState(67);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__2) {
						{
						{
						setState(63); match(T__2);
						setState(64); expression();
						}
						}
						setState(69);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(72); match(T__5);
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
			setState(77);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(75); expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(76); range();
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(79); ((RangeContext)_localctx).min = match(ID);
			setState(80); match(T__4);
			setState(81); ((RangeContext)_localctx).max = match(ID);
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\16V\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\3\2\7\2\26"+
		"\n\2\f\2\16\2\31\13\2\3\3\3\3\3\3\7\3\36\n\3\f\3\16\3!\13\3\3\4\3\4\3"+
		"\4\3\4\3\4\3\4\3\4\5\4*\n\4\3\5\3\5\3\5\3\5\7\5\60\n\5\f\5\16\5\63\13"+
		"\5\3\5\3\5\3\6\5\68\n\6\3\6\3\6\3\6\5\6=\n\6\3\7\3\7\3\7\3\7\3\7\7\7D"+
		"\n\7\f\7\16\7G\13\7\5\7I\n\7\3\7\5\7L\n\7\3\b\3\b\5\bP\n\b\3\t\3\t\3\t"+
		"\3\t\3\t\2\2\n\2\4\6\b\n\f\16\20\2\2Y\2\22\3\2\2\2\4\32\3\2\2\2\6)\3\2"+
		"\2\2\b+\3\2\2\2\n\67\3\2\2\2\f>\3\2\2\2\16O\3\2\2\2\20Q\3\2\2\2\22\27"+
		"\5\4\3\2\23\24\7\f\2\2\24\26\5\4\3\2\25\23\3\2\2\2\26\31\3\2\2\2\27\25"+
		"\3\2\2\2\27\30\3\2\2\2\30\3\3\2\2\2\31\27\3\2\2\2\32\37\5\6\4\2\33\34"+
		"\7\r\2\2\34\36\5\6\4\2\35\33\3\2\2\2\36!\3\2\2\2\37\35\3\2\2\2\37 \3\2"+
		"\2\2 \5\3\2\2\2!\37\3\2\2\2\"#\7\3\2\2#$\5\2\2\2$%\7\4\2\2%*\3\2\2\2&"+
		"*\5\b\5\2\'*\5\n\6\2(*\5\f\7\2)\"\3\2\2\2)&\3\2\2\2)\'\3\2\2\2)(\3\2\2"+
		"\2*\7\3\2\2\2+,\7\6\2\2,\61\5\16\b\2-.\7\7\2\2.\60\5\16\b\2/-\3\2\2\2"+
		"\60\63\3\2\2\2\61/\3\2\2\2\61\62\3\2\2\2\62\64\3\2\2\2\63\61\3\2\2\2\64"+
		"\65\7\b\2\2\65\t\3\2\2\2\668\7\f\2\2\67\66\3\2\2\2\678\3\2\2\289\3\2\2"+
		"\29<\7\n\2\2:;\7\t\2\2;=\7\n\2\2<:\3\2\2\2<=\3\2\2\2=\13\3\2\2\2>K\7\13"+
		"\2\2?H\7\3\2\2@E\5\2\2\2AB\7\7\2\2BD\5\2\2\2CA\3\2\2\2DG\3\2\2\2EC\3\2"+
		"\2\2EF\3\2\2\2FI\3\2\2\2GE\3\2\2\2H@\3\2\2\2HI\3\2\2\2IJ\3\2\2\2JL\7\4"+
		"\2\2K?\3\2\2\2KL\3\2\2\2L\r\3\2\2\2MP\5\2\2\2NP\5\20\t\2OM\3\2\2\2ON\3"+
		"\2\2\2P\17\3\2\2\2QR\7\13\2\2RS\7\5\2\2ST\7\13\2\2T\21\3\2\2\2\f\27\37"+
		")\61\67<EHKO";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}