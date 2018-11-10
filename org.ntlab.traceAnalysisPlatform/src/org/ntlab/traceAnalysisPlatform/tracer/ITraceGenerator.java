package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Output format dependent part of OutputStatementsGenerator
 * @author Nitta
 *
 */
public interface ITraceGenerator {
	public abstract String generateReplaceStatementsForFieldSet(
			String fieldName, String containerClass, String containerObject, 
			String valueClass, String valueObject, 
			String threadId, String lineNum, String timeStamp);

	public abstract String generateReplaceStatementsForFieldGet(
			String fieldName, String thisClass, String thisObject, 
			String containerClass, String containerObject,
			String valueClass, String valueObject, 
			String threadId, String lineNum, String timeStamp);

	public abstract String generateInsertBeforeStatements(
			CtBehavior m, String methodSignature,
			String thisClass, String thisObject, 
			List<String> argClasses, List<String> argObjects,
			String threadId, String timeStamp);

	public abstract String generateInsertAfterStatements(
			CtClass cls, CtBehavior m,
			String thisClass, String thisObject, 
			String returnedClass, String returnedObject,
			String threadId, String timeStamp,
			boolean isCallerSideInstrumentation);

	public abstract String generateInsertStatementsForCall(
			CtBehavior m, String lineNum, String threadId);

	public abstract String generateReplaceStatementsForNewArray(
			String arrayClass, String arrayObject, String dimension,
			String threadId, String lineNum, String timeStamp);

	public abstract String generateInsertStatementsForBlockEntry(
			CtMethod m, String blockId, String incomings, 
			String threadId, String lineNum, String timeStamp);

	public abstract String generateInsertBeforeStatementsForClassDefinition(
			String className, String classPath, String loaderPath);

	public abstract String getArrayAdvisorClassName();

}
