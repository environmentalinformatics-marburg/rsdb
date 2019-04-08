// Generated from SubsetDSL.g4 by ANTLR 4.7
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
public class SubsetDSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

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
	public static final String[] ruleNames = {
		"region_scirpt", "region_sequence", "region", "bbox", "square", "point_sequence", 
		"point_sequence2", "number", "point", "poi", "roi", "url_sequence", "url", 
		"p", "constant", "num_id"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\21\u00ec\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\3\2\5\2"+
		"$\n\2\3\2\3\2\5\2(\n\2\3\2\3\2\3\3\3\3\5\3.\n\3\3\3\3\3\5\3\62\n\3\3\3"+
		"\7\3\65\n\3\f\3\16\38\13\3\3\4\3\4\3\4\5\4=\n\4\3\5\3\5\5\5A\n\5\3\5\3"+
		"\5\5\5E\n\5\3\5\5\5H\n\5\3\5\3\5\3\6\3\6\5\6N\n\6\3\6\3\6\5\6R\n\6\3\6"+
		"\3\6\5\6V\n\6\3\6\3\6\5\6Z\n\6\3\6\3\6\3\7\3\7\5\7`\n\7\3\7\3\7\5\7d\n"+
		"\7\3\7\7\7g\n\7\f\7\16\7j\13\7\3\b\3\b\5\bn\n\b\3\b\3\b\5\br\n\b\3\b\3"+
		"\b\5\bv\n\b\3\b\3\b\5\bz\n\b\3\b\7\b}\n\b\f\b\16\b\u0080\13\b\3\t\3\t"+
		"\3\t\5\t\u0085\n\t\3\n\3\n\5\n\u0089\n\n\3\13\3\13\5\13\u008d\n\13\3\13"+
		"\3\13\3\13\5\13\u0092\n\13\3\13\3\13\5\13\u0096\n\13\3\13\5\13\u0099\n"+
		"\13\3\13\3\13\5\13\u009d\n\13\3\13\3\13\3\f\3\f\5\f\u00a3\n\f\3\f\3\f"+
		"\3\f\5\f\u00a8\n\f\3\f\3\f\5\f\u00ac\n\f\3\f\5\f\u00af\n\f\3\f\3\f\5\f"+
		"\u00b3\n\f\3\f\3\f\3\r\3\r\5\r\u00b9\n\r\3\r\3\r\5\r\u00bd\n\r\3\r\7\r"+
		"\u00c0\n\r\f\r\16\r\u00c3\13\r\3\16\3\16\3\16\5\16\u00c8\n\16\3\16\3\16"+
		"\3\17\3\17\5\17\u00ce\n\17\3\17\3\17\5\17\u00d2\n\17\3\17\3\17\5\17\u00d6"+
		"\n\17\3\17\3\17\5\17\u00da\n\17\3\17\3\17\3\20\5\20\u00df\n\20\3\20\3"+
		"\20\3\20\5\20\u00e4\n\20\3\21\3\21\3\21\3\21\5\21\u00ea\n\21\3\21\2\2"+
		"\22\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \2\2\2\u0109\2#\3\2\2\2\4+"+
		"\3\2\2\2\6<\3\2\2\2\b>\3\2\2\2\nK\3\2\2\2\f]\3\2\2\2\16k\3\2\2\2\20\u0081"+
		"\3\2\2\2\22\u0088\3\2\2\2\24\u008a\3\2\2\2\26\u00a0\3\2\2\2\30\u00b6\3"+
		"\2\2\2\32\u00c7\3\2\2\2\34\u00cb\3\2\2\2\36\u00de\3\2\2\2 \u00e9\3\2\2"+
		"\2\"$\7\16\2\2#\"\3\2\2\2#$\3\2\2\2$%\3\2\2\2%\'\5\4\3\2&(\7\16\2\2\'"+
		"&\3\2\2\2\'(\3\2\2\2()\3\2\2\2)*\7\2\2\3*\3\3\2\2\2+\66\5\6\4\2,.\7\16"+
		"\2\2-,\3\2\2\2-.\3\2\2\2./\3\2\2\2/\61\7\21\2\2\60\62\7\16\2\2\61\60\3"+
		"\2\2\2\61\62\3\2\2\2\62\63\3\2\2\2\63\65\5\6\4\2\64-\3\2\2\2\658\3\2\2"+
		"\2\66\64\3\2\2\2\66\67\3\2\2\2\67\5\3\2\2\28\66\3\2\2\29=\5\n\6\2:=\5"+
		"\26\f\2;=\5\b\5\2<9\3\2\2\2<:\3\2\2\2<;\3\2\2\2=\7\3\2\2\2>@\7\3\2\2?"+
		"A\7\16\2\2@?\3\2\2\2@A\3\2\2\2AD\3\2\2\2BE\5\16\b\2CE\5\4\3\2DB\3\2\2"+
		"\2DC\3\2\2\2EG\3\2\2\2FH\7\16\2\2GF\3\2\2\2GH\3\2\2\2HI\3\2\2\2IJ\7\4"+
		"\2\2J\t\3\2\2\2KM\7\5\2\2LN\7\16\2\2ML\3\2\2\2MN\3\2\2\2NO\3\2\2\2OQ\5"+
		"\f\7\2PR\7\16\2\2QP\3\2\2\2QR\3\2\2\2RS\3\2\2\2SU\7\6\2\2TV\7\16\2\2U"+
		"T\3\2\2\2UV\3\2\2\2VW\3\2\2\2WY\5\20\t\2XZ\7\16\2\2YX\3\2\2\2YZ\3\2\2"+
		"\2Z[\3\2\2\2[\\\7\4\2\2\\\13\3\2\2\2]h\5\22\n\2^`\7\16\2\2_^\3\2\2\2_"+
		"`\3\2\2\2`a\3\2\2\2ac\7\21\2\2bd\7\16\2\2cb\3\2\2\2cd\3\2\2\2de\3\2\2"+
		"\2eg\5\22\n\2f_\3\2\2\2gj\3\2\2\2hf\3\2\2\2hi\3\2\2\2i\r\3\2\2\2jh\3\2"+
		"\2\2km\5\22\n\2ln\7\16\2\2ml\3\2\2\2mn\3\2\2\2no\3\2\2\2oq\7\21\2\2pr"+
		"\7\16\2\2qp\3\2\2\2qr\3\2\2\2rs\3\2\2\2s~\5\22\n\2tv\7\16\2\2ut\3\2\2"+
		"\2uv\3\2\2\2vw\3\2\2\2wy\7\21\2\2xz\7\16\2\2yx\3\2\2\2yz\3\2\2\2z{\3\2"+
		"\2\2{}\5\22\n\2|u\3\2\2\2}\u0080\3\2\2\2~|\3\2\2\2~\177\3\2\2\2\177\17"+
		"\3\2\2\2\u0080~\3\2\2\2\u0081\u0084\7\17\2\2\u0082\u0083\7\7\2\2\u0083"+
		"\u0085\7\17\2\2\u0084\u0082\3\2\2\2\u0084\u0085\3\2\2\2\u0085\21\3\2\2"+
		"\2\u0086\u0089\5\24\13\2\u0087\u0089\5\34\17\2\u0088\u0086\3\2\2\2\u0088"+
		"\u0087\3\2\2\2\u0089\23\3\2\2\2\u008a\u0095\7\b\2\2\u008b\u008d\7\16\2"+
		"\2\u008c\u008b\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f"+
		"\7\t\2\2\u008f\u0091\5 \21\2\u0090\u0092\7\16\2\2\u0091\u0090\3\2\2\2"+
		"\u0091\u0092\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0094\7\6\2\2\u0094\u0096"+
		"\3\2\2\2\u0095\u008c\3\2\2\2\u0095\u0096\3\2\2\2\u0096\u0098\3\2\2\2\u0097"+
		"\u0099\7\16\2\2\u0098\u0097\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u009a\3"+
		"\2\2\2\u009a\u009c\5\30\r\2\u009b\u009d\7\16\2\2\u009c\u009b\3\2\2\2\u009c"+
		"\u009d\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u009f\7\4\2\2\u009f\25\3\2\2"+
		"\2\u00a0\u00ab\7\n\2\2\u00a1\u00a3\7\16\2\2\u00a2\u00a1\3\2\2\2\u00a2"+
		"\u00a3\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a5\7\t\2\2\u00a5\u00a7\5 "+
		"\21\2\u00a6\u00a8\7\16\2\2\u00a7\u00a6\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8"+
		"\u00a9\3\2\2\2\u00a9\u00aa\7\6\2\2\u00aa\u00ac\3\2\2\2\u00ab\u00a2\3\2"+
		"\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad\u00af\7\16\2\2\u00ae"+
		"\u00ad\3\2\2\2\u00ae\u00af\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00b2\5\30"+
		"\r\2\u00b1\u00b3\7\16\2\2\u00b2\u00b1\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3"+
		"\u00b4\3\2\2\2\u00b4\u00b5\7\4\2\2\u00b5\27\3\2\2\2\u00b6\u00c1\5\32\16"+
		"\2\u00b7\u00b9\7\16\2\2\u00b8\u00b7\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9"+
		"\u00ba\3\2\2\2\u00ba\u00bc\7\21\2\2\u00bb\u00bd\7\16\2\2\u00bc\u00bb\3"+
		"\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00c0\5\32\16\2\u00bf"+
		"\u00b8\3\2\2\2\u00c0\u00c3\3\2\2\2\u00c1\u00bf\3\2\2\2\u00c1\u00c2\3\2"+
		"\2\2\u00c2\31\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c4\u00c5\5 \21\2\u00c5\u00c6"+
		"\7\13\2\2\u00c6\u00c8\3\2\2\2\u00c7\u00c4\3\2\2\2\u00c7\u00c8\3\2\2\2"+
		"\u00c8\u00c9\3\2\2\2\u00c9\u00ca\5 \21\2\u00ca\33\3\2\2\2\u00cb\u00cd"+
		"\7\f\2\2\u00cc\u00ce\7\16\2\2\u00cd\u00cc\3\2\2\2\u00cd\u00ce\3\2\2\2"+
		"\u00ce\u00cf\3\2\2\2\u00cf\u00d1\5\36\20\2\u00d0\u00d2\7\16\2\2\u00d1"+
		"\u00d0\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3\u00d5\7\6"+
		"\2\2\u00d4\u00d6\7\16\2\2\u00d5\u00d4\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6"+
		"\u00d7\3\2\2\2\u00d7\u00d9\5\36\20\2\u00d8\u00da\7\16\2\2\u00d9\u00d8"+
		"\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u00dc\7\4\2\2\u00dc"+
		"\35\3\2\2\2\u00dd\u00df\7\20\2\2\u00de\u00dd\3\2\2\2\u00de\u00df\3\2\2"+
		"\2\u00df\u00e0\3\2\2\2\u00e0\u00e3\7\17\2\2\u00e1\u00e2\7\7\2\2\u00e2"+
		"\u00e4\7\17\2\2\u00e3\u00e1\3\2\2\2\u00e3\u00e4\3\2\2\2\u00e4\37\3\2\2"+
		"\2\u00e5\u00ea\7\17\2\2\u00e6\u00ea\7\r\2\2\u00e7\u00e8\7\17\2\2\u00e8"+
		"\u00ea\7\r\2\2\u00e9\u00e5\3\2\2\2\u00e9\u00e6\3\2\2\2\u00e9\u00e7\3\2"+
		"\2\2\u00ea!\3\2\2\2.#\'-\61\66<@DGMQUY_chmquy~\u0084\u0088\u008c\u0091"+
		"\u0095\u0098\u009c\u00a2\u00a7\u00ab\u00ae\u00b2\u00b8\u00bc\u00c1\u00c7"+
		"\u00cd\u00d1\u00d5\u00d9\u00de\u00e3\u00e9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}