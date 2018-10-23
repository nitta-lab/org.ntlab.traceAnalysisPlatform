package org.ntlab.traceAnalysisPlatform.tracer.trace;


public interface IStatementVisitor {
	abstract public boolean preVisitStatement(Statement statement);
	abstract public boolean postVisitStatement(Statement statement);
}
