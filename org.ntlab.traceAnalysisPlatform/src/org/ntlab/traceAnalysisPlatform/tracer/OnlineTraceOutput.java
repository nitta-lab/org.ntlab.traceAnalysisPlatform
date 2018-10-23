package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.HashMap;

import org.ntlab.traceAnalysisPlatform.tracer.trace.ArrayUpdate;
import org.ntlab.traceAnalysisPlatform.tracer.trace.ClassInfo;
import org.ntlab.traceAnalysisPlatform.tracer.trace.FieldUpdate;
import org.ntlab.traceAnalysisPlatform.tracer.trace.MethodExecution;
import org.ntlab.traceAnalysisPlatform.tracer.trace.MethodInvocation;
import org.ntlab.traceAnalysisPlatform.tracer.trace.ThreadInstance;
import org.ntlab.traceAnalysisPlatform.tracer.trace.TraceJSON;
import org.ntlab.traceAnalysisPlatform.tracer.trace.TracePoint;

public class OnlineTraceOutput {

	public static void onlineTraceClassDefinition(String className,
			String classPath, String loaderPath) {
		TraceJSON.onlineTraceClassDefinition(className, classPath, loaderPath);
	}

	public static void onlineTracePreCallMethod(String signature,
			String threadId, String lineNum) {
		TraceJSON.onlineTracePreCallMethod(signature, threadId, lineNum);
	}
	
	public static void onlineTraceMethodEntry(String signature,
			String thisClassName, String thisObjectId, String threadId,
			long timeStamp, String argList) {
		TraceJSON.onlineTraceMethodEntry(signature, thisClassName,
				thisObjectId, threadId, timeStamp, argList);
	}

	public static void onlineTraceConstructorEntry(String signature,
			String thisClassName, String thisObjectId, String threadId,
			long timeStamp, String argList) {
		TraceJSON.onlineTraceConstructorEntry(signature, thisClassName,
				thisObjectId, threadId, timeStamp, argList);
	}

	public static void onlineTraceMethodExit(String shortSignature,
			String thisClassName, String thisObjectId, String returnClassName,
			String returnObjectId, String threadId, long timeStamp) {
		TraceJSON.onlineTraceMethodExit(shortSignature, thisClassName,
				thisObjectId, returnClassName, returnObjectId, threadId,
				timeStamp);
	}

	public static void onlineTraceConstructorExit(String shortSignature,
			String returnClassName, String returnObjectId, String threadId,
			long timeStamp) {
		TraceJSON.onlineTraceConstructorExit(shortSignature, returnClassName,
				returnObjectId, threadId, timeStamp);
	}

	public static void onlineTraceFieldGet(String fieldName,
			String thisClassName, String thisObjectId,
			String containerClassName, String containerObjectId,
			String valueClassName, String valueObjectId, String threadId,
			String lineNum, long timeStamp) {
		TraceJSON.onlineTraceFieldGet(fieldName, thisClassName, thisObjectId,
				containerClassName, containerObjectId, valueClassName,
				valueObjectId, threadId, lineNum, timeStamp);
	}

	public static void onlineTraceFieldSet(String fieldName,
			String containerClassName, String containerObjectId,
			String valueClassName, String valueObjectId, String threadId,
			String lineNum, long timeStamp) {
		TraceJSON.onlineTraceFieldSet(fieldName, containerClassName,
				containerObjectId, valueClassName, valueObjectId, threadId,
				lineNum, timeStamp);
	}

	public static void onlineTraceArrayCreate(String arrayClassName,
			String arrayObjectId, String dimension, String threadId,
			String lineNum, long timeStamp) {
		TraceJSON.onlineTraceArrayCreate(arrayClassName, arrayObjectId,
				dimension, threadId, lineNum, timeStamp);
	}

	public static void onlineTraceArraySet(String arrayClassName,
			String arrayObjectId, int index, String valueClassName,
			String valueObjectId, String threadId, long timeStamp) {
		TraceJSON.onlineTraceArraySet(arrayClassName, arrayObjectId, index,
				valueClassName, valueObjectId, threadId, timeStamp);
	}

	public static void onlineTraceArrayGet(String arrayClassName,
			String arrayObjectId, int index, String valueClassName,
			String valueObjectId, String threadId, long timeStamp) {
		TraceJSON.onlineTraceArrayGet(arrayClassName, arrayObjectId, index,
				valueClassName, valueObjectId, threadId, timeStamp);
	}

	public static void onlineTraceBlockEntry(String blockId, String incomings,
			String threadId, String lineNum, long timeStamp) {
		TraceJSON.onlineTraceBlockEntry(blockId, incomings, threadId, lineNum,
				timeStamp);
	}
}
