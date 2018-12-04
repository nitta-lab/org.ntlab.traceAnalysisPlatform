package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * Plain text trace specific part of OutputStatementsGenerator (for backward compatibility)
 * @author Nitta
 *
 */
public class PlainTextTraceGenerator implements ITraceGenerator {
	public static final String LINE_AND_THREAD = "\":Line \" + (" + Tracer.TRACER + "Tracer.lineNo++) + \":ThreadNo \" + ";
	public static final String LINE = "\":Line \" + (" + Tracer.TRACER + "Tracer.lineNo++) + \":\"";

	@Override
	public String generateReplaceStatementsForFieldSet(
			String fieldName, String containerClass, String containerObject, 
			String valueClass, String valueObject, String threadId, String lineNum, String timeStamp) {
		return "$proceed($$); " + 
					Tracer.TRACER + "OfflineTraceOutput.println(\"set:\" + " + containerClass + " + \":\" + " + containerObject + " + \":\" + " + 
													valueClass + " + \":\" + " + valueObject + " + " + LINE_AND_THREAD + threadId + ");";
	}

	@Override
	public String generateReplaceStatementsForFieldGet(
			String fieldName, String thisClass, String thisObject, String containerClass, String containerObject, 
			String valueClass, String valueObject, String threadId, String lineNum, String timeStamp) {
		return "$_ = $proceed(); " + 
					Tracer.TRACER + "OfflineTraceOutput.println(\"get:\" + " + thisClass + " + \":\" + " + thisObject + " + \":\" + " + 
													containerClass + " + \":\" + " + containerObject + " + \":\" + " + 
													valueClass + " + \":\" + " + valueObject + " + " + LINE_AND_THREAD + threadId + ");";
	}
	
	@Override
	public String generateReplaceStatementsForNewArray(
			String arrayClass, String arrayObject, String dimension, 
			String threadId, String lineNum, String timeStamp) {
		return "$_ = $proceed($$);";
	}
	
	@Override
	public String generateInsertBeforeStatements(CtBehavior m, String methodSignature, 
			String thisClass, String thisObject, 
			List<String> argClasses, List<String> argObjects,
			String threadId, String timeStamp) {
		String newOutput = "";
		String methodOutput = "";
		String classOutput = "";
		String argsOutput = "";
		// For output arguments
		String delimiter = Tracer.TRACER + "OfflineTraceOutput.println(\"Args:\" + ";
		for (int p = 0; p < argClasses.size(); p++) {
			argsOutput += delimiter + argClasses.get(p) + " + \":\" + " + argObjects.get(p);
			delimiter = " + \":\" + ";
		}
		if (argClasses.size() > 0) {
			argsOutput += " + " + LINE_AND_THREAD + threadId + ");";
		}
		if (m instanceof CtConstructor) {
			// For a constructor
			newOutput = Tracer.TRACER + "OfflineTraceOutput.println(\"New \" + " + thisClass + " + \":\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";
		}
		methodOutput = Tracer.TRACER + "OfflineTraceOutput.println(\"Method \" + " + thisClass + " + \",\" + " + methodSignature
				+ " + \":\" + " + thisObject + " + " + LINE + " + " + timeStamp + " + \":ThreadNo \" + " + threadId + ");";
		classOutput = Tracer.TRACER + "OfflineTraceOutput.println(\"Class \" + " + thisClass + " + \":\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";
		
		return newOutput + methodOutput + classOutput + argsOutput;
	}	

	@Override
	public String generateInsertAfterStatements(CtClass cls, CtBehavior m, 
			String thisClass, String thisObject, 
			String returnedClass, String returnedObject, 
			String threadId, String timeStamp, boolean isCallerSideInstrumentation) {
		String shortName = null;
		String invocationType = null;
		if (m instanceof CtConstructor) {
			// For a constructor
			shortName = m.getName().replace('$', '.') + "()";	// The name of an anonymous class is delimited by '.' within a method signature by AspectJ.
			invocationType = "initialization";
		} else {
			// For a normal or static method
			shortName = cls.getSimpleName().replace('$', '.') + "." + m.getName() + "()";	// The name of an anonymous class is delimited by '.' within a method signature by AspectJ
			if (!isCallerSideInstrumentation) {
				// In the case of callee instrumentation (normal case)
				invocationType = "execution";
			} else {
				// In the case of caller instrumentation (invocation of a standard class method)
				invocationType = "call";
			}
		}
		
		String returnOutput =  Tracer.TRACER + "OfflineTraceOutput.print(\"Return " + invocationType + "(" + shortName + "):\" + " + returnedClass + " + \":\" + " + returnedObject + " + \":\");" + 
				Tracer.TRACER + "OfflineTraceOutput.println(\"\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";

		return returnOutput;
	}

	@Override
	public String generateInsertStatementsForCall(CtBehavior m, String lineNum, String threadId) {
		return "";
	}

	@Override
	public String generateInsertStatementsForBlockEntry(
			CtMethod m, String blockId, String incomings,
			String threadId, String lineNum, String timeStamp) {
		return "";
	}

	@Override
	public String generateInsertBeforeStatementsForClassDefinition(
			String className, String classPath, String loaderPath) {
		return "";
	}

	@Override
	public String getArrayAdvisorClassName() {
		return null;
	}
}
