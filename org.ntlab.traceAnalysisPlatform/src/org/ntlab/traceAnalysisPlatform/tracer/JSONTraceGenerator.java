package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * JSONトレース出力用の実行文生成器
 * @author Nitta
 *
 */
public class JSONTraceGenerator implements ITraceGenerator {
	private static final String DQ_GEN = "\"\\\"\"";

	@Override
	public String generateReplaceStatementsForFieldSet(
			String fieldName, String containerClass, String containerObject, 
			String valueClass, String valueObject,
			String threadId, String lineNum, String timeStamp) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");		values.add(DQ_GEN + " + \"fieldSet\" + " + DQ_GEN);
		keys.add("fieldName");	values.add(DQ_GEN + " + " + fieldName + " + " + DQ_GEN);
		keys.add("container");	values.add(generateJSONObjectGenerator(containerClass, containerObject));
		keys.add("value");		values.add(generateJSONObjectGenerator(valueClass, valueObject));
		keys.add("threadId");	values.add(threadId);
		keys.add("lineNum");	values.add(lineNum);
		keys.add("time");		values.add(timeStamp);
		return "$proceed($$); " + 
				Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateReplaceStatementsForFieldGet(
			String fieldName, String thisClass, String thisObject, 
			String containerClass, String containerObject,
			String valueClass, String valueObject, 
			String threadId, String lineNum, String timeStamp) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");		values.add(DQ_GEN + " + \"fieldGet\" + " + DQ_GEN);
		keys.add("fieldName");	values.add(DQ_GEN + " + " + fieldName + " + " + DQ_GEN);
		keys.add("this");		values.add(generateJSONObjectGenerator(thisClass, thisObject));
		keys.add("container");	values.add(generateJSONObjectGenerator(containerClass, containerObject));
		keys.add("value");		values.add(generateJSONObjectGenerator(valueClass, valueObject));
		keys.add("threadId");	values.add(threadId);
		keys.add("lineNum");	values.add(lineNum);
		keys.add("time");		values.add(timeStamp);
		return "$_ = $proceed(); " + 
				Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}
	

	@Override
	public String generateReplaceStatementsForNewArray(
			String arrayClass, String arrayObject, String dimension, 
			String threadId, String lineNum, String timeStamp) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");		values.add(DQ_GEN + " + \"arrayCreate\" + " + DQ_GEN);
		keys.add("array");		values.add(generateJSONObjectGenerator(arrayClass, arrayObject));
		keys.add("dimension");	values.add(dimension);
		keys.add("threadId");	values.add(threadId);
		keys.add("lineNum");	values.add(lineNum);
		keys.add("time");		values.add(timeStamp);
		return "$_ = $proceed($$); " + 
				Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateInsertBeforeStatements(
			CtBehavior m, String methodSignature, 
			String thisClass, String thisObject,
			List<String> argClasses, List<String> argObjects, 
			String threadId, String timeStamp) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		if (!(m instanceof CtConstructor)) {
			keys.add("type");		values.add(DQ_GEN + " + \"methodEntry\" + " + DQ_GEN);
		} else {
			keys.add("type");		values.add(DQ_GEN + " + \"constructorEntry\" + " + DQ_GEN);
		}
		keys.add("signature");		values.add(DQ_GEN + " + " + methodSignature + " + " + DQ_GEN);
		if (!(m instanceof CtConstructor)) {
			keys.add("receiver");	values.add(generateJSONObjectGenerator(thisClass, thisObject));
		} else {
			keys.add("class");		values.add(DQ_GEN + " + " + thisClass + " + " + DQ_GEN);			
		}
		ArrayList<String> argList = new ArrayList<>();
		for (int i = 0; i < argClasses.size(); i++) {
			argList.add(generateJSONObjectGenerator(argClasses.get(i), argObjects.get(i)));
		}
		keys.add("args");			values.add(generateJSONArrayGenerator(argList));
		keys.add("threadId");		values.add(threadId);
		keys.add("time");			values.add(timeStamp);
		return Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateInsertAfterStatements(CtClass cls, CtBehavior m,
			String thisClass, String thisObject, String returnedClass, String returnedObject,
			String threadId, String timeStamp,
			boolean isCallerSideInstrumentation) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		if (!(m instanceof CtConstructor)) {
			keys.add("type");		values.add(DQ_GEN + " + \"methodExit\" + " + DQ_GEN);
		} else {
			keys.add("type");		values.add(DQ_GEN + " + \"constructorExit\" + " + DQ_GEN);			
		}
		keys.add("shortSignature");	values.add(DQ_GEN + " + \"" + m.getLongName().replace('$', '.') + "\" + " + DQ_GEN);		// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
		if (!(m instanceof CtConstructor)) {
			keys.add("receiver");	values.add(generateJSONObjectGenerator(thisClass, thisObject));
		}
		keys.add("returnValue");	values.add(generateJSONObjectGenerator(returnedClass, returnedObject));
		keys.add("threadId");		values.add(threadId);
		keys.add("time");			values.add(timeStamp);
		return Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateInsertStatementsForCall(CtBehavior m, String lineNum, String threadId) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");					values.add(DQ_GEN + " + \"methodCall\" + " + DQ_GEN);
		keys.add("callerSideSignature");	values.add(DQ_GEN + " + \"" + m.getLongName().replace('$', '.') + "\" + " + DQ_GEN);		// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
		keys.add("threadId");				values.add(threadId);
		keys.add("lineNum");				values.add(lineNum);
		return Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateInsertStatementsForBlockEntry(
			CtMethod m, String blockId, String incomings,
			String threadId, String lineNum, String timeStamp) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");				values.add(DQ_GEN + " + \"blockEntry\" + " + DQ_GEN);
		keys.add("methodSignature");	values.add(DQ_GEN + " + \"" + m.getLongName().replace('$', '.') + "\" + " + DQ_GEN);		// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
		keys.add("blockId");			values.add(blockId);
//		keys.add("blockPos");			values.add(blockPos);
//		keys.add("blockLen");			values.add(blockLen);
		keys.add("incomings");			values.add(incomings);
		keys.add("threadId");			values.add(threadId);
		keys.add("lineNum");			values.add(lineNum);
		keys.add("time");				values.add(timeStamp);
		return Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}

	@Override
	public String generateInsertBeforeStatementsForClassDefinition(String className, String classPath, String loaderPath) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("type");				values.add(DQ_GEN + " + \"classDef\" + " + DQ_GEN);
		keys.add("name");				values.add(DQ_GEN + " + " + className + " + " + DQ_GEN);
		keys.add("path");				values.add(DQ_GEN + " + " + classPath + " + " + DQ_GEN);
		keys.add("loaderPath");			values.add(DQ_GEN + " + " + loaderPath + " + " + DQ_GEN);
		return Tracer.TRACER + "MyPrintStream.println(" + generateJSONMapGenerator(keys, values) + " + \",\");";
	}
	
	private String generateJSONObjectGenerator(String className, String objectId) {
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		keys.add("class");	values.add(DQ_GEN + " + " + className + " + " + DQ_GEN);
		keys.add("id");		values.add(objectId);
		return generateJSONMapGenerator(keys, values);
	}

	private String generateJSONMapGenerator(ArrayList<String> keys, ArrayList<String> valueGenerators) {
		String mapJSON = "\"{\" + ";
		String delimiter = "";
		for (int i = 0; i < keys.size(); i++) {
			mapJSON += delimiter + "\"\\\"" + keys.get(i) + "\\\":\" + " + valueGenerators.get(i);			
			delimiter = " + \",\" + ";
		}
		mapJSON += " + \"}\"";
		return mapJSON;
	}
	private String generateJSONArrayGenerator(ArrayList<String> valueGenerators) {
		String arrayJSON = "\"[\"";
		String delimiter = " + ";
		for (int i = 0; i < valueGenerators.size(); i++) {
			arrayJSON += delimiter + valueGenerators.get(i);
			delimiter = " + \",\" + ";
		}
		arrayJSON += " + \"]\"";
		return arrayJSON;
	}

	public static void arraySetOutput(String arrayType, String arrayId, int index, String valueType, String valueId, long threadId, long timeStamp) {
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		keys.add("type");		values.add("\"arraySet\"");
		keys.add("array");		values.add(generateJSONObjectOutput(arrayType, arrayId));
		keys.add("index");		values.add(Integer.toString(index));
		keys.add("value");		values.add(generateJSONObjectOutput(valueType, valueId));
		keys.add("threadId");	values.add(Long.toString(threadId));
		keys.add("time");		values.add(Long.toString(timeStamp));
		MyPrintStream.println(generateJSONMapOutput(keys, values) + ",");
	}

	public static void arrayGetOutput(String arrayType, String arrayId, int index, String valueType, String valueId, long threadId, long timeStamp) {
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		keys.add("type");		values.add("\"arrayGet\"");
		keys.add("array");		values.add(generateJSONObjectOutput(arrayType, arrayId));
		keys.add("index");		values.add(Integer.toString(index));
		keys.add("value");		values.add(generateJSONObjectOutput(valueType, valueId));
		keys.add("threadId");	values.add(Long.toString(threadId));
		keys.add("time");		values.add(Long.toString(timeStamp));
		MyPrintStream.println(generateJSONMapOutput(keys, values) + ",");
	}
	
	private static String generateJSONObjectOutput(String className, String objectId) {
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		keys.add("class");	values.add("\"" + className + "\"");
		keys.add("id");		values.add(objectId);
		return generateJSONMapOutput(keys, values);
	}

	private static String generateJSONMapOutput(ArrayList<String> keys, ArrayList<String> valueGenerators) {
		String mapJSON = "{";
		String delimiter = "";
		for (int i = 0; i < keys.size(); i++) {
			mapJSON += delimiter + "\"" + keys.get(i) + "\":" + valueGenerators.get(i);			
			delimiter = ",";
		}
		mapJSON += "}";
		return mapJSON;
	}
}
