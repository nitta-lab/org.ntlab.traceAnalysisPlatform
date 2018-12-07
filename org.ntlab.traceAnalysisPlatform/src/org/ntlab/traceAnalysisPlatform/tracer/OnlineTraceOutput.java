package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.ArrayList;
import java.util.Stack;

import org.ntlab.traceAnalysisPlatform.tracer.trace.ObjectReference;
import org.ntlab.traceAnalysisPlatform.tracer.trace.ThreadInstance;
import org.ntlab.traceAnalysisPlatform.tracer.trace.TraceJSON;

/**
 * A wrapper class to record an online trace
 *
 */
public class OnlineTraceOutput {
	private static ThreadInstance thread = null;

	public static synchronized void onlineTraceClassDefinition(String className, String classPath, String loaderPath) {
		// remove the head character '/' of the classPath and the loaderPath
		TraceJSON.initializeClass(className, classPath.substring(1), loaderPath.substring(1));
	}

	public static synchronized void onlineTracePreCallMethod(String signature, String threadId, String lineNum) {
		TraceJSON.getThreadInstance(threadId).preCallMethod(signature, Integer.parseInt(lineNum));
	}

	public static synchronized void onlineTraceMethodEntry(String signature, String thisClassName, String thisObjectId,
			String threadId, long timeStamp, String argList) {
		boolean isConstractor = false;
		boolean isStatic = false;
		if (signature.contains("static ")) {
			isStatic = true;
		}
		thread = TraceJSON.getThreadInstance(threadId);
		Stack<String> stack;
		if (thread == null) {
			thread = new ThreadInstance(threadId);
			TraceJSON.getThreads().put(threadId, thread);
			stack = new Stack<String>();
			TraceJSON.getStacks().put(threadId, stack);
		} else {
			stack = TraceJSON.getStacks().get(threadId);
		}
		stack.push(signature);
		// Specify a method call
		thread.callMethod(signature, null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
		// Specify its arguments
		ArrayList<ObjectReference> arguments = new ArrayList<>();
		String[] args = argList.split(",");
		for (int i = 0; i < args.length - 1; i += 2) {
			arguments.add(new ObjectReference(args[i + 1], args[i]));
		}
		thread.setArgments(arguments);
	}

	public static synchronized void onlineTraceConstructorEntry(String signature, String thisClassName, String thisObjectId,
			String threadId, long timeStamp, String argList) {
		boolean isConstractor = true;
		boolean isStatic = false;
		thread = TraceJSON.getThreadInstance(threadId);
		Stack<String> stack;
		if (thread == null) {
			thread = new ThreadInstance(threadId);
			TraceJSON.getThreads().put(threadId, thread);
			stack = new Stack<String>();
			TraceJSON.getStacks().put(threadId, stack);
		} else {
			stack = TraceJSON.getStacks().get(threadId);
		}
		stack.push(signature);
		// Specify a method call
		thread.callMethod(signature, null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
		// Specify its arguments
		ArrayList<ObjectReference> arguments = new ArrayList<>();
		String[] args = argList.split(",");
		for (int i = 0; i < args.length - 1; i += 2) {
			arguments.add(new ObjectReference(args[i + 1], args[i]));
		}
		thread.setArgments(arguments);
	}

	public static synchronized void onlineTraceMethodExit(String shortSignature, String thisClassName, String thisObjectId,
			String returnClassName, String returnObjectId, String threadId, long timeStamp) {
		Stack<String> stack = TraceJSON.getStacks().get(threadId);
		if (!stack.isEmpty()) {
			String line2 = stack.peek();
			if (line2.endsWith(shortSignature)) {
				stack.pop();
			} else {
				do {
					stack.pop();
					thread.terminateMethod();
					if (stack.isEmpty()) break;	// add this statement tentatively
					line2 = stack.peek();
				} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
				if (!stack.isEmpty()) stack.pop();
			}
			thread = TraceJSON.getThreadInstance(threadId);
			ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);
			boolean isCollectionType = false;
			if (thisClassName.contains("java.util.List") || thisClassName.contains("java.util.Vector")
					|| thisClassName.contains("java.util.Iterator") || thisClassName.contains("java.util.ListIterator")
					|| thisClassName.contains("java.util.ArrayList") || thisClassName.contains("java.util.Stack")
					|| thisClassName.contains("java.util.Hash") || thisClassName.contains("java.util.Map")
					|| thisClassName.contains("java.util.Set") || thisClassName.contains("java.util.Linked")
					|| thisClassName.contains("java.lang.Thread")) {
				isCollectionType = true;
			}
			// Specify the return from a method
			thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
		}
	}

	public static synchronized void onlineTraceConstructorExit(String shortSignature, String returnClassName, String returnObjectId,
			String threadId, long timeStamp) {
		String thisClassName = returnClassName;
		String thisObjectId = returnObjectId;
		Stack<String> stack = TraceJSON.getStacks().get(threadId);
		if (!stack.isEmpty()) {
			String line2 = stack.peek();
			if (line2.endsWith(shortSignature)) {
				stack.pop();
			} else {
				do {
					stack.pop();
					thread.terminateMethod();
					if (stack.isEmpty()) break; // add this statement tentatively (as is the case with MethodExit)
					line2 = stack.peek();
				} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
				if (!stack.isEmpty()) stack.pop();
			}
			thread = TraceJSON.getThreadInstance(threadId);
			ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);
			boolean isCollectionType = false;
			if (thisClassName.contains("java.util.List") || thisClassName.contains("java.util.Vector")
					|| thisClassName.contains("java.util.Iterator") || thisClassName.contains("java.util.ListIterator")
					|| thisClassName.contains("java.util.ArrayList") || thisClassName.contains("java.util.Stack")
					|| thisClassName.contains("java.util.Hash") || thisClassName.contains("java.util.Map")
					|| thisClassName.contains("java.util.Set") || thisClassName.contains("java.util.Linked")
					|| thisClassName.contains("java.lang.Thread")) {
				isCollectionType = true;
			}
			// Specify the return from a method
			thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
		}
	}

	public static synchronized void onlineTraceFieldGet(String fieldName, String thisClassName, String thisObjectId,
			String containerClassName, String containerObjectId, String valueClassName, String valueObjectId,
			String threadId, String lineNum, long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.fieldAccess(fieldName, valueClassName, valueObjectId, containerClassName, containerObjectId,
					thisClassName, thisObjectId, Integer.parseInt(lineNum), timeStamp);
		}
	}

	public static synchronized void onlineTraceFieldSet(String fieldName, String containerClassName, String containerObjectId,
			String valueClassName, String valueObjectId, String threadId, String lineNum, long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.fieldUpdate(fieldName, valueClassName, valueObjectId, containerClassName, containerObjectId,
					Integer.parseInt(lineNum), timeStamp);
		}
	}

	public static synchronized void onlineTraceArrayCreate(String arrayClassName, String arrayObjectId, String dimension,
			String threadId, String lineNum, long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.arrayCreate(arrayClassName, arrayObjectId, Integer.parseInt(dimension), Integer.parseInt(lineNum),
					timeStamp);
		}
	}

	public static synchronized void onlineTraceArraySet(String arrayClassName, String arrayObjectId, int index,
			String valueClassName, String valueObjectId, String threadId, long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
		}
	}

	public static synchronized void onlineTraceArrayGet(String arrayClassName, String arrayObjectId, int index,
			String valueClassName, String valueObjectId, String threadId, long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
		}
	}

	public static synchronized void onlineTraceBlockEntry(String blockId, String incomings, String threadId, String lineNum,
			long timeStamp) {
		thread = TraceJSON.getThreadInstance(threadId);
		if (thread != null) {
			thread.blockEnter(Integer.parseInt(blockId), Integer.parseInt(incomings), Integer.parseInt(lineNum),
					timeStamp);
		}
	}
}
