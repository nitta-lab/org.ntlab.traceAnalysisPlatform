package org.ntlab.traceAnalysisPlatform.tracer;

import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * �]���̃g���[�X�o�͗p�̎��s��������
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
					Tracer.TRACER + "MyPrintStream.println(\"set:\" + " + containerClass + " + \":\" + " + containerObject + " + \":\" + " + 
													valueClass + " + \":\" + " + valueObject + " + " + LINE_AND_THREAD + threadId + ");";
	}

	@Override
	public String generateReplaceStatementsForFieldGet(
			String fieldName, String thisClass, String thisObject, String containerClass, String containerObject, 
			String valueClass, String valueObject, String threadId, String lineNum, String timeStamp) {
		return "$_ = $proceed(); " + 
					Tracer.TRACER + "MyPrintStream.println(\"get:\" + " + thisClass + " + \":\" + " + thisObject + " + \":\" + " + 
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
		// �����̏o��
		String delimiter = Tracer.TRACER + "MyPrintStream.println(\"Args:\" + ";
		for (int p = 0; p < argClasses.size(); p++) {
			argsOutput += delimiter + argClasses.get(p) + " + \":\" + " + argObjects.get(p);
			delimiter = " + \":\" + ";
		}
		if (argClasses.size() > 0) {
			argsOutput += " + " + LINE_AND_THREAD + threadId + ");";
		}
		if (m instanceof CtConstructor) {
			// �R���X�g���N�^�̏ꍇ
			newOutput = Tracer.TRACER + "MyPrintStream.println(\"New \" + " + thisClass + " + \":\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";
		}
		methodOutput = Tracer.TRACER + "MyPrintStream.println(\"Method \" + " + thisClass + " + \",\" + " + methodSignature
				+ " + \":\" + " + thisObject + " + " + LINE + " + " + timeStamp + " + \":ThreadNo \" + " + threadId + ");";
		classOutput = Tracer.TRACER + "MyPrintStream.println(\"Class \" + " + thisClass + " + \":\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";
		
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
			// �R���X�g���N�^�̏ꍇ
			shortName = m.getName().replace('$', '.') + "()";	// AspectJ�ł̓��\�b�h�V�O�j�`�����ł͖����N���X�̓h�b�g�ŋ�؂���
			invocationType = "initialization";
		} else {
			// �ʏ�̃��\�b�h��������static���\�b�h�̏ꍇ
			shortName = cls.getSimpleName().replace('$', '.') + "." + m.getName() + "()";	// AspectJ�ł̓��\�b�h�V�O�j�`�����ł͖����N���X�̓h�b�g�ŋ�؂���
			if (!isCallerSideInstrumentation) {
				// �Ăяo����ɖ��ߍ��ޏꍇ(�ʏ�)
				invocationType = "execution";
			} else {
				// �ďo�����ɖ��ߍ��ޏꍇ(�W���N���X�̌ďo��)
				invocationType = "call";
			}
		}
		
		String returnOutput =  Tracer.TRACER + "MyPrintStream.print(\"Return " + invocationType + "(" + shortName + "):\" + " + returnedClass + " + \":\" + " + returnedObject + " + \":\");" + 
				Tracer.TRACER + "MyPrintStream.println(\"\" + " + thisObject + " + " + LINE_AND_THREAD + threadId + ");";

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
}
