package org.ntlab.traceAnalysisPlatform.tracer.trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 * Base class for a trace
 * @author Nitta
 *
 */
public class Trace {
	protected static final boolean EAGER_DETECTION_OF_ARRAY_SET = false;		// Whether will an execution of an array set be guessed or not even when its occurrence is not clear? (True value may induce false positives)
	protected static Trace theTrace = null;
	protected HashMap<String, ThreadInstance> threads = new HashMap<String, ThreadInstance>();

	protected Trace() {
	}
	
	/**
	 * Create a trace object from a plain text trace file.
	 * @param file a plain text trace file
	 */
	public Trace(BufferedReader file) {
		try {
			read(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a trace object from a plain text trace file.
	 * @param traceFile the path of a plain text trace file
	 */
	public Trace(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			read(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void read(BufferedReader file) throws IOException {
		// Read a trace file
		String line, prevLine = null;
		String signature;
		String callerSideSignature;
		String threadNo = null;
		String[] methodData;
		String[] argData;
		String[] returnData;
		String[] accessData;
		String[] updateData;
		String thisObjectId;
		String thisClassName;
		boolean isConstractor = false;
		boolean isCollectionType = false;
		boolean isStatic = false;
		long timeStamp = 0L;
		ThreadInstance thread = null;
		HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
		while ((line = file.readLine()) != null) {
			// Decodes the trace file
			if (line.startsWith("Method")) {
				// A method or constructor invocation
				methodData = line.split(":");
				int n = methodData[0].indexOf(',');
				signature = methodData[0].substring(n + 1);
				threadNo = methodData[methodData.length - 1].split(" ")[1];
				thisObjectId = methodData[1];
				thisClassName = methodData[0].substring(0, n).split(" ")[1];
				isConstractor = false;
				isStatic = false;
				if (signature.contains("static ")) {
					isStatic = true;
				}
				callerSideSignature = signature;
				timeStamp = Long.parseLong(methodData[methodData.length - 2]);
				if (prevLine != null) {
					if (prevLine.startsWith("New")) {
						isConstractor = true;							
					} else if (prevLine.startsWith("Invoke")) {
						callerSideSignature = prevLine.split(":")[1];
					}
				}
				thread = threads.get(threadNo);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadNo);
					threads.put(threadNo, thread);
					stack = new Stack<String>();
					stacks.put(threadNo, stack);
				} else {
					stack = stacks.get(threadNo);
				}
				stack.push(line);
				thread.callMethod(signature, callerSideSignature, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
			} else if (line.startsWith("Args")) {
				// The arguments of the last invocation
				argData = line.split(":");
				threadNo = argData[argData.length - 1].split(" ")[1];
				thread = threads.get(threadNo);
				ArrayList<ObjectReference> arguments = new ArrayList<ObjectReference>();
				for (int k = 1; k < argData.length - 2; k += 2) {
					arguments.add(new ObjectReference(argData[k+1], argData[k]));
				}
				thread.setArgments(arguments);
			} else if (line.startsWith("Return")) {
				// Return from a method
				returnData = line.split(":");
				threadNo = returnData[returnData.length - 1].split(" ")[1];
				Stack<String> stack = stacks.get(threadNo);
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.split("\\(")[0].endsWith(line.split("\\(")[1])) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.split("\\(")[0].endsWith(line.split("\\(")[1]));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadNo);
					ObjectReference returnValue = new ObjectReference(returnData[2], returnData[1]);					
					thisObjectId = returnData[2];
					isCollectionType = false;
					String curLine = returnData[0];
					if(curLine.contains("Return call(List")
							|| curLine.contains("Return call(Vector")
							|| curLine.contains("Return call(Iterator")
							|| curLine.contains("Return call(ListIterator")
							|| curLine.contains("Return call(ArrayList")
							|| curLine.contains("Return call(Stack")
							|| curLine.contains("Return call(Hash")
							|| curLine.contains("Return call(Map")
							|| curLine.contains("Return call(Set")
							|| curLine.contains("Return call(Linked")
							|| curLine.contains("Return call(Thread")) {
						isCollectionType = true;
					}
					thread.returnMethod(returnValue, thisObjectId, isCollectionType);
				}
			} else if (line.startsWith("get")) {
				// Field access
				accessData = line.split(":");
				threadNo = accessData[8].split(" ")[1];
				thread = threads.get(threadNo);
				if (thread != null) thread.fieldAccess(accessData[5], accessData[6], accessData[3], accessData[4], accessData[1], accessData[2]);
			} else if (line.startsWith("set")) {
				// Field update
				updateData = line.split(":");
				threadNo = updateData[6].split(" ")[1];
				thread = threads.get(threadNo);
				if (thread != null) thread.fieldUpdate(updateData[3], updateData[4], updateData[1], updateData[2]);
			}
			prevLine = line;
		}
	}
	
	/**
	 * Get the singleton object to record an online trace.
	 * @return
	 */
	public static Trace getInstance() {
		if (theTrace == null) {
			theTrace = new Trace();
		}
		return theTrace;
	}
	
	/**
	 * Get the thread instance by thread ID.
	 * @param threadId thread ID
	 * @return corresponding thread instance
	 */
	public static ThreadInstance getThreadInstance(String threadId) {
		return getInstance().threads.get(threadId);
	}
	
	/**
	 * Get the current method execution in a specified thread (for online analysis).
	 * @param thread a thread
	 * @return the current method execution in thread
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * Get the current trace point in a specified thread (for online analysis).
	 * @param thread a thread
	 * @return the current trace point in thread
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}
	
	/**
	 * Get all threads in this trace.
	 * @return A map from a thread ID to the corresponding thread instance
	 */
	public HashMap<String, ThreadInstance> getAllThreads() {
		return threads;
	}

	/**
	 * Get all method executions in this trace with grouping by method signature.
	 * @return a map from each method signature to its all executions in this trace
	 */
	public HashMap<String, ArrayList<MethodExecution>> getAllMethodExecutions() {
		Iterator<String> threadsIterator = threads.keySet().iterator();
		final HashMap<String, ArrayList<MethodExecution>> results = new HashMap<>();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					String signature = methodExecution.getSignature();
					ArrayList<MethodExecution> executions = results.get(signature);
					if (executions == null) {
						executions = new ArrayList<>();
						results.put(signature, executions);
					}
					executions.add(methodExecution);
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return results;		
	}
	
	/**
	 * Get all method signatures in this trace.
	 * @return all method signatures
	 */
	public HashSet<String> getAllMethodSignatures() {
		final HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					signatures.add(methodExecution.getSignature());
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return signatures;
	}

	/**
	 * Get all method executions in this trace whose signature starts with a given string.
	 * @param methodSignature a string that matches a substring of a method signature
	 * @return all corresponding method executions
	 */
	public ArrayList<MethodExecution> getMethodExecutions(final String methodSignature) {
		Iterator<String> threadsIterator = threads.keySet().iterator();
		final ArrayList<MethodExecution> results = new ArrayList<MethodExecution>();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					if (methodExecution.getSignature().startsWith(methodSignature)) {
						results.add(methodExecution);
					}
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return results;		
	}

	/**
	 * Get the last execution of the method in this trace whose signature starts with a given string.
	 * @param methodSignature a string that matches a substring of a method signature
	 * @return the last execution of the corresponding method
	 */
	public MethodExecution getLastMethodExecution(final String methodSignature) {
		return traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean postVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				if (methodExecution.getSignature().startsWith(methodSignature)) return true;
				return false;
			}
		});
	}

	/**
	 * Get the last execution of the method whose signature starts with a given string and which occurs before a given execution point in this trace.
	 * @param methodSignature a string that matches a substring of a method signature
	 * @param before an execution point in this trace
	 * @return the last execution of the corresponding method before a given execution point 
	 */
	public MethodExecution getLastMethodExecution(final String methodSignature, TracePoint before) {
		return traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean postVisitThread(ThreadInstance thread) { return false; }
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				if (methodExecution.getSignature().startsWith(methodSignature)) return true;
				return false;
			}
		}, before);
	}
	
	/**
	 * Get all method signatures such that some execution of the method starts within a specified term.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of a term
	 * @return corresponding method signatures
	 */
	public HashSet<String> getMarkedMethodSignatures(final long markStart, final long markEnd) {
		final HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMarkedMethodExecutions(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					signatures.add(methodExecution.getSignature());
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			}, markStart, markEnd);
		}	
		return signatures;
	}
	
	/**
	 * Get all method signatures such that some execution of the method starts within a specified term with grouping by method signature.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of a term
	 * @return a map from each method signature to the corresponding method executions
	 */
	public HashMap<String, ArrayList<MethodExecution>> getMarkedMethodExecutions(final long markStart, final long markEnd) {
		final HashMap<String, ArrayList<MethodExecution>>allExecutions = new HashMap<>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMarkedMethodExecutions(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					ArrayList<MethodExecution> executions = allExecutions.get(methodExecution.getSignature());
					if (executions == null) {
						executions = new ArrayList<>();
						allExecutions.put(methodExecution.getSignature(), executions);
					}
					executions.add(methodExecution);
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			}, markStart, markEnd);
		}	
		return allExecutions;
	}
	
	/**
	 * Get all method signatures such that some execution of the method starts outside a specified term.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of a term
	 * @return corresponding method signatures
	 */
	public HashSet<String> getUnmarkedMethodSignatures(long markStart, long markEnd) {
		HashSet<String> signatures = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.getUnmarkedMethodSignatures(signatures, markStart, markEnd);
		}	
		return signatures;
	}
	
	/**
	 * Get all method signatures such that some execution of the method starts outside a specified term with grouping by method signature.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of a term
	 * @return a map from each method signature to the corresponding method executions
	 */
	public HashMap<String, ArrayList<MethodExecution>> getUnmarkedMethodExecutions(long markStart, long markEnd) {
		HashMap<String, ArrayList<MethodExecution>> executions = new HashMap<>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.getUnmarkedMethodExecutions(executions, markStart, markEnd);
		}	
		return executions;
	}
	
	private TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions) {
		MethodExecution lastExecution = rootExecutions.remove(rootExecutions.size() - 1);
		return getLastMethodEntryInThread(rootExecutions, lastExecution.getExitOutPoint());
	}

	private TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions, TracePoint start) {
		return getLastMethodEntryInThread(rootExecutions, start, -1L);
	}
	
	private TracePoint getLastMethodEntryInThread(ArrayList<MethodExecution> rootExecutions, TracePoint start, final long before) {
		final TracePoint cp[] = new TracePoint[1];
		cp[0] = start;
		for (;;) {
			if (!cp[0].isStepBackOut() && traverseMethodExecutionsInCallTreeBackward(
					new IMethodExecutionVisitor() {
						@Override
						public boolean preVisitThread(ThreadInstance thread) { return false; }
						@Override
						public boolean preVisitMethodExecution(MethodExecution methodExecution) { return false; }
						@Override
						public boolean postVisitThread(ThreadInstance thread) { return false; }
						@Override
						public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
							if (methodExecution.getEntryTime() < before || before == -1L) {
								cp[0] = methodExecution.getEntryPoint();
								return true;
							}
							return false;
						}
					}, cp[0])) {
				return cp[0];
			}
			if (rootExecutions.size() == 0) break;
			MethodExecution lastExecution = rootExecutions.remove(rootExecutions.size() - 1);
			cp[0] = lastExecution.getExitOutPoint();
		}
		return null;
	}
	
	public TracePoint getCreationTracePoint(final ObjectReference newObjectId, TracePoint before) {
		before = before.duplicate();
		before = traverseStatementsInTraceBackward(
				new IStatementVisitor() {
					@Override
					public boolean preVisitStatement(Statement statement) {
						if (statement instanceof MethodInvocation) {
							MethodInvocation mi = (MethodInvocation)statement;
							if (mi.getCalledMethodExecution().isConstructor() 
									&& mi.getCalledMethodExecution().getReturnValue().equals(newObjectId)) {
								return true;
							}
						}
						return false;
					}
					@Override
					public boolean postVisitStatement(Statement statement) { return false; }
				}, before);
		if (before != null) {
			return before;
		}
		return null;
	}

	/**
	 * Search a field set before a given execution point in the trace.
	 * @param ref the referred object by a field and the container object to search
	 * @param before an execution point
	 * @return the corresponding execution point
	 */
	public TracePoint getFieldUpdateTracePoint(final Reference ref, TracePoint before) {
		before = before.duplicate();
		final String srcType = ref.getSrcClassName();
		final String dstType = ref.getDstClassName();
		final String srcObjId = ref.getSrcObjectId();
		final String dstObjId = ref.getDstObjectId();
		
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate fu = (FieldUpdate)statement;
					if (fu.getContainerObjId().equals(srcObjId) 
							&& fu.getValueObjId().equals(dstObjId)) {
						// Totally corresponds.
						return true;
					} else if ((srcObjId == null || isNull(srcObjId)) && fu.getContainerClassName().equals(srcType)) {
						if ((dstObjId == null || isNull(dstObjId)) && fu.getValueClassName().equals(dstType)) {
							// In the case that object ID is not specified in ref.
							ref.setSrcObjectId(fu.getContainerObjId());
							ref.setDstObjectId(fu.getValueObjId());
							return true;
						} else if (fu.getValueObjId().equals(dstObjId)) {
							// In the case of static field set.
							ref.setSrcObjectId(srcObjId);
							ref.setDstClassName(dstType);
							return true;
						}
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return before;			
		}
		return null;
	}

	/**
	 * Search an addition of an object to a collection object before a given execution point in the trace.
	 * @param ref the added object and the collection object to search
	 * @param before an execution point
	 * @return the corresponding execution point
	 */
	public TracePoint getCollectionAddTracePoint(final Reference ref, TracePoint before) {
		final TracePoint[] result = new TracePoint[1];
		if (traverseMethodEntriesInTraceBackward(new IMethodExecutionVisitor() {
			@Override
			public boolean preVisitThread(ThreadInstance thread) {
				return false;
			}
			@Override
			public boolean postVisitThread(ThreadInstance thread) {
				return false;
			}			
			@Override
			public boolean preVisitMethodExecution(MethodExecution methodExecution) {
				return false;
			}
			@Override
			public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
				String srcType = ref.getSrcClassName();
				String dstType = ref.getDstClassName();
				String srcObjId = ref.getSrcObjectId();
				String dstObjId = ref.getDstObjectId();
				if (methodExecution.isCollectionType() && isCollectionAdd(methodExecution.getSignature())) {
					if (dstObjId != null && methodExecution.getThisObjId().equals(srcObjId)) {
						ArrayList<ObjectReference> args = methodExecution.getArguments();
						for (int i = 0; i < args.size(); i++) {
							ObjectReference arg = args.get(i);
							if (arg.getId().equals(dstObjId)) {
								ref.setSrcClassName(methodExecution.getThisClassName());
								ref.setDstClassName(arg.getActualType());
								result[0] = methodExecution.getCallerTracePoint();
								return true;								
							}
						}
					} else if (dstObjId == null && methodExecution.getThisClassName().equals(srcType)) {
						ArrayList<ObjectReference> args = methodExecution.getArguments();						
						for (int i = 0; i < args.size(); i++) {
							ObjectReference arg = args.get(i);
							if (arg.getActualType().equals(dstType)) {
								ref.setSrcObjectId(methodExecution.getThisObjId());
								ref.setDstObjectId(arg.getId());
								result[0] = methodExecution.getCallerTracePoint();
								return true;								
							}
						}
					}
				}
				return false;
			}
		}, before) != null) {
			return result[0];
		}
		return null;
	}

	/**
	 * Search a set of an element to an array object before a given execution point in the trace.
	 * @param ref the new element and the array object to search
	 * @param before an execution point
	 * @return the corresponding execution point
	 */
	public TracePoint getArraySetTracePoint(final Reference ref, TracePoint before) {
		final TracePoint start = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
				@Override
				public boolean preVisitStatement(Statement statement) {
					if (statement instanceof FieldAccess) {
						if (isArraySet(ref, start)) {
							return true;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitStatement(Statement statement) { return false; }
			}, start);
		if (before != null) {
			return before;
		}
		return null;
	}
	
	private boolean isCollectionAdd(String methodSignature) {
		return (methodSignature.contains("add(") || methodSignature.contains("set(") || methodSignature.contains("put(") || methodSignature.contains("push("));
	}
	
	private boolean isArraySet(Reference ref, TracePoint fieldAccessPoint) {
		FieldAccess fieldAccess = (FieldAccess)fieldAccessPoint.getStatement();
		String srcObjId = ref.getSrcObjectId();
		String dstObjId = ref.getDstObjectId();
		if (fieldAccess.getValueClassName().startsWith("[L") 
				&& fieldAccess.getValueObjId().equals(srcObjId)) {
			TracePoint p = fieldAccessPoint.duplicate();
			while (p.stepBackOver()) {
				Statement statement = p.getStatement();
				if (statement instanceof MethodInvocation) {
					MethodExecution calledMethod = ((MethodInvocation)statement).getCalledMethodExecution();
					if (calledMethod.getReturnValue().getId().equals(dstObjId)) {
						ref.setSrcClassName(fieldAccess.getValueClassName());
						ref.setDstClassName(calledMethod.getReturnValue().getActualType());
						return true;
					} else if (dstObjId == null || isNull(dstObjId) && calledMethod.getReturnValue().getActualType().equals(ref.getDstClassName())) {
						ref.setSrcObjectId(fieldAccess.getValueObjId());
						ref.setDstObjectId(calledMethod.getReturnValue().getId());
						return true;								
					}				
				}
				if (EAGER_DETECTION_OF_ARRAY_SET) {
					if (statement instanceof FieldAccess) {
						if (((FieldAccess)statement).getContainerObjId().equals(dstObjId)) {
							ref.setSrcClassName(fieldAccess.getValueClassName());
							ref.setDstClassName(((FieldAccess)statement).getContainerClassName());
							return true;
						} else if (dstObjId == null || isNull(dstObjId) && ((FieldAccess)statement).getContainerClassName().equals(ref.getDstClassName())) {
							ref.setSrcObjectId(fieldAccess.getValueObjId());
							ref.setDstObjectId(((FieldAccess)statement).getContainerObjId());
							return true;
						}
					}
				}
			}
			ArrayList<ObjectReference> args = fieldAccessPoint.getMethodExecution().getArguments();
			int argindex = args.indexOf(new ObjectReference(dstObjId));
			if (argindex != -1) {
				ref.setSrcClassName(fieldAccess.getValueClassName());
				ref.setDstClassName(args.get(argindex).getActualType());
				return true;
			} else if (dstObjId == null || isNull(dstObjId)) {
				for (int j = 0; j < args.size(); j++) {
					if (args.get(j).getActualType().equals(ref.getDstClassName())) {
						ref.setSrcObjectId(fieldAccess.getValueObjId());
						ref.setDstObjectId(args.get(j).getId());
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Traverse backward all method entries in this trace. 
	 * @param visitor a method visitor (only postVisitMethodExecution() is called back)
	 * @return a method execution where the traverse is aborted
	 */
	public MethodExecution traverseMethodEntriesInTraceBackward(IMethodExecutionVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		// Search the last executed method in each thread.
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (; threadsIterator.hasNext();) {
			String threadId = threadsIterator.next();
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			TracePoint threadLastTp = getLastMethodEntryInThread(rootExecutions);
			threadLastPoints.put(threadId, threadLastTp);
			if (threadLastTp != null) {
				long threadLastTime = threadLastTp.getMethodExecution().getEntryTime();
				if (traceLastTime < threadLastTime) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = threadLastTime;
					traceLastThread = threadId;
				}
			}
		}
		return traverseMethodEntriesInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * Traverse backward all method entries before a given execution point in this trace. 
	 * @param visitor a method visitor (only postVisitMethodExecution() is called back)
	 * @param before an execution point in this trace
	 * @return a method execution where the traverse is aborted
	 */
	public MethodExecution traverseMethodEntriesInTraceBackward(IMethodExecutionVisitor visitor, TracePoint before) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		ThreadInstance thread = threads.get(before.getStatement().getThreadNo());
		ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)thread.getRoot().clone();		
		for (int n = rootExecutions.size() - 1; n >= 0; n--) {
			MethodExecution root = rootExecutions.get(n);
			if (root.getEntryTime() > before.getMethodExecution().getEntryTime()) {
				rootExecutions.remove(n);
			} else {
				break;
			}
		}
		if (rootExecutions.size() > 0) {
			rootExecutions.remove(rootExecutions.size() - 1);
		}
		before = getLastMethodEntryInThread(rootExecutions, before);
		for (; threadsIterator.hasNext();) {
			String threadId = threadsIterator.next();
			ThreadInstance t = threads.get(threadId);
			if (t == thread) {
				threadRoots.put(threadId, rootExecutions);
				traceLastThread = threadId;
				threadLastPoints.put(threadId, before);
			} else {
				ArrayList<MethodExecution> rootExes = (ArrayList<MethodExecution>)t.getRoot().clone();
				threadRoots.put(threadId, rootExes);
				MethodExecution threadLastExecution = rootExes.remove(rootExes.size() - 1);
				TracePoint threadBeforeTp = getLastMethodEntryInThread(rootExes, threadLastExecution.getExitOutPoint(), before.getMethodExecution().getEntryTime());
				threadLastPoints.put(threadId, threadBeforeTp);
				if (threadBeforeTp != null) {
					long threadLastTime = threadBeforeTp.getMethodExecution().getEntryTime();
					if (traceLastTime2 < threadLastTime) {
						traceLastTime2 = threadLastTime;
						traceLastThread2 = threadId;
					}
				}
			}
		}
		return traverseMethodEntriesInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);		
	}
	
	private MethodExecution traverseMethodEntriesInTraceBackwardSub(
			final IMethodExecutionVisitor visitor, 
			HashMap<String, ArrayList<MethodExecution>> threadRoots, HashMap<String, TracePoint> threadLastPoints, 
			String traceLastThread, String traceLastThread2, long traceLastTime2) {
		// Traverse backward all method executions in this trace synchronizing all threads
		for (;;) {
			TracePoint threadLastTp = threadLastPoints.get(traceLastThread);
			MethodExecution threadLastExecution = threadLastTp.getMethodExecution();
			do {
				threadLastTp.stepBackOver();
				threadLastTp = getLastMethodEntryInThread(threadRoots.get(traceLastThread), threadLastTp);
				if (threadLastTp == null) break;
				if (visitor.postVisitMethodExecution(threadLastExecution, threadLastExecution.getChildren())) {
					return threadLastExecution;
				}
				threadLastExecution = threadLastTp.getMethodExecution();
			} while (threadLastExecution.getEntryTime() > traceLastTime2);
			threadLastPoints.put(traceLastThread, threadLastTp);
			traceLastThread = traceLastThread2;
			
			traceLastTime2 = 0;
			traceLastThread2 = null;
			boolean continueTraverse = false;
			Iterator<String> threadIterator = threadLastPoints.keySet().iterator();
			for (; threadIterator.hasNext();) {
				String threadId = threadIterator.next();
				if (!threadId.equals(traceLastThread)) {
					TracePoint lastTp = threadLastPoints.get(threadId);
					if (lastTp != null) {
						continueTraverse = true;
						long threadLastTime = lastTp.getMethodExecution().getEntryTime();
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
	
	/**
	 * Get all method executions in this trace that start within a specified term.
	 * @param visitor a method execution visitor
	 * @param markStart the start time of a term
	 * @param markEnd the end time of a term
	 */
	public void traverseMarkedMethodExecutions(IMethodExecutionVisitor visitor, long markStart, long markEnd) {
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMarkedMethodExecutions(visitor, markStart, markEnd);
		}		
	}
	
	/**
	 * Traverse forward all statements in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTrace(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadCurPoints = new HashMap<String, TracePoint>();
		// Search the first method execution in each thread.
		long traceCurTime = -1;
		String traceCurThread = null;
		long traceCurTime2 = -1;
		String traceCurThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> roots = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, roots);
			TracePoint threadCurTp;
			do {
				MethodExecution threadCurExecution = roots.remove(0);
				threadCurTp = threadCurExecution.getEntryPoint();
			} while (!threadCurTp.isValid() && roots.size() > 0);
			if (threadCurTp.isValid()) {
				threadCurPoints.put(threadId, threadCurTp);
				long methodEntry = threadCurTp.getMethodExecution().getEntryTime();
				if (traceCurTime == -1 || traceCurTime > methodEntry) {
					traceCurTime2 = traceCurTime;
					traceCurThread2 = traceCurThread;
					traceCurTime = methodEntry;
					traceCurThread = threadId;
				}
			} else {
				threadCurPoints.put(threadId, null);				
			}
		}
		return traverseStatementsInTraceSub(visitor, threadRoots, threadCurPoints, traceCurThread, traceCurThread2, traceCurTime2);
	}
	
	private TracePoint traverseStatementsInTraceSub(IStatementVisitor visitor,
			HashMap<String, ArrayList<MethodExecution>> threadRoots,
			HashMap<String, TracePoint> threadCurPoints, 
			String curThreadId, String nextThreadId, long nextThreadTime) {
		// Traverse statements forward with synchronizing all threads.
		for (;;) {
			TracePoint curTp = threadCurPoints.get(curThreadId);
			while (curTp != null 
					&& (curTp.getStatement().getTimeStamp() <= nextThreadTime || nextThreadTime == -1)) {
				Statement statement = curTp.getStatement();
				if (visitor.preVisitStatement(statement)) return curTp;
				if (!(statement instanceof MethodInvocation)) {
					if (visitor.postVisitStatement(statement)) return curTp;
				}
				curTp.stepNoReturn();
				if (!curTp.isValid()) {
					while (!curTp.stepOver()) {
						if (curTp.isValid()) {
							if (visitor.postVisitStatement(curTp.getStatement())) return curTp;
						} else {
							ArrayList<MethodExecution> roots = threadRoots.get(curThreadId);
							while (!curTp.isValid() && roots.size() > 0) {
								MethodExecution firstExecution = roots.remove(0);
								curTp = firstExecution.getEntryPoint();							
							}
							if (curTp.isValid()) {
								threadCurPoints.put(curThreadId, curTp);
							} else {
								threadCurPoints.put(curThreadId, null);
								curTp = null;
							}						
							break;
						}
					}
				}
			}
			curThreadId = nextThreadId;
			if (curThreadId == null) break;
			nextThreadTime = -1;
			nextThreadId = null;
			boolean continueTraverse = false;
			for (String threadId: threadCurPoints.keySet()) {
				if (!threadId.equals(curThreadId)) {
					TracePoint threadTp = threadCurPoints.get(threadId);
					if (threadTp != null) {
						continueTraverse = true;
						long threadTime = threadTp.getStatement().getTimeStamp();
						if (threadTime > 0 && (nextThreadTime == -1 || nextThreadTime > threadTime)) {
							nextThreadTime = threadTime;
							nextThreadId = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadCurPoints.get(curThreadId) == null) break;
		}
		return null;
	}
	
	/**
	 * Traverse backward all statements in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		// Search the last method execution in each thread.
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (; threadsIterator.hasNext();) {
			String threadId = threadsIterator.next();
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> root = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, root);
			TracePoint threadLastTp;
			do {
				MethodExecution threadLastExecution = root.remove(root.size() - 1);
				threadLastTp = threadLastExecution.getExitPoint();
			} while (!threadLastTp.isValid() && root.size() > 0);
			if (threadLastTp.isValid()) {
				threadLastPoints.put(threadId, threadLastTp);
				if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, threadLastTp)) return threadLastTp;
				long methodEntry = threadLastTp.getMethodExecution().getEntryTime();
				if (traceLastTime < methodEntry) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = methodEntry;
					traceLastThread = threadId;
				}
			} else {
				threadLastPoints.put(threadId, null);				
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * Traverse backward all statements before a given execution point in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @param before an execution point 
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, TracePoint before) {
		if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, before)) return before;
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		ThreadInstance thread = threads.get(before.getStatement().getThreadNo());
		for (; threadsIterator.hasNext();) {
			String threadId = threadsIterator.next();
			ThreadInstance t = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)t.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			if (t == thread) {
				traceLastThread = threadId;
				threadLastPoints.put(threadId, before);
				for (int n = rootExecutions.size() - 1; n >= 0; n--) {
					MethodExecution root = rootExecutions.get(n);
					if (root.getEntryTime() > before.getMethodExecution().getEntryTime()) {
						rootExecutions.remove(n);
					} else {
						break;
					}
				}
				if (rootExecutions.size() > 0) {
					rootExecutions.remove(rootExecutions.size() - 1);
				}
			} else {
				MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
				TracePoint threadBeforeTp = getLastMethodEntryInThread(rootExecutions, threadLastExecution.getExitOutPoint(), before.getMethodExecution().getEntryTime());
				threadLastPoints.put(threadId, threadBeforeTp);
				if (threadBeforeTp != null) {
					long threadLastTime = threadBeforeTp.getMethodExecution().getEntryTime();
					if (traceLastTime2 < threadLastTime) {
						traceLastTime2 = threadLastTime;
						traceLastThread2 = threadId;
					}
				}
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}

	private TracePoint traverseStatementsInTraceBackwardSub(IStatementVisitor visitor,
			HashMap<String, ArrayList<MethodExecution>> threadRoots,
			HashMap<String, TracePoint> threadLastPoints, 
			String traceLastThread, String traceLastThread2, long traceLastTime2) {
		// Traverse statements backward with synchronizing all threads.
		for (;;) {
			TracePoint lastTp = threadLastPoints.get(traceLastThread);
			do {
				if (lastTp.stepBackOver()) {
					if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;					
				} else {
					if (lastTp.isValid()) {
						if (visitor.postVisitStatement(lastTp.getStatement())) return lastTp;
					} else {
						ArrayList<MethodExecution> root = threadRoots.get(traceLastThread);
						while (!lastTp.isValid() && root.size() > 0) {
							MethodExecution lastExecution = root.remove(root.size() - 1);
							lastTp = lastExecution.getExitPoint();							
						}
						if (lastTp.isValid()) {
							threadLastPoints.put(traceLastThread, lastTp);
							if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, lastTp)) return lastTp;
						} else {
							threadLastPoints.put(traceLastThread, null);
							break;
						}
					}
				}
			} while (lastTp.getMethodExecution().getEntryTime() >= traceLastTime2);
			traceLastThread = traceLastThread2;
			
			traceLastTime2 = 0;
			traceLastThread2 = null;
			boolean continueTraverse = false;
			Iterator<String> threadsIterator = threadLastPoints.keySet().iterator();
			for (; threadsIterator.hasNext();) {
				String threadId = threadsIterator.next();
				if (!threadId.equals(traceLastThread)) {
					TracePoint threadLastTp = threadLastPoints.get(threadId);
					if (threadLastTp != null) {
						continueTraverse = true;
						long threadLastTime = threadLastTp.getMethodExecution().getEntryTime();
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
		
	/**
	 * Traverse backward all statement executions in the call tree before a specified execution point (while the visitor returns false).
	 * @param visitor a statement execution visitor
	 * @param before an execution point (also fixing a target thread)
	 * @return true -- aborted, false -- terminates normally
	 */
	public boolean traverseStatementsInCallTreeBackward(IStatementVisitor visitor, TracePoint before) {
		for (;;) {
			if (traverseStatamentsInCallTreeBackwardNoReturn(visitor, before)) return true;
			before.stepBackOver();
			if (!before.isValid()) break;
		}
		return false;
	}

	/**
	 * Traverse backward all statement executions in the call tree without returning to the parent before a specified execution point.
	 * @param visitor a statement execution visitor
	 * @param before an execution point (also fixing a target thread)
	 * @return true -- aborted, false -- cannot traverse any more without return
	 */
	private boolean traverseStatamentsInCallTreeBackwardNoReturn(IStatementVisitor visitor, TracePoint before) {
		for (;;) {
			Statement statement = before.getStatement();
			if (statement instanceof MethodInvocation) {
				// preVisit and postVisit are separately called back in the case of method invocation.
				if (visitor.preVisitStatement(statement)) return true;
				before.stepBackNoReturn();
				if (!before.isValid()) {
					// In the case that no statement execution is recorded in the callee method.
					before.stepBackOver();
					if (visitor.postVisitStatement(statement)) return true;
					if (before.isMethodEntry()) return false;
					before.stepBackOver();
				}
			} else {
				if (visitor.preVisitStatement(statement)) return true;
				if (visitor.postVisitStatement(statement)) return true;
				if (before.isMethodEntry()) return false;
				before.stepBackNoReturn();
			}
		}
	}
	
	/**
	 * Traverse backward all method executions in the call tree before a specified execution point (while the visitor returns false).
	 * @param visitor a method execution visitor
	 * @param before an execution point (also fixing a target thread)
	 * @return true -- aborted, false -- terminates normally
	 */
	public boolean traverseMethodExecutionsInCallTreeBackward(IMethodExecutionVisitor visitor, TracePoint before) {
		ArrayList<MethodExecution> prevMethodExecutions = before.getPreviouslyCalledMethods();
		for (int i = prevMethodExecutions.size() - 1; i >= 0; i--) {
			MethodExecution child = prevMethodExecutions.get(i);
			if (child.traverseMethodExecutionsBackward(visitor)) return true;
		}
		MethodExecution methodExecution = before.getMethodExecution();
		if (visitor.postVisitMethodExecution(methodExecution, null)) return true;
		TracePoint caller = methodExecution.getCallerTracePoint();
		if (caller != null) {
			if (traverseMethodExecutionsInCallTreeBackward(visitor, caller)) return true;
		}
		return false;
	}

	public static String getDeclaringType(String methodSignature, boolean isConstructor) {
		if (isConstructor) {
			String[] fragments = methodSignature.split("\\(");
			return fragments[0].substring(fragments[0].lastIndexOf(' ') + 1);			
		}
		String[] fragments = methodSignature.split("\\(");
		return fragments[0].substring(fragments[0].lastIndexOf(' ') + 1, fragments[0].lastIndexOf('.'));		
	}		
	
	public static String getMethodName(String methodSignature) {
		String[] fragments = methodSignature.split("\\(");
		String[] fragments2 = fragments[0].split("\\.");
		return fragments2[fragments2.length - 1];
	}
	
	public static String getReturnType(String methodSignature) {
		String[] fragments = methodSignature.split(" ");
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].equals("public") && !fragments[i].equals("private") && !fragments[i].equals("protected")
					&& !fragments[i].equals("abstract") && !fragments[i].equals("final") && !fragments[i].equals("static")
					&& !fragments[i].equals("synchronized") && !fragments[i].equals("native")) {
				return fragments[i];
			}
		}
		return "";
	}
	
	public static boolean isNull(String objectId) {
		return objectId.equals("0");
	}
	
	public static String getNull() {
		return "0";
	}

	public static boolean isPrimitive(String typeName) {
		if (typeName.equals("int") 
				|| typeName.equals("boolean") 
				|| typeName.equals("long") 
				|| typeName.equals("double") 
				|| typeName.equals("float") 
				|| typeName.equals("char") 
				|| typeName.equals("byte") 
				|| typeName.equals("java.lang.Integer") 
				|| typeName.equals("java.lang.Boolean") 
				|| typeName.equals("java.lang.Long") 
				|| typeName.equals("java.lang.Double") 
				|| typeName.equals("java.lang.Float") 
				|| typeName.equals("java.lang.Character") 
				|| typeName.equals("java.lang.Byte")) return true;
		return false;
	}
}
