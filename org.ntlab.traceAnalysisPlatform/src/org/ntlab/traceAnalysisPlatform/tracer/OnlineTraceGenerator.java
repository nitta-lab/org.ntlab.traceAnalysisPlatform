package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.ArrayList;
import java.util.List;

import org.ntlab.traceAnalysisPlatform.tracer.trace.TraceJSON;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * オンライン解析用の実行文生成器
 * @author Isitani
 *
 */
public class OnlineTraceGenerator implements ITraceGenerator {

	@Override
	public String generateReplaceStatementsForFieldSet(
			String fieldName, String containerClass, String containerObject, 
			String valueClass, String valueObject,
			String threadId, String lineNum, String timeStamp) {
		// 埋め込み処理
		String proceed = "$proceed($$); ";
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceFieldSet(");
		embedded.append(fieldName + ", ");
		embedded.append(containerClass + ", String.valueOf(" + containerObject + "), ");
		embedded.append(valueClass + ", String.valueOf(" + valueObject + "), ");
		embedded.append("String.valueOf(" + threadId + "), " + lineNum + ", " + timeStamp + ");");					
		return proceed + embedded;
	}
		
	@Override
	public String generateReplaceStatementsForFieldGet(
			String fieldName, String thisClass, String thisObject, 
			String containerClass, String containerObject,
			String valueClass, String valueObject, 
			String threadId, String lineNum, String timeStamp) {
		// 埋め込み処理
		String proceed = "$_ = $proceed(); ";
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceFieldGet(");
		embedded.append(fieldName + ", ");
		embedded.append(thisClass + ", String.valueOf(" + thisObject + "), ");
		embedded.append(containerClass + ", String.valueOf(" + containerObject + "), ");
		embedded.append(valueClass + ", String.valueOf(" + valueObject + "), ");
		embedded.append("String.valueOf(" + threadId + "), " + lineNum + ", " + timeStamp + ");");				
		return proceed + embedded;
	}
		
	@Override
	public String generateReplaceStatementsForNewArray(
			String arrayClass, String arrayObject, String dimension, 
			String threadId, String lineNum, String timeStamp) {
		// 埋め込み処理
		String proceed = "$_ = $proceed($$); ";
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceArrayCreate(");
		embedded.append(arrayClass + ", String.valueOf(" + arrayObject + "), ");
		embedded.append(dimension + ", String.valueOf(" + threadId + "), ");
		embedded.append(lineNum + ", " + timeStamp + ");");
		return proceed + embedded;
	}
	
	@Override
	public String generateInsertBeforeStatements(
			CtBehavior m, String methodSignature, 
			String thisClass, String thisObject,
			List<String> argClasses, List<String> argObjects, 
			String threadId, String timeStamp) {		
		// 埋め込み処理で呼び出すメソッドに渡す引数のうち、引数データを文字列連結で作成する
		StringBuilder args = new StringBuilder();
		String delimiter = "";
		for (int i = 0; i < argClasses.size(); i++) {
			args.append(delimiter + argClasses.get(i));
			delimiter = " + \",\" + "; // 引数データを、 + "," + で区切る 
			args.append(delimiter + argObjects.get(i));
		}
		if (args.toString().isEmpty()) {
			args.append("\"\"");
		}

		// 埋め込み処理
		StringBuilder embedded = new StringBuilder();
		boolean isConstructor = (m instanceof CtConstructor);
		if (!isConstructor) {
			embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceMethodEntry(");
			embedded.append(methodSignature + ", ");
			embedded.append(thisClass + ", String.valueOf(" + thisObject + "), ");
			embedded.append("String.valueOf(" + threadId + "), " + timeStamp + ", ");
			embedded.append(args.toString() + ");");			
		} else {
			embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceConstructorEntry(");
			embedded.append(methodSignature + ", ");
			embedded.append(thisClass + ", String.valueOf(" + thisObject + "), ");
			embedded.append("String.valueOf(" + threadId + "), " + timeStamp + ", ");
			embedded.append(args.toString() + ");");
		}				
		return embedded.toString();
	}

	@Override
	public String generateInsertAfterStatements(CtClass cls, CtBehavior m,
			String thisClass, String thisObject, String returnedClass, String returnedObject,
			String threadId, String timeStamp, boolean isCallerSideInstrumentation) {
		// 埋め込み処理
		String shortSignature = "\"" + m.getLongName().replace('$', ',') + "\"";
		StringBuilder embedded = new StringBuilder();
		boolean isConstructor = (m instanceof CtConstructor);
		if (!isConstructor) {
			embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceMethodExit(");
			embedded.append(shortSignature + ", ");
			embedded.append(thisClass + ", String.valueOf(" + thisObject + "), ");
			embedded.append(returnedClass + ", " + "String.valueOf(" + returnedObject + "), ");
			embedded.append("String.valueOf(" + threadId + "), " + timeStamp + ");");			
		} else {
			embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceConstructorExit(");
			embedded.append(shortSignature + ", ");
			embedded.append(returnedClass + ", String.valueOf(" + returnedObject + "), ");
			embedded.append("String.valueOf(" + threadId + "), " + timeStamp + ");");
		}		
		return embedded.toString();
	}
	
	@Override
	public String generateInsertStatementsForCall(CtBehavior m, String lineNum, String threadId) {
		// 埋め込み処理
		String signature = "\"" + m.getLongName().replace('$', '.') + "\"";
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTracePreCallMethod(");
		embedded.append(signature + ", String.valueOf(" + threadId + "), " + lineNum + ");");		
		return embedded.toString();
	}
	
	@Override
	public String generateInsertStatementsForBlockEntry(
			CtMethod m, String blockId, String incomings,
			String threadId, String lineNum, String timeStamp) {
		// 埋め込み処理
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceBlockEntry(");
		embedded.append(blockId + ", " + incomings + ", ");
		embedded.append("String.valueOf(" + threadId + "), " + lineNum + ", " + timeStamp + ");");
		return embedded.toString();
	}
	
	@Override
	public String generateInsertBeforeStatementsForClassDefinition(String className, String classPath, String loaderPath) {
		// 埋め込み処理
		StringBuilder embedded = new StringBuilder();
		embedded.append(Tracer.TRACER + "OnlineTraceOutput.onlineTraceClassDefinition(");
		embedded.append(className + ", " + classPath + ", " + loaderPath + ");");
		return embedded.toString();
	}
	
	public static void arraySetOutput(String arrayType, String arrayId, int index, String valueType, String valueId, long threadId, long timeStamp) {
		// 埋め込み処理
		OnlineTraceOutput.onlineTraceArraySet(arrayType, arrayId, index,
				valueType, valueId, String.valueOf(threadId), timeStamp);
	}
		
	public static void arrayGetOutput(String arrayType, String arrayId, int index, String valueType, String valueId, long threadId, long timeStamp) {
		// 埋め込み処理
		OnlineTraceOutput.onlineTraceArrayGet(arrayType, arrayId, index,
				valueType, valueId, String.valueOf(threadId), timeStamp);
	}	
}
