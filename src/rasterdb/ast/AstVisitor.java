package rasterdb.ast;

public interface AstVisitor<T, P> {
	
	public T visitRadiation(AST_radiance ast, P param);
	public T visitSequence(AST_Sequence ast, P param);
	public T visitBandNumber(AST_Band_number ast, P param);
	public T visitFunction(AST_function ast, P param);
	public T visitConstant(AST_Constant ast_Constant, P param);

}
