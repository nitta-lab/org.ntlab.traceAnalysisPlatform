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
 * トレース出力を行う実行文を生成するクラス（フォーマット依存部分はITraceGeneratorに移譲）
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
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// 基本型の場合、getClass()できないため
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$1";
			} else {
				valueObject = "Character.getNumericValue($1)";					// 文字型の場合文字が出力されてしまうため				
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
			// staticメソッドかコンストラクタの場合
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
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// 基本型の場合、getClass()できないため
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$_";
			} else {
				valueObject = "Character.getNumericValue($_)";					// 文字型の場合文字が出力されてしまうため				
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
			// 呼び出し先に埋め込むことができる場合
			return generateInsertStatementsForCall(cls, m, line) + " $_ = $proceed($$);";
		} else {
			// 呼び出し先に埋め込むことができない場合
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
	 * トレース出力用の命令列を生成する
	 * @param cls 対象クラス
	 * @param m 対象メソッド(コンストラクタ)
	 * @param isCallerSideInstrumentation 命令列を呼び出し側に挿入するか呼び出される側に挿入するか?
	 * @return
	 * @throws NotFoundException
	 */
	private String generateInsertBeforeStatements(CtClass cls, CtBehavior m, boolean isCallerSideInstrumentation) throws NotFoundException {
		// メソッドシグニチャの構成
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
			// staticメソッドの場合
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else if (m instanceof CtConstructor) {
			// コンストラクタの場合(クラス初期化子の場合もある)
			methodSignature = "\"" + modifiers + m.getLongName().replace('$', '.') + "\"";	// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else {
			// 通常メソッドの場合
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	// AspectJではメソッドシグニチャ内では無名クラスはドットで区切られる
			if (!isCallerSideInstrumentation) {
				// 呼び出し先に埋め込む場合(通常)
				thisClass = "this.getClass().getName()";
				thisObject = "System.identityHashCode(this)";
			} else {
				// 呼出し元に埋め込む場合(標準クラスの呼出し)				
				thisClass = "$0.getClass().getName()";
				thisObject = "System.identityHashCode($0)";
			}
		}
		// 引数の出力式の構成
		int p = 0;
		CtClass parameterClasses[] = m.getParameterTypes();
		ArrayList<String> argClasses = new ArrayList<>();
		ArrayList<String> argObjects = new ArrayList<>();
		for (CtClass c : parameterClasses) {
			if (!c.isPrimitive()) {
				argClasses.add("(($" + (p + 1) + " != null)?($" + (p + 1) + ").getClass().getName():\"" + c.getName() + "\")");
				argObjects.add("(($" + (p + 1) + " != null)?System.identityHashCode($" + (p + 1) + "):0)");
			} else {
				argClasses.add("\"" + c.getName() + "\"");								// 基本型の場合、getClass()できないため
				if (c != CtClass.charType) {
					argObjects.add("$" + (p + 1));
				} else {
					argObjects.add("Character.getNumericValue($" + (p + 1) + ")");		// 文字型の場合文字が出力されてしまうため				
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
			// staticメソッドの場合
			if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
				returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
				returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			} else {
				returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";	// void と基本型の場合、getClass()できないため
				if (((CtMethod)m).getReturnType() != CtClass.charType) {
					returnedObject = "$_";
				} else {
					returnedObject = "Character.getNumericValue($_)";					// 文字型の場合文字が出力されてしまうため				
				}
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			}
		} else if (m instanceof CtConstructor) {
			// コンストラクタの場合(クラス初期化子の場合もある)
			if (!isCallerSideInstrumentation) {
				// 呼び出し先に埋め込む場合
				if ((m.getModifiers() & Modifier.STATIC) == 0) {
					// 通常のコンストラクタの場合
					returnedClass = "$0.getClass().getName()";
					returnedObject = "System.identityHashCode($0)";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "System.identityHashCode($0)";
				} else {
					// クラス初期化子の場合
					returnedClass = "\"void\"";
					returnedObject = "\"0\"";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "\"0\"";
				}
			} else {
				// 呼出し元に埋め込む場合(標準クラスもしくはデフォルトコンストラクタの呼出し、または親コンストラクタの呼出し)
				returnedClass = "(($_ != null)?$_.getClass().getName():$0.getClass().getName())";
				returnedObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
			}
		} else {
			// 通常のメソッドの場合
			if (!isCallerSideInstrumentation) {
				// 呼び出し先に埋め込む場合(通常)
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		// void と基本型の場合、getClass()できないため
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// 文字型の場合文字が出力されてしまうため				
					}
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				}
			} else {
				// 呼出し元に埋め込む場合(標準クラスの呼出し)
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "$0.getClass().getName()"; 
					thisObject = "System.identityHashCode($0)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		// void と基本型の場合、getClass()できないため
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// 文字型の場合文字が出力されてしまうため				
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
			String classPath = "\"" + URLDecoder.decode(cc.getURL().getPath(), "UTF-8") + "\"";		// パスが URL encode になっているため
			String loaderPath = "\"" + URLDecoder.decode(cc.getClassPool().getClassLoader().getResource("").getPath(), "UTF-8") + "\"";		// パスが URL encode になっているため
			return generator.generateInsertBeforeStatementsForClassDefinition(className, classPath, loaderPath);
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
}
