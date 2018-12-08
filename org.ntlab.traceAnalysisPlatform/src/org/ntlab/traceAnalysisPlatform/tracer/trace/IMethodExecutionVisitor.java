package org.ntlab.traceAnalysisPlatform.tracer.trace;

import java.util.ArrayList;

/**
 * Interface to visit method executions in a trace
 * @author Nitta
 *
 */
public interface IMethodExecutionVisitor {
	abstract public boolean preVisitThread(ThreadInstance thread);
	abstract public boolean postVisitThread(ThreadInstance thread);
	abstract public boolean preVisitMethodExecution(MethodExecution methodExecution);
	abstract public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children);
}
