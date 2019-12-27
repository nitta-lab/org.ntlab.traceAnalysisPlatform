package org.ntlab.traceAnalysisPlatform.tracer.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * An execution of a method in a trace
 * @author Nitta
 *
 */
public class MethodExecution {
	private String signature;
	private String callerSideSignature;
	private String thisClassName;
	private String thisObjId;
	private ArrayList<ObjectReference> arguments;
	private ObjectReference returnValue = null;
	private boolean isConstructor;
	private boolean isStatic;
	private boolean isCollectionType;
	private ArrayList<Statement> statements = new ArrayList<Statement>();
	private ArrayList<MethodExecution> children = new ArrayList<MethodExecution>();
	private MethodExecution callerMethodExecution = null;
	private int callerStatementExecution = -1;
	private boolean isTerminated = false;
	private AugmentationInfo augmentation = null;
	private long entryTime = 0L;
	private long exitTime = 0L;
	
	public MethodExecution(String signature, String callerSideSignature,
			String thisClassName, String thisObjId, boolean isConstructor,
			boolean isStatic, long enterTime) {
		this.signature = signature;
		this.callerSideSignature = callerSideSignature;
		this.thisClassName = thisClassName;
		this.thisObjId = thisObjId;
		this.isConstructor = isConstructor;
		this.isStatic = isStatic;
		this.isCollectionType = false;
		this.isTerminated = false;
		this.entryTime  = enterTime;
	}

	public void setArguments(ArrayList<ObjectReference> arguments) {
		this.arguments = arguments;
	}
	
	public void setThisObjeId(String thisObjId) {
		this.thisObjId = thisObjId;
	}

	public void setReturnValue(ObjectReference returnValue) {
		this.returnValue = returnValue;
	}
	
	public void setCollectionType(boolean isCollectionType) {
		this.isCollectionType = isCollectionType;
	}

	public void setTerminated(boolean isTerminated) {
		this.isTerminated = isTerminated;
	}
	
	public String getDeclaringClassName() {
		return Trace.getDeclaringType(signature, isConstructor);
	}

	public String getSignature() {
		return signature;
	}

	public String getCallerSideSignature() {
		return callerSideSignature;
	}

	public String getThisClassName() {
		return thisClassName;
	}

	public String getThisObjId() {
		if (isStatic) return Trace.getNull();
		return thisObjId;
	}

	public ArrayList<ObjectReference> getArguments() {
		if (arguments == null) arguments = new ArrayList<ObjectReference>();
		return arguments;
	}

	public ObjectReference getReturnValue() {
		return returnValue;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isCollectionType() {
		return isCollectionType;
	}
	
	public boolean isTerminated() {
		return isTerminated;
	}

	public long getEntryTime() {
		return entryTime;
	}

	public long getExitTime() {
		if (isTerminated || exitTime == 0L) {
			TracePoint exitPoint = getExitPoint();
			if (!exitPoint.isValid()) return entryTime;
			Statement lastStatament = exitPoint.getStatement();
			if (lastStatament instanceof MethodInvocation) {
				return ((MethodInvocation) lastStatament).getCalledMethodExecution().getExitTime();
			} else {
				return lastStatament.getTimeStamp();
			}
		}
		return exitTime;
	}

	public void setExitTime(long exitTime) {
		this.exitTime = exitTime;
	}

	public void addStatement(Statement statement) {
		statements.add(statement);
		if (statement instanceof MethodInvocation) {
			children.add(((MethodInvocation)statement).getCalledMethodExecution());
		}
	}

	public ArrayList<Statement> getStatements() {
		return statements;
	}

	public ArrayList<MethodExecution> getChildren() {
		return children;
	}

	public void setCaller(MethodExecution callerMethodExecution, int callerStatementExecution) {
		this.callerMethodExecution  = callerMethodExecution;
		this.callerStatementExecution = callerStatementExecution;
	}

	public MethodExecution getParent() {
		return callerMethodExecution;
	}
	
	public TracePoint getEntryPoint() {
		return new TracePoint(this, 0);
	}
	
	public TracePoint getExitPoint() {
		return new TracePoint(this, statements.size() - 1);
	}
	
	public TracePoint getExitOutPoint() {
		return new TracePoint(this, statements.size());
	}

	public MethodExecution getCallerMethodExecution() {
		return callerMethodExecution;
	}

	public int getCallerStatementExecution() {
		return callerStatementExecution;
	}

	public TracePoint getCallerTracePoint() {
		if (callerMethodExecution == null) return null; 
		return new TracePoint(callerMethodExecution, callerStatementExecution);
	}
	
	/**
	 * Traverse backward all descendant method executions of this method execution in the call tree (while the visitor does not return true)
	 * @param visitor   a method execution visitor
	 * @returnÅ@true -- aborted, false -- terminates normally
	 */
	public boolean traverseMethodExecutionsBackward(IMethodExecutionVisitor visitor) {
		if (visitor.preVisitMethodExecution(this)) return true;
		ArrayList<MethodExecution> calledMethodExecutions = getChildren();
		for (int i = calledMethodExecutions.size() - 1; i >= 0; i--) {
			MethodExecution child = calledMethodExecutions.get(i);
			if (child.traverseMethodExecutionsBackward(visitor)) return true;
		}		
		if (visitor.postVisitMethodExecution(this, null)) return true;
		return false;
	}

	/**
	 * Traverse forward all descendant method executions of this method execution in the call tree (within a marked term)
	 * @param visitor   a method execution visitor
	 * @param markStart the start time of a term to traverse
	 * @param markEnd   the end time of a term to traverse
	 */
	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		if (entryTime <= markEnd) {
			if (entryTime >= markStart) {
				ArrayList<MethodExecution> markedChildren = new ArrayList<MethodExecution>();
				visitor.preVisitMethodExecution(this);
				for (int i = 0; i < children.size(); i++) {
					MethodExecution child = children.get(i);
					if (child.getEntryTime() <= markEnd) {
						child.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
						markedChildren.add(child);
					} else {
						break;
					}
				}
				visitor.postVisitMethodExecution(this, markedChildren);
			} else {
				for (int i = 0; i < children.size(); i++) {
					MethodExecution child = children.get(i);
					if (child.getEntryTime() <= markEnd) {
						child.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
					}
				}				
			}
		}
	}

	public void getMarkedMethodSignatures(HashSet<String> signatures, long markStart, long markEnd) {
		if (entryTime <= markEnd) {
			if (entryTime >= markStart) {
				signatures.add(getSignature());
			}
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getMarkedMethodSignatures(signatures, markStart, markEnd);
			}
		}
	}

	public void getUnmarkedMethodSignatures(HashSet<String> signatures, long markStart, long markEnd) {
		if (entryTime < markStart || entryTime > markEnd) {
			signatures.add(getSignature());
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
			}
		} else {
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
			}
		}
	}

	public void getUnmarkedMethodExecutions(HashMap<String, ArrayList<MethodExecution>> allExecutions, long markStart, long markEnd) {
		if (entryTime < markStart || entryTime > markEnd) {
			ArrayList<MethodExecution> executions = allExecutions.get(getSignature());
			if (executions == null) {
				executions = new ArrayList<>();
				allExecutions.put(getSignature(), executions);
			}
			executions.add(this);
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodExecutions(allExecutions, markStart, markEnd);
			}
		} else {
			for (int i = 0; i < children.size(); i++) {
				MethodExecution child = children.get(i);
				child.getUnmarkedMethodExecutions(allExecutions, markStart, markEnd);
			}
		}
	}
	
	public AugmentationInfo getAugmentation() {
		return augmentation;
	}

	public void setAugmentation(AugmentationInfo augmentation) {
		this.augmentation = augmentation;
	}
	
	/**
	 *Å@Search the first method invocation within this method execution that calls a given method execution
	 * @param child a method execution
	 * @return the first method invocation within this method execution that calls child
	 */
	public MethodInvocation getMethodInvocation(MethodExecution child) {
		int callerStatementExecution = child.getCallerStatementExecution();
		if (callerStatementExecution != -1) {
			return (MethodInvocation)statements.get(callerStatementExecution);			
		}
		return null;
	}

	/**
	 * Create TracePoint object that refers to a statement execution within this method execution by specifying its order
	 * @param order the order of a statement execution in the sequence of statement executions within this method execution
	 * @return
	 */
	public TracePoint getTracePoint(int order) {
		if (order < this.getStatements().size()) {
			return new TracePoint(this, order);
		}
		return null;
	}
}
