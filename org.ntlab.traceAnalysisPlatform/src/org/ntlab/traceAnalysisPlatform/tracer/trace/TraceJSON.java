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
 * A trace specific to JSON format (specific to Javassist)
 * @author Nitta
 *
 */
public class TraceJSON extends Trace {
	private HashMap<String, ClassInfo> classes = new HashMap<>();
	
	private TraceJSON() {
		
	}

	/**
	 * Create a trace object from a JSON trace file.
	 * @param file a JSON trace file
	 */
	public TraceJSON(BufferedReader file) {
		try {
			readJSON(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a trace object from a JSON trace file.
	 * @param traceFile the path of a JSON trace file
	 */
	public TraceJSON(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			readJSON(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readJSON(BufferedReader file) throws IOException {
		// Read a trace file.
		String line = null;
		String[] type;
		String[] classNameData;
		String[] pathData;
		String[] signature;
		String[] receiver;
		String[] arguments;
		String[] lineData;
		String[] threadId;
		String[] thisObj;
		String[] containerObj;
		String[] valueObj;
		String[] returnValue;
		String[] arrayObj;
		String[] thisData;
		String[] containerData;
		String[] valueData;
		String[] returnData;
		String[] fieldData;
		String[] arrayData;
		String[] blockIdData;
		String[] incomingsData;
		String[] dimensionData;
		String[] indexData;
		String className;
		String classPath;
		String loaderPath;
		String time;
		String thisObjectId;
		String thisClassName;
		String containerObjectId;
		String containerClassName;
		String valueObjectId;
		String valueClassName;
		String returnClassName;
		String returnObjectId;
		String arrayObjectId;
		String arrayClassName;
		String shortSignature;
		boolean isConstractor = false;
		boolean isCollectionType = false;
		boolean isStatic = false;
		int dimension;
		int index;
		int blockId;
		int incomings;
		int lineNum;
		long timeStamp = 0L;
		ThreadInstance thread = null;
		HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
		while ((line = file.readLine()) != null) {
			// Decodes the trace file.
			if (line.startsWith("{\"type\":\"classDef\"")) {
				// Class definition
				type = line.split(",\"name\":\"");
				classNameData = type[1].split("\",\"path\":\"");
				className = classNameData[0];
				pathData = classNameData[1].split("\",\"loaderPath\":\"");
				classPath = pathData[0].substring(1);								// remove the top character '/'
				loaderPath = pathData[1].substring(1, pathData[1].length() - 3);	// remove the top character '/' and the tail string "\"},"
				initializeClass(className, classPath, loaderPath);
			} else if (line.startsWith("{\"type\":\"methodCall\"")) {
				// A method call (in a caller side)
				type = line.split(",\"callerSideSignature\":\"");
				signature = type[1].split("\",\"threadId\":");
				threadId = signature[1].split(",\"lineNum\":");
				lineNum = Integer.parseInt(threadId[1].substring(0, threadId[1].length() - 2));	// remove the tail string "},"
				thread = threads.get(threadId[0]);
				thread.preCallMethod(signature[0], lineNum);
			} else if (line.startsWith("{\"type\":\"methodEntry\"")) {
				// A method entry
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				isConstractor = false;
				isStatic = false;
				if (signature[0].contains("static ")) {
					isStatic = true;
				}
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// Record a method entry.
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// Record its arguments.
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"constructorEntry\"")) {
				// A constructor entry.
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"class\":\"");
				receiver = signature[1].split("\",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisClassName = receiver[0];
				thisObjectId = "0";
				isConstractor = true;
				isStatic = false;
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// Record a constructor entry.
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// Record its arguments.
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"methodExit\"")) {
				// Return from a method.
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"returnValue\":");
				returnValue = receiver[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				returnData = parseClassNameAndObjectId(returnValue[0]);
				returnClassName = returnData[0];
				returnObjectId = returnData[1];
				shortSignature = signature[0];
				time = threadId[1].substring(0, threadId[1].length() - 2);			// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							if (!stack.isEmpty()) line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.util.Collection")
							|| thisClassName.contains("java.util.Arrays")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// Record a return from a method.
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"constructorExit\"")) {
				// Return from a constructor.
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"returnValue\":");
				returnValue = signature[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				returnData = parseClassNameAndObjectId(returnValue[0]);
				thisClassName = returnClassName = returnData[0];
				thisObjectId = returnObjectId = returnData[1];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				shortSignature = signature[0];
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// Record a return from a constructor.
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"fieldGet\"")) {
				// A field get
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"this\":");
				thisObj = fieldData[1].split(",\"container\":");
				containerObj = thisObj[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(thisObj[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				// Record a field get.
				if (thread != null) thread.fieldAccess(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"fieldSet\"")) {
				// A field set
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"container\":");
				containerObj = fieldData[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				// Record a field set.
				if (thread != null) thread.fieldUpdate(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayCreate\"")) {
				// An array create
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"dimension\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				dimensionData = arrayObj[1].split(",\"threadId\":");
				dimension = Integer.parseInt(dimensionData[0]);
				threadId = dimensionData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayCreate(arrayClassName, arrayObjectId, dimension, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arraySet\"")) {
				// A set to an array element
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayGet\"")) {
				// A get from an array element
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"blockEntry\"")) {
				// An entry of a basic block
				type = line.split(",\"methodSignature\":\"");
				signature = type[1].split("\",\"blockId\":");
				blockIdData = signature[1].split(",\"incomings\":");
				blockId = Integer.parseInt(blockIdData[0]);
				incomingsData = blockIdData[1].split(",\"threadId\":");
				incomings = Integer.parseInt(incomingsData[0]);
				threadId = incomingsData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// remove the tail string "},"
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.blockEnter(blockId, incomings, lineNum, timeStamp);
			}
		}
	}

	/**
	 * Parse a JSON object containing a class name and an object ID.
	 * @param classNameAndObjectIdJSON a JSON object in this trace
	 * @return
	 */
	protected String[] parseClassNameAndObjectId(String classNameAndObjectIdJSON) {
		// Remove the top string "{\"class\":\"" with ten chanacters and the tail character "}".
		return classNameAndObjectIdJSON.substring(10, classNameAndObjectIdJSON.length() - 1).split("\",\"id\":");
	}
	
	/**
	 * Parse a JSON array containing arguments
	 * @param a JSON array in this trace
	 * @return
	 */
	protected ArrayList<ObjectReference> parseArguments(String[] arguments) {
		String[] argData;
		argData = arguments[0].substring(1, arguments[0].length() - 1).split(",");		// Remove the top '[' and the tail ']'.
		ArrayList<ObjectReference> argumentsData = new ArrayList<ObjectReference>();
		for (int k = 0; k < argData.length - 1; k += 2) {
			argumentsData.add(new ObjectReference(argData[k+1].substring(5, argData[k+1].length() - 1), argData[k].substring(10, argData[k].length() - 1)));
		}
		return argumentsData;
	}
	
	/**
	 * Get the singleton object to record an online trace.
	 * @return
	 */
	public static TraceJSON getInstance() {
		if (theTrace == null) {
			theTrace = new TraceJSON();
		}
		return (TraceJSON)theTrace;
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
	 * @param thread a thread in this trace
	 * @return thread the current method in the thread
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * Get the current execution point in a specified thread (for online analysis).
	 * @param thread a thread in this trace
	 * @return thread the current execution point in the thread
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}

	public void initializeClass(String name, String path, String loaderPath) {
		classes.put(name, new ClassInfo(name, path, loaderPath));
	}

	public ClassInfo getClassInfo(String className) {
		return classes.get(className);
	}
	
	public TracePoint getArraySetTracePoint(final Reference ref, TracePoint before) {
		final TracePoint start = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
				@Override
				public boolean preVisitStatement(Statement statement) {
					if (statement instanceof ArrayUpdate) {
						ArrayUpdate arraySet = (ArrayUpdate)start.getStatement();
						String srcObjId = ref.getSrcObjectId();
						String dstObjId = ref.getDstObjectId();
						String srcClassName = ref.getSrcClassName();
						String dstClassName = ref.getDstClassName();
						if ((srcObjId != null && srcObjId.equals(arraySet.getArrayObjectId())) 
							|| (srcObjId == null || isNull(srcObjId)) && srcClassName.equals(arraySet.getArrayClassName())) {
							if ((dstObjId != null && dstObjId.equals(arraySet.getValueObjectId()))
								|| ((dstObjId == null || isNull(dstObjId)) && dstClassName.equals(arraySet.getValueClassName()))) {
								if (srcObjId == null) {
									ref.setSrcObjectId(arraySet.getArrayObjectId());
								} else if (srcClassName == null) {
									ref.setSrcClassName(arraySet.getArrayClassName());
								}
								if (dstObjId == null) {
									ref.setDstObjectId(arraySet.getValueObjectId());
								} else if (dstClassName == null) {
									ref.setDstClassName(arraySet.getValueClassName());
								}
								return true;
							}
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
	
	/**
	 * Get all executions of basic blocks in this trace.
	 * @return All basic blocks (method signature:block ID)
	 */
	public HashSet<String> getAllBlocks() {
		final HashSet<String> blocks = new HashSet<String>();
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
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * Get all executions of basic blocks that start within a specified term.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of the term
	 * @return corresponding basic blocks (method signature:block ID)
	 */
	public HashSet<String> getMarkedBlocks(final long markStart, final long markEnd) {
		final HashSet<String> blocks = new HashSet<String>();
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// Terminate normally
					if (methodExecution.getEntryTime() > markEnd) return false;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							if (entryTime >= markStart && entryTime <= markEnd) {
								blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
							}
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * Get all flows between basic blocks.
	 * @return all flows between basic blocks (method signature:the source basic block ID:the destination basic block ID)
	 */
	public HashSet<String> getAllFlows() {
		final HashSet<String> flows = new HashSet<String>();
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
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (prevBlockId != -1) {
								flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
							} else {
								flows.add(methodExecution.getSignature() + ":" + curBlockID);
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}
	
	/**
	 * Get all flows between basic blocks that start within a specified term.
	 * @param markStart the start time of a term
	 * @param markEnd the end time of the term
	 * @return corresponding flows between basic blocks (method signature:the source basic block ID:the destination basic block ID)
	 */
	public HashSet<String> getMarkedFlows(final long markStart, final long markEnd) {
		final HashSet<String> flows = new HashSet<String>();
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
					if (!methodExecution.isTerminated() && methodExecution.getExitTime() < markStart) return true;		// Terminate normally
					if (methodExecution.getEntryTime() > markEnd) return false;
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (entryTime >= markStart && entryTime <= markEnd) {
								if (prevBlockId != -1) {
									flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
								} else {
									flows.add(methodExecution.getSignature() + ":" + curBlockID);
								}
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}
	
	/**
	 * Traverse backward all statements in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor) {
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		// Search the last method execution in each thread.
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance thread = threads.get(threadId);
			ArrayList<MethodExecution> roots = (ArrayList<MethodExecution>)thread.getRoot().clone();
			threadRoots.put(threadId, roots);
			TracePoint threadLastTp;
			do {
				MethodExecution threadLastExecution = roots.remove(roots.size() - 1);
				threadLastTp = threadLastExecution.getExitPoint();
			} while (!threadLastTp.isValid() && roots.size() > 0);
			if (threadLastTp.isValid()) {
				threadLastPoints.put(threadId, threadLastTp);
				long threadLastTime;
				if (threadLastTp.getStatement() instanceof MethodInvocation) {
					threadLastTime = ((MethodInvocation) threadLastTp.getStatement()).getCalledMethodExecution().getExitTime();
				} else {
					threadLastTime = threadLastTp.getStatement().getTimeStamp();
				}				
				if (traceLastTime < threadLastTime) {
					traceLastTime2 = traceLastTime;
					traceLastThread2 = traceLastThread;
					traceLastTime = threadLastTime;
					traceLastThread = threadId;
				} else if (traceLastTime2 < threadLastTime) {
					traceLastTime2 = threadLastTime;
					traceLastThread2 = threadId;
				}
			} else {
				threadLastPoints.put(threadId, null);	
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * Traverse backward all statements from a given execution point in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @param before an execution point 
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, TracePoint before) {
		if (before == null) {
			return traverseStatementsInTraceBackward(visitor);
		}
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		Statement st = before.getStatement();
		if (st == null) {
			st = before.getMethodExecution().getCallerTracePoint().getStatement();
		}
		ThreadInstance thread = threads.get(st.getThreadNo());
		for (String threadId: threads.keySet()) {
			ThreadInstance t = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)t.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			if (t == thread) {
				traceLastThread = threadId;
				threadLastPoints.put(threadId, before);
				for (int n = rootExecutions.size() - 1; n >= 0; n--) {
					MethodExecution root = rootExecutions.get(n);
					if (root.getEntryTime() > before.getStatement().getTimeStamp()) {
						rootExecutions.remove(n);
					} else {
						break;
					}
				}
				if (rootExecutions.size() > 0) {
					rootExecutions.remove(rootExecutions.size() - 1);
				}
			} else {
				TracePoint threadBeforeTp;
				do {
					MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
					threadBeforeTp = threadLastExecution.getExitPoint();
				} while (!threadBeforeTp.isValid() && rootExecutions.size() > 0);
				if (threadBeforeTp.isValid()) {
					TracePoint[] tp = new TracePoint[] {threadBeforeTp};
					long threadLastTime;
					if (before.getStatement() instanceof MethodInvocation) {
						threadLastTime = ((MethodInvocation) before.getStatement()).getCalledMethodExecution().getExitTime();
					} else {
						threadLastTime = before.getStatement().getTimeStamp();
					}
					getLastStatementInThread(rootExecutions, tp, threadLastTime, new IStatementVisitor() {
						@Override
						public boolean preVisitStatement(Statement statement) { return false; }
						@Override
						public boolean postVisitStatement(Statement statement) { return false; }
					});
					threadLastPoints.put(threadId, tp[0]);
					if (tp[0] != null) {
						if (tp[0].getStatement() instanceof MethodInvocation) {
							threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// Å¶Exception Ç™î≠ê∂ÇµÇΩèÍçáÇÕçlÇ¶Ç»Ç≠ÇƒÇÊÇ¢?
						} else {
							threadLastTime = tp[0].getStatement().getTimeStamp();
						}
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}					
				} else {
					threadLastPoints.put(threadId, null);					
				}
			}
		}
		return traverseStatementsInTraceBackwardSub(visitor, threadRoots, threadLastPoints, traceLastThread, traceLastThread2, traceLastTime2);
	}
	
	/**
	 * Traverse backward all statements from a given execution point in this trace with synchronizing all threads.
	 * @param visitor a statement visitor
	 * @param before start time 
	 * @return the execution point where the traverse is aborted
	 */
	public TracePoint traverseStatementsInTraceBackward(IStatementVisitor visitor, long before) {
		if (before <= 0L) {
			return traverseStatementsInTraceBackward(visitor);
		}
		HashMap<String, ArrayList<MethodExecution>> threadRoots = new HashMap<String, ArrayList<MethodExecution>>();
		HashMap<String, TracePoint> threadLastPoints = new HashMap<String, TracePoint>();
		long traceLastTime = 0;
		String traceLastThread = null;
		long traceLastTime2 = 0;
		String traceLastThread2 = null;
		for (String threadId: threads.keySet()) {
			ThreadInstance t = threads.get(threadId);
			ArrayList<MethodExecution> rootExecutions = (ArrayList<MethodExecution>)t.getRoot().clone();
			threadRoots.put(threadId, rootExecutions);
			TracePoint threadBeforeTp;
			do {
				MethodExecution threadLastExecution = rootExecutions.remove(rootExecutions.size() - 1);
				threadBeforeTp = threadLastExecution.getExitPoint();
			} while (!threadBeforeTp.isValid() && rootExecutions.size() > 0);
			if (threadBeforeTp.isValid()) {
				TracePoint[] tp = new TracePoint[] {threadBeforeTp};
				getLastStatementInThread(rootExecutions, tp, before, new IStatementVisitor() {
					@Override
					public boolean preVisitStatement(Statement statement) { return false; }
					@Override
					public boolean postVisitStatement(Statement statement) { return false; }
				});
				threadLastPoints.put(threadId, tp[0]);
				if (tp[0] != null) {
					long threadLastTime;
					if (tp[0].getStatement() instanceof MethodInvocation) {
						threadLastTime = ((MethodInvocation) tp[0].getStatement()).getCalledMethodExecution().getExitTime();	// Å¶Exception Ç™î≠ê∂ÇµÇΩèÍçáÇÕçlÇ¶Ç»Ç≠ÇƒÇÊÇ¢?
					} else {
						threadLastTime = tp[0].getStatement().getTimeStamp();
					}
					if (traceLastTime < threadLastTime) {
						traceLastTime2 = traceLastTime;
						traceLastThread2 = traceLastThread;
						traceLastTime = threadLastTime;
						traceLastThread = threadId;
					} else if (traceLastTime2 < threadLastTime) {
						traceLastTime2 = threadLastTime;
						traceLastThread2 = threadId;
					}
				}					
			} else {
				threadLastPoints.put(threadId, null);					
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
			while (lastTp != null) {
				Statement statement = lastTp.getStatement();
				if (visitor.preVisitStatement(statement)) return lastTp;
				if (statement instanceof MethodInvocation) {
					lastTp.stepBackNoReturn();
					if (lastTp.isValid()) {
						MethodExecution methodExecution = ((MethodInvocation) statement).getCalledMethodExecution();
						if (!methodExecution.isTerminated() && methodExecution.getExitTime() < traceLastTime2) {
							break;
						}
						continue;
					}
				} else {
					if (visitor.postVisitStatement(statement)) return lastTp;					
				}
				if (lastTp.isValid() && lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
				while (!lastTp.stepBackOver() && lastTp.isValid()) {
					statement = lastTp.getStatement();
					if (visitor.postVisitStatement(statement)) return lastTp;
				}
				if (!lastTp.isValid()) {
					ArrayList<MethodExecution> roots = threadRoots.get(traceLastThread);
					while (!lastTp.isValid() && roots.size() > 0) {
						MethodExecution lastExecution = roots.remove(roots.size() - 1);
						lastTp = lastExecution.getExitPoint();							
					}
					if (lastTp.isValid()) {
						threadLastPoints.put(traceLastThread, lastTp);							
						if (lastTp.getStatement().getTimeStamp() < traceLastTime2) break;				
					} else {
						threadLastPoints.put(traceLastThread, null);
						break;
					}
				}
			}
			traceLastThread = traceLastThread2;
			traceLastTime2 = 0;
			traceLastThread2 = null;
			boolean continueTraverse = false;
			for (String threadId: threadLastPoints.keySet()) {
				if (!threadId.equals(traceLastThread)) {
					TracePoint threadLastTp = threadLastPoints.get(threadId);
					if (threadLastTp != null) {
						continueTraverse = true;
						long threadLastTime;
						if (threadLastTp.getStatement() instanceof MethodInvocation) {
							threadLastTime = ((MethodInvocation) threadLastTp.getStatement()).getCalledMethodExecution().getExitTime();
						} else {
							threadLastTime = threadLastTp.getStatement().getTimeStamp();
						}				
						if (traceLastTime2 < threadLastTime) {
							traceLastTime2 = threadLastTime;
							traceLastThread2 = threadId;
						}
					}
				}
			}
			if (traceLastThread == null && traceLastThread2 == null) break;
			if (!continueTraverse && threadLastPoints.get(traceLastThread) == null) break;
		}
		return null;
	}
	
	public TracePoint getFieldUpdateTracePoint(final String containerObjId, final String fieldName, final TracePoint tp, final boolean isReturned) {
		TracePoint before = tp.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
			boolean arrivedBeforeStatement = false;
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (!isReturned && !arrivedBeforeStatement) return false;
				if (statement instanceof FieldUpdate) {
					FieldUpdate fu = (FieldUpdate)statement;					
					if (fu.getContainerObjId().equals(containerObjId)
							&& fu.getFieldName().equals(fieldName)) {
						return true;
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) {
				if (statement.equals(tp.getStatement())) arrivedBeforeStatement = true;
				return false;
			}
		}, before);
		if (before != null) {
//			return (FieldUpdate)before.getStatement();
			return before;
		}
		return null;
	}
	
	/**
	 * Get the last update of a specified field of a specified container before a specified execution point.
	 * @param containerObjId  a container object
	 * @param fieldName       a field name of the object
	 * @param before          an execution point
	 * @return the corresponding execution point in this trace
	 */
	public FieldUpdate getRecentlyFieldUpdate(final String containerObjId, final String fieldName, TracePoint before) {
		before = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate fu = (FieldUpdate)statement;					
					if (fu.getContainerObjId().equals(containerObjId)
							&& fu.getFieldName().equals(fieldName)) {
						return true;
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return (FieldUpdate)before.getStatement();
		}
		return null;
	}
	
	/**
	 * Get the last update of a specified element of a specified array object before a specified execution point.
	 * @param arrayObjId an array object
	 * @param index      an index of an element
	 * @param before     an execution point
	 * @return the corresponding execution point in this trace
	 */
	public ArrayUpdate getRecentlyArrayUpdate(final String arrayObjId, final int index, TracePoint before) {		
		before = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof ArrayUpdate) {
					ArrayUpdate au = (ArrayUpdate)statement;
					if (au.getArrayObjectId().equals(arrayObjId)
							&& au.getIndex() == index) {
						return true;
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return (ArrayUpdate)before.getStatement();
		}
		return null;
	}
}