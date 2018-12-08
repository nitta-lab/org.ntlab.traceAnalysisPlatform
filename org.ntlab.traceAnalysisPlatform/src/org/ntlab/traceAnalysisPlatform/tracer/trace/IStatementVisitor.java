package org.ntlab.traceAnalysisPlatform.tracer.trace;

/**
 * Interface to visit statement executions in a trace
 * @author Nitta
 *
 */
public interface IStatementVisitor {
	abstract public boolean preVisitStatement(Statement statement);
	abstract public boolean postVisitStatement(Statement statement);
}
