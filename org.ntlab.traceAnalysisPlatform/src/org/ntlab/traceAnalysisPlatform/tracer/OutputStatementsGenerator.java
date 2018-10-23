package org.ntlab.traceAnalysisPlatform.tracer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.expr.FieldAccess;
import javassist.expr.NewArray;

/**
 * �g���[�X�o�͂��s�����s���𐶐�����N���X�i�t�H�[�}�b�g�ˑ�������ITraceGenerator�Ɉڏ��j
 * 
 * @author Nitta
 *
 */
public class OutputStatementsGenerator {
	private ITraceGenerator generator;
	
	public OutputStatementsGenerator(ITraceGenerator generator) {
		this.generator = generator;
	}
	
	public ITraceGenerator getGenerator() {
		return generator;
	}

	public String generateReplaceStatementsForFieldSet(CtClass cc, FieldAccess f, int line) throws NotFoundException {
		String fieldName = "\"" + f.getClassName() + "." + f.getFieldName() + "\"";
		String containerClass = "(($0 != null)?$0.getClass().getName():\"" + cc.getName() + "\")";
		String containerObject = "(($0 != null)?System.identityHashCode($0):0)";
		String valueClass;
		String valueObject;
		if (!f.getField().getType().isPrimitive()) {
			valueClass = "(($1 != null)?$1.getClass().getName():\"---\")";
			valueObject = "(($1 != null)?System.identityHashCode($1):0)";
		} else {
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// ��{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$1";
			} else {
				valueObject = "Character.getNumericValue($1)";					// �����^�̏ꍇ�������o�͂���Ă��܂�����				
			}
		}
		String threadId = "Thread.currentThread().getId()";
		String lineNum = "\"" + line + "\"";
		String timeStamp = "System.nanoTime()";
		return generator.generateReplaceStatementsForFieldSet(fieldName, containerClass, containerObject, valueClass, valueObject, threadId, lineNum, timeStamp);
	}

	public String generateReplaceStatementsForFieldGet(CtClass cc, CtBehavior m, FieldAccess f, int line) throws NotFoundException {
		String fieldName = "\"" + f.getClassName() + "." + f.getFieldName() + "\"";
		String thisClass;
		String thisObject;
		if ((m.getModifiers() & Modifier.STATIC) == 0 && m instanceof CtMethod) {
			thisClass = "this.getClass().getName()";
			thisObject = "System.identityHashCode(this)";
		} else {
			// static���\�b�h���R���X�g���N�^�̏ꍇ
			thisClass = "\"" + cc.getName() + "\"";
			thisObject = "\"0\"";
		}
		String containerClass = "(($0 != null)?$0.getClass().getName():\"---\")";
		String containerObject = "(($0 != null)?System.identityHashCode($0):0)";
		String valueClass;
		String valueObject;
		if (!f.getField().getType().isPrimitive()) {
			valueClass = "(($_ != null)?$_.getClass().getName():\"---\")";
			valueObject = "(($_ != null)?System.identityHashCode($_):0)";
		} else {
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// ��{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$_";
			} else {
				valueObject = "Character.getNumericValue($_)";					// �����^�̏ꍇ�������o�͂���Ă��܂�����				
			}
		}
		String threadId = "Thread.currentThread().getId()";
		String lineNum = "\"" + line + "\"";
		String timeStamp = "System.nanoTime()";
		return generator.generateReplaceStatementsForFieldGet(fieldName, thisClass, thisObject, containerClass, containerObject, valueClass, valueObject, threadId, lineNum, timeStamp);
	}


	public String generateReplaceStatementsForNewArray(NewArray a, int line) {
		String arrayClass = "(($_ != null)?$_.getClass().getName():\"---\")";
		String arrayObject = "(($_ != null)?System.identityHashCode($_):0)";
		String dimension = "\"" + Integer.toString(a.getDimension()) + "\"";
		String threadId = "Thread.currentThread().getId()";
		String lineNum = "\"" + line + "\"";
		String timeStamp = "System.nanoTime()";
		return generator.generateReplaceStatementsForNewArray(arrayClass, arrayObject, dimension, threadId, lineNum, timeStamp);
	}
	
	public String generateReplaceStatementsForCall(CtClass cls, CtBehavior m, int line, boolean canManipulateCalledMethod) throws NotFoundException {
		if (canManipulateCalledMethod) {
			// �Ăяo����ɖ��ߍ��ނ��Ƃ��ł���ꍇ
			return generateInsertStatementsForCall(cls, m, line) + " $_ = $proceed($$);";
		} else {
			// �Ăяo����ɖ��ߍ��ނ��Ƃ��ł��Ȃ��ꍇ
			return generateInsertStatementsForCall(cls, m, line) + generateInsertBeforeStatements(cls, m, true) + " $_ = $proceed($$); " + generateInsertAfterStatements(cls, m, true);
		}
	}

	public String generateInsertBeforeStatementsForMethodBody(CtClass cls, CtBehavior m) throws NotFoundException {
		return "{" + generateInsertBeforeStatements(cls, m, false) + "}";
	}
	
	public String generateInsertAfterStatementsForMethodBody(CtClass cls, CtBehavior m) throws NotFoundException {
		return "{" + generateInsertAfterStatements(cls, m, false) + "}";
	}
	
	/**
	 * �g���[�X�o�͗p�̖��ߗ�𐶐�����
	 * @param cls �ΏۃN���X
	 * @param m �Ώۃ��\�b�h(�R���X�g���N�^)
	 * @param isCallerSideInstrumentation ���ߗ���Ăяo�����ɑ}�����邩�Ăяo����鑤�ɑ}�����邩?
	 * @return
	 * @throws NotFoundException
	 */
	private String generateInsertBeforeStatements(CtClass cls, CtBehavior m, boolean isCallerSideInstrumentation) throws NotFoundException {
		// ���\�b�h�V�O�j�`���̍\��
		String declaredClassName = cls.getName();
		String modifiers = "";
		if ((m.getModifiers() & Modifier.PUBLIC) != 0) {
			modifiers = "public ";
		} else if ((m.getModifiers() & Modifier.PRIVATE) != 0) {
			modifiers = "private ";			
		} else if ((m.getModifiers() & Modifier.PROTECTED) != 0) {
			modifiers = "protected ";
		}
		if ((m.getModifiers() & Modifier.STATIC) != 0) {
			modifiers += "static ";
		}
		if ((m.getModifiers() & Modifier.FINAL) != 0) {
			modifiers += "final ";
		}
		if ((m.getModifiers() & Modifier.SYNCHRONIZED) != 0) {
			modifiers += "synchronized ";
		}
		String thisClass;
		String thisObject;
		String methodSignature = null;
		if ((m.getModifiers() & Modifier.STATIC) != 0 && m instanceof CtMethod) {
			// static���\�b�h�̏ꍇ
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	// AspectJ�ł̓��\�b�h�V�O�j�`�����ł͖����N���X�̓h�b�g�ŋ�؂���
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else if (m instanceof CtConstructor) {
			// �R���X�g���N�^�̏ꍇ(�N���X�������q�̏ꍇ������)
			methodSignature = "\"" + modifiers + m.getLongName().replace('$', '.') + "\"";	// AspectJ�ł̓��\�b�h�V�O�j�`�����ł͖����N���X�̓h�b�g�ŋ�؂���
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else {
			// �ʏ탁�\�b�h�̏ꍇ
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	// AspectJ�ł̓��\�b�h�V�O�j�`�����ł͖����N���X�̓h�b�g�ŋ�؂���
			if (!isCallerSideInstrumentation) {
				// �Ăяo����ɖ��ߍ��ޏꍇ(�ʏ�)
				thisClass = "this.getClass().getName()";
				thisObject = "System.identityHashCode(this)";
			} else {
				// �ďo�����ɖ��ߍ��ޏꍇ(�W���N���X�̌ďo��)				
				thisClass = "$0.getClass().getName()";
				thisObject = "System.identityHashCode($0)";
			}
		}
		// �����̏o�͎��̍\��
		int p = 0;
		CtClass parameterClasses[] = m.getParameterTypes();
		ArrayList<String> argClasses = new ArrayList<>();
		ArrayList<String> argObjects = new ArrayList<>();
		for (CtClass c : parameterClasses) {
			if (!c.isPrimitive()) {
				argClasses.add("(($" + (p + 1) + " != null)?($" + (p + 1) + ").getClass().getName():\"" + c.getName() + "\")");
				argObjects.add("(($" + (p + 1) + " != null)?System.identityHashCode($" + (p + 1) + "):0)");
			} else {
				argClasses.add("\"" + c.getName() + "\"");								// ��{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
				if (c != CtClass.charType) {
					argObjects.add("$" + (p + 1));
				} else {
					argObjects.add("Character.getNumericValue($" + (p + 1) + ")");		// �����^�̏ꍇ�������o�͂���Ă��܂�����				
				}
			}
			p++;
		}

		String threadId = "Thread.currentThread().getId()";
		String timeStamp = "System.nanoTime()";
		return generator.generateInsertBeforeStatements(m, methodSignature, thisClass, thisObject, argClasses, argObjects, threadId, timeStamp);
	}

	private String generateInsertAfterStatements(CtClass cls, CtBehavior m, boolean isCallerSideInstrumentation) throws NotFoundException {
		String declaredClassName = cls.getName();
		String returnedClass;
		String returnedObject;
		String thisClass;
		String thisObject;
		if ((m.getModifiers() & Modifier.STATIC) != 0 && m instanceof CtMethod) {
			// static���\�b�h�̏ꍇ
			if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
				returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
				returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			} else {
				returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";	// void �Ɗ�{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
				if (((CtMethod)m).getReturnType() != CtClass.charType) {
					returnedObject = "$_";
				} else {
					returnedObject = "Character.getNumericValue($_)";					// �����^�̏ꍇ�������o�͂���Ă��܂�����				
				}
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			}
		} else if (m instanceof CtConstructor) {
			// �R���X�g���N�^�̏ꍇ(�N���X�������q�̏ꍇ������)
			if (!isCallerSideInstrumentation) {
				// �Ăяo����ɖ��ߍ��ޏꍇ
				if ((m.getModifiers() & Modifier.STATIC) == 0) {
					// �ʏ�̃R���X�g���N�^�̏ꍇ
					returnedClass = "$0.getClass().getName()";
					returnedObject = "System.identityHashCode($0)";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "System.identityHashCode($0)";
				} else {
					// �N���X�������q�̏ꍇ
					returnedClass = "\"void\"";
					returnedObject = "\"0\"";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "\"0\"";
				}
			} else {
				// �ďo�����ɖ��ߍ��ޏꍇ(�W���N���X�������̓f�t�H���g�R���X�g���N�^�̌ďo���A�܂��͐e�R���X�g���N�^�̌ďo��)
				returnedClass = "(($_ != null)?$_.getClass().getName():$0.getClass().getName())";
				returnedObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
			}
		} else {
			// �ʏ�̃��\�b�h�̏ꍇ
			if (!isCallerSideInstrumentation) {
				// �Ăяo����ɖ��ߍ��ޏꍇ(�ʏ�)
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		// void �Ɗ�{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// �����^�̏ꍇ�������o�͂���Ă��܂�����				
					}
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				}
			} else {
				// �ďo�����ɖ��ߍ��ޏꍇ(�W���N���X�̌ďo��)
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "$0.getClass().getName()"; 
					thisObject = "System.identityHashCode($0)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		// void �Ɗ�{�^�̏ꍇ�AgetClass()�ł��Ȃ�����
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// �����^�̏ꍇ�������o�͂���Ă��܂�����				
					}
					thisClass = "$0.getClass().getName()"; 
					thisObject = "System.identityHashCode($0)";
				}
			}
		}
		String threadId = "Thread.currentThread().getId()";
		String timeStamp = "System.nanoTime()";
		return generator.generateInsertAfterStatements(cls, m, thisClass, thisObject, returnedClass, returnedObject, threadId, timeStamp, isCallerSideInstrumentation);
	}
	
	private String generateInsertStatementsForCall(CtClass cls, CtBehavior m, int line) {
		String lineNum = "\"" + line + "\"";
		String threadId = "Thread.currentThread().getId()";
		return generator.generateInsertStatementsForCall(m, lineNum, threadId);
	}

	public String generateInsertStatementsForBlockEntry(CtMethod m, int id, Block block, int line) {
		String blockId = "\"" + id + "\"";
//		String blockPos = "\"" + block.position() + "\"";
//		String blockLen = "\"" + block.length() + "\"";
		String incomings = "\"" + block.incomings() + "\"";
		String threadId = "Thread.currentThread().getId()";
		String lineNum = "\"" + line + "\"";
		String timeStamp = "System.nanoTime()";
		return generator.generateInsertStatementsForBlockEntry(m, blockId, incomings, threadId, lineNum, timeStamp);		
	}

	public String generateInsertBeforeStatementsForClassDefinition(CtClass cc, CtConstructor classInitializer) throws NotFoundException {
		try {
			String className = "\"" + cc.getName() + "\"";
			String classPath = "\"" + URLDecoder.decode(cc.getURL().getPath(), "UTF-8") + "\"";		// �p�X�� URL encode �ɂȂ��Ă��邽��
			String loaderPath = "\"" + URLDecoder.decode(cc.getClassPool().getClassLoader().getResource("").getPath(), "UTF-8") + "\"";		// �p�X�� URL encode �ɂȂ��Ă��邽��
			return generator.generateInsertBeforeStatementsForClassDefinition(className, classPath, loaderPath);
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
}
