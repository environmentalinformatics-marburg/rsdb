package rasterdb.ast;

public interface AST {
	
	public static int PRIO_BASE = 0;
	public static int PRIO_ADD_SUB = 10;
	public static int PRIO_MUL_DIV = 20;
	public static int PRIO_POW = 30;
	public static int UNKNOWN = PRIO_BASE;
	
	<T,P> T accept(AstVisitor<T,P> visitor, P param);
	
	default <T> T accept(AstVisitor<T,Void> visitor) {
		return accept(visitor, null);
	}
	
	default int getPriority() {
		return 0;	
	}
	static int priorityOf(AST ast) {
		if(ast==null) {
			return 0;
		}
		return ast.getPriority();
	}
}