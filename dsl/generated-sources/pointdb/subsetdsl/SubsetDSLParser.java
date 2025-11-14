// Generated from grammars/SubsetDSL.g4 by ANTLR 4.13.2
package pointdb.subsetdsl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class SubsetDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, ID=11, WS=12, INT=13, PLUS_MINUS=14, SEPERATOR=15;
	public static final int
		RULE_region_scirpt = 0, RULE_region_sequence = 1, RULE_region = 2, RULE_bbox = 3, 
		RULE_square = 4, RULE_point_sequence = 5, RULE_point_sequence2 = 6, RULE_number = 7, 
		RULE_point = 8, RULE_poi = 9, RULE_roi = 10, RULE_url_sequence = 11, RULE_url = 12, 
		RULE_p = 13, RULE_constant = 14, RULE_num_id = 15;
	private static String[] makeRuleNames() {
		return new String[] {
			"region_scirpt", "region_sequence", "region", "bbox", "square", "point_sequence", 
			"point_sequence2", "number", "point", "poi", "roi", "url_sequence", "url", 
			"p", "constant", "num_id"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'bbox('", "')'", "'square('", "','", "'.'", "'poi('", "'group='", 
			"'roi('", "'/'", "'p('"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, "ID", 
			"WS", "INT", "PLUS_MINUS", "SEPERATOR"
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
	public String getGrammarFileName() { return "SubsetDSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SubsetDSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Region_scirptContext extends ParserRuleContext {
		public Region_sequenceContext region_sequence() {
			return getRuleContext(Region_sequenceContext.class,0);
		}
		public TerminalNode EOF() { return getToken(SubsetDSLParser.EOF, 0); }
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Region_scirptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_region_scirpt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitRegion_scirpt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Region_scirptContext region_scirpt() throws RecognitionException {
		Region_scirptContext _localctx = new Region_scirptContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_region_scirpt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(32);
				match(WS);
				}
			}

			setState(35);
			region_sequence();
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(36);
				match(WS);
				}
			}

			setState(39);
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
	public static class Region_sequenceContext extends ParserRuleContext {
		public List<RegionContext> region() {
			return getRuleContexts(RegionContext.class);
		}
		public RegionContext region(int i) {
			return getRuleContext(RegionContext.class,i);
		}
		public List<TerminalNode> SEPERATOR() { return getTokens(SubsetDSLParser.SEPERATOR); }
		public TerminalNode SEPERATOR(int i) {
			return getToken(SubsetDSLParser.SEPERATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Region_sequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_region_sequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitRegion_sequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Region_sequenceContext region_sequence() throws RecognitionException {
		Region_sequenceContext _localctx = new Region_sequenceContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_region_sequence);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			region();
			setState(52);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
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
					match(SEPERATOR);
					setState(47);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(46);
						match(WS);
						}
					}

					setState(49);
					region();
					}
					} 
				}
				setState(54);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RegionContext extends ParserRuleContext {
		public SquareContext square() {
			return getRuleContext(SquareContext.class,0);
		}
		public RoiContext roi() {
			return getRuleContext(RoiContext.class,0);
		}
		public BboxContext bbox() {
			return getRuleContext(BboxContext.class,0);
		}
		public RegionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_region; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitRegion(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegionContext region() throws RecognitionException {
		RegionContext _localctx = new RegionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_region);
		try {
			setState(58);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(55);
				square();
				}
				break;
			case T__7:
				enterOuterAlt(_localctx, 2);
				{
				setState(56);
				roi();
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 3);
				{
				setState(57);
				bbox();
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

	@SuppressWarnings("CheckReturnValue")
	public static class BboxContext extends ParserRuleContext {
		public Point_sequence2Context point_sequence2() {
			return getRuleContext(Point_sequence2Context.class,0);
		}
		public Region_sequenceContext region_sequence() {
			return getRuleContext(Region_sequenceContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public BboxContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bbox; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitBbox(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BboxContext bbox() throws RecognitionException {
		BboxContext _localctx = new BboxContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_bbox);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(60);
			match(T__0);
			setState(62);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(61);
				match(WS);
				}
			}

			setState(66);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__5:
			case T__9:
				{
				setState(64);
				point_sequence2();
				}
				break;
			case T__0:
			case T__2:
			case T__7:
				{
				setState(65);
				region_sequence();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
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
			match(T__1);
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
	public static class SquareContext extends ParserRuleContext {
		public NumberContext edge;
		public Point_sequenceContext point_sequence() {
			return getRuleContext(Point_sequenceContext.class,0);
		}
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public SquareContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_square; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitSquare(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SquareContext square() throws RecognitionException {
		SquareContext _localctx = new SquareContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_square);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(T__2);
			setState(75);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(74);
				match(WS);
				}
			}

			setState(77);
			point_sequence();
			setState(79);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(78);
				match(WS);
				}
			}

			setState(81);
			match(T__3);
			setState(83);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(82);
				match(WS);
				}
			}

			setState(85);
			((SquareContext)_localctx).edge = number();
			setState(87);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(86);
				match(WS);
				}
			}

			setState(89);
			match(T__1);
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
	public static class Point_sequenceContext extends ParserRuleContext {
		public List<PointContext> point() {
			return getRuleContexts(PointContext.class);
		}
		public PointContext point(int i) {
			return getRuleContext(PointContext.class,i);
		}
		public List<TerminalNode> SEPERATOR() { return getTokens(SubsetDSLParser.SEPERATOR); }
		public TerminalNode SEPERATOR(int i) {
			return getToken(SubsetDSLParser.SEPERATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Point_sequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_point_sequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitPoint_sequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Point_sequenceContext point_sequence() throws RecognitionException {
		Point_sequenceContext _localctx = new Point_sequenceContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_point_sequence);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
			point();
			setState(102);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(93);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(92);
						match(WS);
						}
					}

					setState(95);
					match(SEPERATOR);
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
					point();
					}
					} 
				}
				setState(104);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
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
	public static class Point_sequence2Context extends ParserRuleContext {
		public List<PointContext> point() {
			return getRuleContexts(PointContext.class);
		}
		public PointContext point(int i) {
			return getRuleContext(PointContext.class,i);
		}
		public List<TerminalNode> SEPERATOR() { return getTokens(SubsetDSLParser.SEPERATOR); }
		public TerminalNode SEPERATOR(int i) {
			return getToken(SubsetDSLParser.SEPERATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Point_sequence2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_point_sequence2; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitPoint_sequence2(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Point_sequence2Context point_sequence2() throws RecognitionException {
		Point_sequence2Context _localctx = new Point_sequence2Context(_ctx, getState());
		enterRule(_localctx, 12, RULE_point_sequence2);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			point();
			setState(107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(106);
				match(WS);
				}
			}

			setState(109);
			match(SEPERATOR);
			setState(111);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(110);
				match(WS);
				}
			}

			setState(113);
			point();
			setState(124);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(115);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(114);
						match(WS);
						}
					}

					setState(117);
					match(SEPERATOR);
					setState(119);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(118);
						match(WS);
						}
					}

					setState(121);
					point();
					}
					} 
				}
				setState(126);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
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
	public static class NumberContext extends ParserRuleContext {
		public List<TerminalNode> INT() { return getTokens(SubsetDSLParser.INT); }
		public TerminalNode INT(int i) {
			return getToken(SubsetDSLParser.INT, i);
		}
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(127);
			match(INT);
			setState(130);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(128);
				match(T__4);
				setState(129);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PointContext extends ParserRuleContext {
		public PoiContext poi() {
			return getRuleContext(PoiContext.class,0);
		}
		public PContext p() {
			return getRuleContext(PContext.class,0);
		}
		public PointContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_point; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitPoint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PointContext point() throws RecognitionException {
		PointContext _localctx = new PointContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_point);
		try {
			setState(134);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__5:
				enterOuterAlt(_localctx, 1);
				{
				setState(132);
				poi();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(133);
				p();
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

	@SuppressWarnings("CheckReturnValue")
	public static class PoiContext extends ParserRuleContext {
		public Num_idContext group;
		public Url_sequenceContext url_sequence() {
			return getRuleContext(Url_sequenceContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Num_idContext num_id() {
			return getRuleContext(Num_idContext.class,0);
		}
		public PoiContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_poi; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitPoi(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PoiContext poi() throws RecognitionException {
		PoiContext _localctx = new PoiContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_poi);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(136);
			match(T__5);
			setState(147);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(137);
					match(WS);
					}
				}

				setState(140);
				match(T__6);
				setState(141);
				((PoiContext)_localctx).group = num_id();
				setState(143);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(142);
					match(WS);
					}
				}

				setState(145);
				match(T__3);
				}
				break;
			}
			setState(150);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(149);
				match(WS);
				}
			}

			setState(152);
			url_sequence();
			setState(154);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(153);
				match(WS);
				}
			}

			setState(156);
			match(T__1);
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
	public static class RoiContext extends ParserRuleContext {
		public Num_idContext group;
		public Url_sequenceContext url_sequence() {
			return getRuleContext(Url_sequenceContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Num_idContext num_id() {
			return getRuleContext(Num_idContext.class,0);
		}
		public RoiContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_roi; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitRoi(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RoiContext roi() throws RecognitionException {
		RoiContext _localctx = new RoiContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_roi);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
			match(T__7);
			setState(169);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(159);
					match(WS);
					}
				}

				setState(162);
				match(T__6);
				setState(163);
				((RoiContext)_localctx).group = num_id();
				setState(165);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WS) {
					{
					setState(164);
					match(WS);
					}
				}

				setState(167);
				match(T__3);
				}
				break;
			}
			setState(172);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(171);
				match(WS);
				}
			}

			setState(174);
			url_sequence();
			setState(176);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(175);
				match(WS);
				}
			}

			setState(178);
			match(T__1);
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
	public static class Url_sequenceContext extends ParserRuleContext {
		public List<UrlContext> url() {
			return getRuleContexts(UrlContext.class);
		}
		public UrlContext url(int i) {
			return getRuleContext(UrlContext.class,i);
		}
		public List<TerminalNode> SEPERATOR() { return getTokens(SubsetDSLParser.SEPERATOR); }
		public TerminalNode SEPERATOR(int i) {
			return getToken(SubsetDSLParser.SEPERATOR, i);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public Url_sequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_url_sequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitUrl_sequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Url_sequenceContext url_sequence() throws RecognitionException {
		Url_sequenceContext _localctx = new Url_sequenceContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_url_sequence);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			url();
			setState(191);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(182);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(181);
						match(WS);
						}
					}

					setState(184);
					match(SEPERATOR);
					setState(186);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WS) {
						{
						setState(185);
						match(WS);
						}
					}

					setState(188);
					url();
					}
					} 
				}
				setState(193);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
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
	public static class UrlContext extends ParserRuleContext {
		public Num_idContext group;
		public Num_idContext name;
		public List<Num_idContext> num_id() {
			return getRuleContexts(Num_idContext.class);
		}
		public Num_idContext num_id(int i) {
			return getRuleContext(Num_idContext.class,i);
		}
		public UrlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_url; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitUrl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UrlContext url() throws RecognitionException {
		UrlContext _localctx = new UrlContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_url);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				{
				setState(194);
				((UrlContext)_localctx).group = num_id();
				setState(195);
				match(T__8);
				}
				break;
			}
			setState(199);
			((UrlContext)_localctx).name = num_id();
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
	public static class PContext extends ParserRuleContext {
		public ConstantContext x;
		public ConstantContext y;
		public List<ConstantContext> constant() {
			return getRuleContexts(ConstantContext.class);
		}
		public ConstantContext constant(int i) {
			return getRuleContext(ConstantContext.class,i);
		}
		public List<TerminalNode> WS() { return getTokens(SubsetDSLParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(SubsetDSLParser.WS, i);
		}
		public PContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_p; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitP(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PContext p() throws RecognitionException {
		PContext _localctx = new PContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_p);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(201);
			match(T__9);
			setState(203);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(202);
				match(WS);
				}
			}

			setState(205);
			((PContext)_localctx).x = constant();
			setState(207);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(206);
				match(WS);
				}
			}

			setState(209);
			match(T__3);
			setState(211);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(210);
				match(WS);
				}
			}

			setState(213);
			((PContext)_localctx).y = constant();
			setState(215);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(214);
				match(WS);
				}
			}

			setState(217);
			match(T__1);
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
	public static class ConstantContext extends ParserRuleContext {
		public List<TerminalNode> INT() { return getTokens(SubsetDSLParser.INT); }
		public TerminalNode INT(int i) {
			return getToken(SubsetDSLParser.INT, i);
		}
		public TerminalNode PLUS_MINUS() { return getToken(SubsetDSLParser.PLUS_MINUS, 0); }
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_constant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS_MINUS) {
				{
				setState(219);
				match(PLUS_MINUS);
				}
			}

			setState(222);
			match(INT);
			setState(225);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__4) {
				{
				setState(223);
				match(T__4);
				setState(224);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Num_idContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(SubsetDSLParser.INT, 0); }
		public TerminalNode ID() { return getToken(SubsetDSLParser.ID, 0); }
		public Num_idContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_num_id; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SubsetDSLVisitor ) return ((SubsetDSLVisitor<? extends T>)visitor).visitNum_id(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Num_idContext num_id() throws RecognitionException {
		Num_idContext _localctx = new Num_idContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_num_id);
		try {
			setState(231);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,43,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(227);
				match(INT);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(228);
				match(ID);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(229);
				match(INT);
				setState(230);
				match(ID);
				}
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

	public static final String _serializedATN =
		"\u0004\u0001\u000f\u00ea\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0001\u0000\u0003\u0000\"\b\u0000\u0001\u0000\u0001\u0000\u0003"+
		"\u0000&\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003"+
		"\u0001,\b\u0001\u0001\u0001\u0001\u0001\u0003\u00010\b\u0001\u0001\u0001"+
		"\u0005\u00013\b\u0001\n\u0001\f\u00016\t\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002;\b\u0002\u0001\u0003\u0001\u0003\u0003\u0003"+
		"?\b\u0003\u0001\u0003\u0001\u0003\u0003\u0003C\b\u0003\u0001\u0003\u0003"+
		"\u0003F\b\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0003"+
		"\u0004L\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004P\b\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004T\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"X\b\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0003\u0005"+
		"^\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005b\b\u0005\u0001\u0005\u0005"+
		"\u0005e\b\u0005\n\u0005\f\u0005h\t\u0005\u0001\u0006\u0001\u0006\u0003"+
		"\u0006l\b\u0006\u0001\u0006\u0001\u0006\u0003\u0006p\b\u0006\u0001\u0006"+
		"\u0001\u0006\u0003\u0006t\b\u0006\u0001\u0006\u0001\u0006\u0003\u0006"+
		"x\b\u0006\u0001\u0006\u0005\u0006{\b\u0006\n\u0006\f\u0006~\t\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0083\b\u0007\u0001\b\u0001"+
		"\b\u0003\b\u0087\b\b\u0001\t\u0001\t\u0003\t\u008b\b\t\u0001\t\u0001\t"+
		"\u0001\t\u0003\t\u0090\b\t\u0001\t\u0001\t\u0003\t\u0094\b\t\u0001\t\u0003"+
		"\t\u0097\b\t\u0001\t\u0001\t\u0003\t\u009b\b\t\u0001\t\u0001\t\u0001\n"+
		"\u0001\n\u0003\n\u00a1\b\n\u0001\n\u0001\n\u0001\n\u0003\n\u00a6\b\n\u0001"+
		"\n\u0001\n\u0003\n\u00aa\b\n\u0001\n\u0003\n\u00ad\b\n\u0001\n\u0001\n"+
		"\u0003\n\u00b1\b\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0003\u000b"+
		"\u00b7\b\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00bb\b\u000b\u0001"+
		"\u000b\u0005\u000b\u00be\b\u000b\n\u000b\f\u000b\u00c1\t\u000b\u0001\f"+
		"\u0001\f\u0001\f\u0003\f\u00c6\b\f\u0001\f\u0001\f\u0001\r\u0001\r\u0003"+
		"\r\u00cc\b\r\u0001\r\u0001\r\u0003\r\u00d0\b\r\u0001\r\u0001\r\u0003\r"+
		"\u00d4\b\r\u0001\r\u0001\r\u0003\r\u00d8\b\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0003\u000e\u00dd\b\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e"+
		"\u00e2\b\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f"+
		"\u00e8\b\u000f\u0001\u000f\u0000\u0000\u0010\u0000\u0002\u0004\u0006\b"+
		"\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e\u0000\u0000"+
		"\u0107\u0000!\u0001\u0000\u0000\u0000\u0002)\u0001\u0000\u0000\u0000\u0004"+
		":\u0001\u0000\u0000\u0000\u0006<\u0001\u0000\u0000\u0000\bI\u0001\u0000"+
		"\u0000\u0000\n[\u0001\u0000\u0000\u0000\fi\u0001\u0000\u0000\u0000\u000e"+
		"\u007f\u0001\u0000\u0000\u0000\u0010\u0086\u0001\u0000\u0000\u0000\u0012"+
		"\u0088\u0001\u0000\u0000\u0000\u0014\u009e\u0001\u0000\u0000\u0000\u0016"+
		"\u00b4\u0001\u0000\u0000\u0000\u0018\u00c5\u0001\u0000\u0000\u0000\u001a"+
		"\u00c9\u0001\u0000\u0000\u0000\u001c\u00dc\u0001\u0000\u0000\u0000\u001e"+
		"\u00e7\u0001\u0000\u0000\u0000 \"\u0005\f\u0000\u0000! \u0001\u0000\u0000"+
		"\u0000!\"\u0001\u0000\u0000\u0000\"#\u0001\u0000\u0000\u0000#%\u0003\u0002"+
		"\u0001\u0000$&\u0005\f\u0000\u0000%$\u0001\u0000\u0000\u0000%&\u0001\u0000"+
		"\u0000\u0000&\'\u0001\u0000\u0000\u0000\'(\u0005\u0000\u0000\u0001(\u0001"+
		"\u0001\u0000\u0000\u0000)4\u0003\u0004\u0002\u0000*,\u0005\f\u0000\u0000"+
		"+*\u0001\u0000\u0000\u0000+,\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000"+
		"\u0000-/\u0005\u000f\u0000\u0000.0\u0005\f\u0000\u0000/.\u0001\u0000\u0000"+
		"\u0000/0\u0001\u0000\u0000\u000001\u0001\u0000\u0000\u000013\u0003\u0004"+
		"\u0002\u00002+\u0001\u0000\u0000\u000036\u0001\u0000\u0000\u000042\u0001"+
		"\u0000\u0000\u000045\u0001\u0000\u0000\u00005\u0003\u0001\u0000\u0000"+
		"\u000064\u0001\u0000\u0000\u00007;\u0003\b\u0004\u00008;\u0003\u0014\n"+
		"\u00009;\u0003\u0006\u0003\u0000:7\u0001\u0000\u0000\u0000:8\u0001\u0000"+
		"\u0000\u0000:9\u0001\u0000\u0000\u0000;\u0005\u0001\u0000\u0000\u0000"+
		"<>\u0005\u0001\u0000\u0000=?\u0005\f\u0000\u0000>=\u0001\u0000\u0000\u0000"+
		">?\u0001\u0000\u0000\u0000?B\u0001\u0000\u0000\u0000@C\u0003\f\u0006\u0000"+
		"AC\u0003\u0002\u0001\u0000B@\u0001\u0000\u0000\u0000BA\u0001\u0000\u0000"+
		"\u0000CE\u0001\u0000\u0000\u0000DF\u0005\f\u0000\u0000ED\u0001\u0000\u0000"+
		"\u0000EF\u0001\u0000\u0000\u0000FG\u0001\u0000\u0000\u0000GH\u0005\u0002"+
		"\u0000\u0000H\u0007\u0001\u0000\u0000\u0000IK\u0005\u0003\u0000\u0000"+
		"JL\u0005\f\u0000\u0000KJ\u0001\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000"+
		"LM\u0001\u0000\u0000\u0000MO\u0003\n\u0005\u0000NP\u0005\f\u0000\u0000"+
		"ON\u0001\u0000\u0000\u0000OP\u0001\u0000\u0000\u0000PQ\u0001\u0000\u0000"+
		"\u0000QS\u0005\u0004\u0000\u0000RT\u0005\f\u0000\u0000SR\u0001\u0000\u0000"+
		"\u0000ST\u0001\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000UW\u0003\u000e"+
		"\u0007\u0000VX\u0005\f\u0000\u0000WV\u0001\u0000\u0000\u0000WX\u0001\u0000"+
		"\u0000\u0000XY\u0001\u0000\u0000\u0000YZ\u0005\u0002\u0000\u0000Z\t\u0001"+
		"\u0000\u0000\u0000[f\u0003\u0010\b\u0000\\^\u0005\f\u0000\u0000]\\\u0001"+
		"\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^_\u0001\u0000\u0000\u0000"+
		"_a\u0005\u000f\u0000\u0000`b\u0005\f\u0000\u0000a`\u0001\u0000\u0000\u0000"+
		"ab\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ce\u0003\u0010\b\u0000"+
		"d]\u0001\u0000\u0000\u0000eh\u0001\u0000\u0000\u0000fd\u0001\u0000\u0000"+
		"\u0000fg\u0001\u0000\u0000\u0000g\u000b\u0001\u0000\u0000\u0000hf\u0001"+
		"\u0000\u0000\u0000ik\u0003\u0010\b\u0000jl\u0005\f\u0000\u0000kj\u0001"+
		"\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000"+
		"mo\u0005\u000f\u0000\u0000np\u0005\f\u0000\u0000on\u0001\u0000\u0000\u0000"+
		"op\u0001\u0000\u0000\u0000pq\u0001\u0000\u0000\u0000q|\u0003\u0010\b\u0000"+
		"rt\u0005\f\u0000\u0000sr\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000"+
		"tu\u0001\u0000\u0000\u0000uw\u0005\u000f\u0000\u0000vx\u0005\f\u0000\u0000"+
		"wv\u0001\u0000\u0000\u0000wx\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000"+
		"\u0000y{\u0003\u0010\b\u0000zs\u0001\u0000\u0000\u0000{~\u0001\u0000\u0000"+
		"\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\r\u0001\u0000"+
		"\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0082\u0005\r\u0000\u0000"+
		"\u0080\u0081\u0005\u0005\u0000\u0000\u0081\u0083\u0005\r\u0000\u0000\u0082"+
		"\u0080\u0001\u0000\u0000\u0000\u0082\u0083\u0001\u0000\u0000\u0000\u0083"+
		"\u000f\u0001\u0000\u0000\u0000\u0084\u0087\u0003\u0012\t\u0000\u0085\u0087"+
		"\u0003\u001a\r\u0000\u0086\u0084\u0001\u0000\u0000\u0000\u0086\u0085\u0001"+
		"\u0000\u0000\u0000\u0087\u0011\u0001\u0000\u0000\u0000\u0088\u0093\u0005"+
		"\u0006\u0000\u0000\u0089\u008b\u0005\f\u0000\u0000\u008a\u0089\u0001\u0000"+
		"\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b\u008c\u0001\u0000"+
		"\u0000\u0000\u008c\u008d\u0005\u0007\u0000\u0000\u008d\u008f\u0003\u001e"+
		"\u000f\u0000\u008e\u0090\u0005\f\u0000\u0000\u008f\u008e\u0001\u0000\u0000"+
		"\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0091\u0001\u0000\u0000"+
		"\u0000\u0091\u0092\u0005\u0004\u0000\u0000\u0092\u0094\u0001\u0000\u0000"+
		"\u0000\u0093\u008a\u0001\u0000\u0000\u0000\u0093\u0094\u0001\u0000\u0000"+
		"\u0000\u0094\u0096\u0001\u0000\u0000\u0000\u0095\u0097\u0005\f\u0000\u0000"+
		"\u0096\u0095\u0001\u0000\u0000\u0000\u0096\u0097\u0001\u0000\u0000\u0000"+
		"\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u009a\u0003\u0016\u000b\u0000"+
		"\u0099\u009b\u0005\f\u0000\u0000\u009a\u0099\u0001\u0000\u0000\u0000\u009a"+
		"\u009b\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000\u009c"+
		"\u009d\u0005\u0002\u0000\u0000\u009d\u0013\u0001\u0000\u0000\u0000\u009e"+
		"\u00a9\u0005\b\u0000\u0000\u009f\u00a1\u0005\f\u0000\u0000\u00a0\u009f"+
		"\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00a2"+
		"\u0001\u0000\u0000\u0000\u00a2\u00a3\u0005\u0007\u0000\u0000\u00a3\u00a5"+
		"\u0003\u001e\u000f\u0000\u00a4\u00a6\u0005\f\u0000\u0000\u00a5\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001"+
		"\u0000\u0000\u0000\u00a7\u00a8\u0005\u0004\u0000\u0000\u00a8\u00aa\u0001"+
		"\u0000\u0000\u0000\u00a9\u00a0\u0001\u0000\u0000\u0000\u00a9\u00aa\u0001"+
		"\u0000\u0000\u0000\u00aa\u00ac\u0001\u0000\u0000\u0000\u00ab\u00ad\u0005"+
		"\f\u0000\u0000\u00ac\u00ab\u0001\u0000\u0000\u0000\u00ac\u00ad\u0001\u0000"+
		"\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae\u00b0\u0003\u0016"+
		"\u000b\u0000\u00af\u00b1\u0005\f\u0000\u0000\u00b0\u00af\u0001\u0000\u0000"+
		"\u0000\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b2\u0001\u0000\u0000"+
		"\u0000\u00b2\u00b3\u0005\u0002\u0000\u0000\u00b3\u0015\u0001\u0000\u0000"+
		"\u0000\u00b4\u00bf\u0003\u0018\f\u0000\u00b5\u00b7\u0005\f\u0000\u0000"+
		"\u00b6\u00b5\u0001\u0000\u0000\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000"+
		"\u00b7\u00b8\u0001\u0000\u0000\u0000\u00b8\u00ba\u0005\u000f\u0000\u0000"+
		"\u00b9\u00bb\u0005\f\u0000\u0000\u00ba\u00b9\u0001\u0000\u0000\u0000\u00ba"+
		"\u00bb\u0001\u0000\u0000\u0000\u00bb\u00bc\u0001\u0000\u0000\u0000\u00bc"+
		"\u00be\u0003\u0018\f\u0000\u00bd\u00b6\u0001\u0000\u0000\u0000\u00be\u00c1"+
		"\u0001\u0000\u0000\u0000\u00bf\u00bd\u0001\u0000\u0000\u0000\u00bf\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c0\u0017\u0001\u0000\u0000\u0000\u00c1\u00bf"+
		"\u0001\u0000\u0000\u0000\u00c2\u00c3\u0003\u001e\u000f\u0000\u00c3\u00c4"+
		"\u0005\t\u0000\u0000\u00c4\u00c6\u0001\u0000\u0000\u0000\u00c5\u00c2\u0001"+
		"\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000\u00c6\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c7\u00c8\u0003\u001e\u000f\u0000\u00c8\u0019\u0001"+
		"\u0000\u0000\u0000\u00c9\u00cb\u0005\n\u0000\u0000\u00ca\u00cc\u0005\f"+
		"\u0000\u0000\u00cb\u00ca\u0001\u0000\u0000\u0000\u00cb\u00cc\u0001\u0000"+
		"\u0000\u0000\u00cc\u00cd\u0001\u0000\u0000\u0000\u00cd\u00cf\u0003\u001c"+
		"\u000e\u0000\u00ce\u00d0\u0005\f\u0000\u0000\u00cf\u00ce\u0001\u0000\u0000"+
		"\u0000\u00cf\u00d0\u0001\u0000\u0000\u0000\u00d0\u00d1\u0001\u0000\u0000"+
		"\u0000\u00d1\u00d3\u0005\u0004\u0000\u0000\u00d2\u00d4\u0005\f\u0000\u0000"+
		"\u00d3\u00d2\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000"+
		"\u00d4\u00d5\u0001\u0000\u0000\u0000\u00d5\u00d7\u0003\u001c\u000e\u0000"+
		"\u00d6\u00d8\u0005\f\u0000\u0000\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d7"+
		"\u00d8\u0001\u0000\u0000\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9"+
		"\u00da\u0005\u0002\u0000\u0000\u00da\u001b\u0001\u0000\u0000\u0000\u00db"+
		"\u00dd\u0005\u000e\u0000\u0000\u00dc\u00db\u0001\u0000\u0000\u0000\u00dc"+
		"\u00dd\u0001\u0000\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de"+
		"\u00e1\u0005\r\u0000\u0000\u00df\u00e0\u0005\u0005\u0000\u0000\u00e0\u00e2"+
		"\u0005\r\u0000\u0000\u00e1\u00df\u0001\u0000\u0000\u0000\u00e1\u00e2\u0001"+
		"\u0000\u0000\u0000\u00e2\u001d\u0001\u0000\u0000\u0000\u00e3\u00e8\u0005"+
		"\r\u0000\u0000\u00e4\u00e8\u0005\u000b\u0000\u0000\u00e5\u00e6\u0005\r"+
		"\u0000\u0000\u00e6\u00e8\u0005\u000b\u0000\u0000\u00e7\u00e3\u0001\u0000"+
		"\u0000\u0000\u00e7\u00e4\u0001\u0000\u0000\u0000\u00e7\u00e5\u0001\u0000"+
		"\u0000\u0000\u00e8\u001f\u0001\u0000\u0000\u0000,!%+/4:>BEKOSW]afkosw"+
		"|\u0082\u0086\u008a\u008f\u0093\u0096\u009a\u00a0\u00a5\u00a9\u00ac\u00b0"+
		"\u00b6\u00ba\u00bf\u00c5\u00cb\u00cf\u00d3\u00d7\u00dc\u00e1\u00e7";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}