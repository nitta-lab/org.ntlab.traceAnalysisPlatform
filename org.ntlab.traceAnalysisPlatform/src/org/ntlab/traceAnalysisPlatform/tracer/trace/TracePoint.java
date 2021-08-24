package org.ntlab.traceAnalysisPlatform.tracer.trace;
 
import java.util.ArrayList;
 
/**
 * An execution point in a trace
 * @author Nitta
 *
 */
public class TracePoint {
	private MethodExecution methodExecution;
	private int order = 0;
	
	public TracePoint(MethodExecution methodExecution, int order) {
		this.methodExecution = methodExecution;
		this.order = order;
	}
	
	public TracePoint duplicate() {
		return new TracePoint(methodExecution, order);
	}
	
	public Statement getStatement() {
		if (methodExecution.getStatements().size() <= order) return null;
		return methodExecution.getStatements().get(order);
	}
	
	public MethodExecution getMethodExecution() {
		return methodExecution;
	}
	
	public ArrayList<MethodExecution> getPreviouslyCalledMethods() {
		ArrayList<MethodExecution> children = new ArrayList<MethodExecution>();
		ArrayList<Statement> statements = methodExecution.getStatements();
		for (int i = 0; i < order; i++) {
			Statement statement = statements.get(i);
			if (statement instanceof MethodInvocation) {
				MethodExecution child = ((MethodInvocation)statement).getCalledMethodExecution();
				children.add(child);
			}
		}
		return children;
	}
	
	/**
	 * forward full step execution (excepting method executions in which no statement is recorded)
	 * @return false: cannot traverse any more, true: otherwise
	 */
	public boolean stepFull() {
		if (getStatement() instanceof MethodInvocation) {
			MethodExecution calledMethodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (calledMethodExecution.getStatements().size() > 0) {
				methodExecution = calledMethodExecution;
				order = 0;
				return true;
			}
		}
		while (order >= methodExecution.getStatements().size() - 1) {
			order = methodExecution.getCallerStatementExecution();
			methodExecution = methodExecution.getCallerMethodExecution();
			if (methodExecution == null) {
				order = -1;
				return false;
			}
		}
		order++;
		return true;
	}
	
	/**
	 * backward  full step execution (excepting method executions in which no statement is recorded. Caution!!: method invocation statement will be revisited after traversing the called method execution)
	 * @return false: cannot traverse any more, true: otherwise
	 */
	public boolean stepBackFull() {
		if (order <= 0) {
			order = methodExecution.getCallerStatementExecution();
			methodExecution = methodExecution.getCallerMethodExecution();
			if (methodExecution == null) {
				order = -1;
				return false;
			}
			return true;
		}
		order--;
		while (getStatement() instanceof MethodInvocation) {
			MethodExecution calledMethodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (calledMethodExecution.getStatements().size() == 0) break;
			methodExecution = calledMethodExecution;
			order = methodExecution.getStatements().size() - 1;
		}
		return true;
	}
	
	/**
	 * forward step execution without stepping into the called method execution
	 * @return false: has returned to the calling method execution or cannot traverse any mode, true: otherwise
	 */
	public boolean stepOver() {
		if (order < methodExecution.getStatements().size() - 1) {
			order++;
			return true;
		}
		order = methodExecution.getCallerStatementExecution();
		methodExecution = methodExecution.getCallerMethodExecution();
		if (methodExecution == null) {
			order = -1;
			return false;
		}
		return false;
	}
	
	/**
	 * backward step execution without stepping into the called method execution
	 * @return false: has returned to the calling method execution or cannot traverse any mode, true: otherwise
	 */
	public boolean stepBackOver() {
		if (order > 0) {
			order--;
			return true;
		}
		order = methodExecution.getCallerStatementExecution();
		methodExecution = methodExecution.getCallerMethodExecution();
		if (methodExecution == null) {
			order = -1;
			return false;
		}
		return false;
	}
	
	/**
	 * forward step execution without returning to the calling method execution
	 * @return false: has stepped into the called method execution or cannot traverse any mode, true: otherwise
	 */
	public boolean stepNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			if (methodExecution.getStatements().size() > 0) {
				order = 0;
			} else {
				order = -1;				// no statement is recorded in the called method execution
			}
			return false;
		}
		order++;
		if (order < methodExecution.getStatements().size()) {
			return true;
		}
		return false;
	}
	
	/**
	 * backward step execution without returning to the calling method execution (Caution!!: before stepping into the called method execution, the method invocation statement in the calling method is visited in advance)
	 * @return@false: has stepped into the called method execution or cannot traverse any mode, true: otherwise
	 */
	public boolean stepBackNoReturn() {
		if (getStatement() instanceof MethodInvocation) {
			methodExecution = ((MethodInvocation)getStatement()).getCalledMethodExecution();
			order = methodExecution.getStatements().size() - 1;			// the value mat become -1 (when no statement is recorded in the called method execution)
			return false;
		}
		order--;
		if (order >= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * forward step execution without returning to the calling method execution and stepping into the called method execution
	 * @return false: exit a method execution, true: otherwise
	 */
	public boolean stepNext() {
		order++;
		return (order < methodExecution.getStatements().size());
	}
	
	public boolean isValid() {
		if (methodExecution == null || order == -1 || order >= methodExecution.getStatements().size()) return false;
		return true;
	}
	
	public boolean isMethodEntry() {
		return (order == 0);
	}
	
	public boolean isStepBackOut() {
		if (order < 0) return true;
		return false;
	}
	
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof TracePoint)) return false;
		if (methodExecution != ((TracePoint) other).methodExecution) return false;
		if (order != ((TracePoint) other).order) return false;
		return true;
	}
}