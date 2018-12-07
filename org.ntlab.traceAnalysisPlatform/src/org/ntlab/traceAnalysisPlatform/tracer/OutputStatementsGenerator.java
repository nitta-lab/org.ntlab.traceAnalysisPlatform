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
 * Generator of statements to output a trace Åiwith delegating output format dependent part to ITraceGeneratorÅj
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
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// because getClass() is not defined in primitive types
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$1";
			} else {
				valueObject = "Character.getNumericValue($1)";					// because the string value is output for the string type
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
			// In the case of a static method or a constructor
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
			valueClass = "\"" + f.getField().getType().getName() + "\"";		// because getClass() is not defined in primitive types
			if (f.getField().getType() != CtClass.charType) {
				valueObject = "$_";
			} else {
				valueObject = "Character.getNumericValue($_)";					// because the string value is output for the string type
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
			// In the case that the callee can be instrumented
			return generateInsertStatementsForCall(cls, m, line) + " $_ = $proceed($$);";
		} else {
			// In the case that the callee cannot be instrumented
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
	 * Generate statements to output a trace
	 * @param cls a target class
	 * @param m   a target method(or constructor)
	 * @param isCallerSideInstrumentation whether the generated statements are to be instrumented into the caller side or callee side?
	 * @return
	 * @throws NotFoundException
	 */
	private String generateInsertBeforeStatements(CtClass cls, CtBehavior m, boolean isCallerSideInstrumentation) throws NotFoundException {
		// Synthesize the method signature
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
			// In the case of a static method
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	//  for the compatibility with AspectJ version
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else if (m instanceof CtConstructor) {
			// In the case of a constructor (or class initializer)
			methodSignature = "\"" + modifiers + m.getLongName().replace('$', '.') + "\"";	// for the compatibility with AspectJ version
			thisClass = "\"" + declaredClassName + "\"";
			thisObject = "\"0\"";
		} else {
			// In the case of a standard method
			methodSignature = "\"" + modifiers + ((CtMethod)m).getReturnType().getName() + " " + m.getLongName().replace('$', '.') + "\"";	//  for the compatibility with AspectJ version
			if (!isCallerSideInstrumentation) {
				// In the case of callee instrumentation (normal)
				thisClass = "this.getClass().getName()";
				thisObject = "System.identityHashCode(this)";
			} else {
				// In the case of caller instrumentation (a call to a standard class)				
				thisClass = "$0.getClass().getName()";
				thisObject = "System.identityHashCode($0)";
			}
		}
		// Synthesize the statements to output the arguments
		int p = 0;
		CtClass parameterClasses[] = m.getParameterTypes();
		ArrayList<String> argClasses = new ArrayList<>();
		ArrayList<String> argObjects = new ArrayList<>();
		for (CtClass c : parameterClasses) {
			if (!c.isPrimitive()) {
				argClasses.add("(($" + (p + 1) + " != null)?($" + (p + 1) + ").getClass().getName():\"" + c.getName() + "\")");
				argObjects.add("(($" + (p + 1) + " != null)?System.identityHashCode($" + (p + 1) + "):0)");
			} else {
				argClasses.add("\"" + c.getName() + "\"");								// because getClass() is not defined in primitive types
				if (c != CtClass.charType) {
					argObjects.add("$" + (p + 1));
				} else {
					argObjects.add("Character.getNumericValue($" + (p + 1) + ")");		// because the string value is output for the string type				
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
			// In the case of a static method
			if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
				returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
				returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			} else {
				returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";	// because getClass() is not defined in primitive types and the void type
				if (((CtMethod)m).getReturnType() != CtClass.charType) {
					returnedObject = "$_";
				} else {
					returnedObject = "Character.getNumericValue($_)";					// because the string value is output for the string type				
				}
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "\"0\"";
			}
		} else if (m instanceof CtConstructor) {
			// In the case of a constructor (or class initializer)
			if (!isCallerSideInstrumentation) {
				// In the case of callee instrumentation
				if ((m.getModifiers() & Modifier.STATIC) == 0) {
					// In the case of a normal constructor
					returnedClass = "$0.getClass().getName()";
					returnedObject = "System.identityHashCode($0)";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "System.identityHashCode($0)";
				} else {
					// In the case of a class initializer
					returnedClass = "\"void\"";
					returnedObject = "\"0\"";
					thisClass = "\"" + declaredClassName + "\"";
					thisObject = "\"0\"";
				}
			} else {
				// In the case of caller instrumentation (a call to a constructor of a standard class, a default constructor or a super constructor)
				returnedClass = "(($_ != null)?$_.getClass().getName():$0.getClass().getName())";
				returnedObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
				thisClass = "\"" + declaredClassName + "\"";
				thisObject = "(($_ != null)?System.identityHashCode($_):System.identityHashCode($0))";
			}
		} else {
			// In the case of a standard method
			if (!isCallerSideInstrumentation) {
				// In the case of callee instrumentation (normal)
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		// because getClass() is not defined in primitive types and the void type
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// because the string value is output for the string type				
					}
					thisClass = "this.getClass().getName()"; 
					thisObject = "System.identityHashCode(this)";
				}
			} else {
				// In the case of caller instrumentation (a call to a standard class)				
				if (!((CtMethod)m).getReturnType().isPrimitive() || ((CtMethod)m).getReturnType() == CtClass.voidType) {
					returnedClass = "(($_ != null)?$_.getClass().getName():\"void\")";
					returnedObject = "(($_ != null)?System.identityHashCode($_):0)";
					thisClass = "$0.getClass().getName()"; 
					thisObject = "System.identityHashCode($0)";
				} else {
					returnedClass = "\"" + ((CtMethod)m).getReturnType().getName() +"\"";		//  because getClass() is not defined in primitive types and the void type
					if (((CtMethod)m).getReturnType() != CtClass.charType) {
						returnedObject = "$_";
					} else {
						returnedObject = "Character.getNumericValue($_)";						// because the string value is output for the string type				
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
			String classPath = "\"" + URLDecoder.decode(cc.getURL().getPath(), "UTF-8") + "\"";		// because the path is URL encoded
			String loaderPath = "\"" + URLDecoder.decode(cc.getClassPool().getClassLoader().getResource("").getPath(), "UTF-8") + "\"";		// because the path is URL encoded
			return generator.generateInsertBeforeStatementsForClassDefinition(className, classPath, loaderPath);
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
}
