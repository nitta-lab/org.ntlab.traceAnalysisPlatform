package org.ntlab.traceAnalysisPlatform.tracer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.bytecode.analysis.ControlFlow.Catcher;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;

/**
 * The class that embeds into the specified package the statements that output traces
 * 
 * @author Nitta
 *
 */
public class Tracer {
	public static int lineNo = 1;
	public static final String TRACER = "org.ntlab.traceAnalysisPlatform.tracer.";
	public static final String TRACER_CLASS_PATH = "org/ntlab/traceAnalysisPlatform/tracer/Tracer.class";
	public static final String JAVASSIST_LIBRARY = "javassist.jar";
	private static final String STANDARD_CLASSES = "java.util.Collection|java.util.ListIterator|java.util.Iterator|java.util.List|java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.Map|java.util.HashMap|java.util.Set|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.util.Collections|java.util.Arrays|java.lang.Thread|java.awt.Component|java.awt.Container|javax.swing.AbstractButton|javax.swing.ActionMap|javax.swing.JTabbedPane";
	private static final String CONCRETE_STANDARD_CLASSES = "java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.HashMap|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.lang.Thread|java.awt.Container|java.awt.Panel|java.awt.ScrollPane|java.awt.Window|java.awt.Dialog|java.awt.Frame|javax.swing.JFrame|javax.swing.JPanel|javax.swing.JScrollPane|javax.swing.JTabbedPane|javax.swing.JToolBar|javax.swing.JMenuBar|javax.swing.JButton|javax.swing.JMenuItem|javax.swing.JMenu|javax.swing.JToggleButton|javax.swing.ActionMap";
//	private static final String STANDARD_CLASSES = "java.util.ListIterator|java.util.Iterator|java.util.List|java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.Map|java.util.HashMap|java.util.Set|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.lang.Thread";
//	private static final String CONCRETE_STANDARD_CLASSES = "java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.HashMap|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.lang.Thread";
	private static final String EXCEPT_FOR_METHODS = "java.lang.Thread.currentThread..|java.lang.Thread.getId..";
	private static final String STANDARD_LIB = "java.";
	private static final String STANDARD_LIB2 = "javax.";
	private static OutputStatementsGenerator outputStatementsGenerator = null;
	private static ClassPool cp = null;
	private static CodeConverter conv = null;
	private static IProgressMonitor monitor = null;
	
	private static final int MAX_METHOD_LENGTH = 65535;

	public static void main(String[] args) {
		initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// Specify the output format by the instance of ITraceGenerator
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL resource = loader.getResource("");
		try {
			String classPath = URLDecoder.decode(resource.getPath(), "UTF-8");
			packageInstrumentation("worstCase/", classPath);							// Instrument the all classes under the specified package
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initialize the instrumentation by specifying an output statements generator
	 * 
	 * @param outputStatementsGenerator a generator of the statements that output traces
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator) {
		initialize(outputStatementsGenerator, ClassPool.getDefault());
	}

	/**
	 * Initialize the instrumentation by specifying an output statements generator and a class pool
	 * 
	 * @param outputStatementsGenerator	a generator of the statements that output traces
	 * @param cp						a ClassPool object
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator, ClassPool cp) {
		initialize(outputStatementsGenerator, cp, null);
	}
	
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator, ClassPool cp, IProgressMonitor monitor) {
		Tracer.cp = cp;
		Tracer.outputStatementsGenerator = outputStatementsGenerator;
		Tracer.conv = new CodeConverter();
		if (outputStatementsGenerator.getGenerator().getArrayAdvisorClassName() != null) {
			try {
				CtClass cc = cp.get(TRACER + outputStatementsGenerator.getGenerator().getArrayAdvisorClassName());	// To collect array accesses by Javassist
				conv.replaceArrayAccess(cc, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
			} catch (NotFoundException e1) {
				e1.printStackTrace();
			}
		}
		Tracer.monitor = monitor;
	}

	/**
	 * Instrument the all classes under a specified package
	 * 
	 * @param packageName	the name of a package where the classes are instrumented
	 * @param classPath		the base path of the class files to be output (null for not output)
	 */
	public static void packageInstrumentation(String packageName, String classPath) {
		File dir;
		dir = new File(classPath + packageName);
		for (File file : dir.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".class")) {
				String className = packageName.replace("/", ".") + file.getName().replace(".class", "");
				classInstrumentation(className, classPath);
			} else if (file.isDirectory()) {
				packageInstrumentation(packageName + file.getName() + "/", classPath);
			}
			if (monitor != null && monitor.isCanceled()) return;
		}
	}

	/**
	 * Instrument the specified class
	 * 
	 * @param className the name of the class to instrument
	 * @param classPath the base path of the class files to be output (null for not output)
	 */
	public static void classInstrumentation(String className, String classPath) {
		try {
			CtClass cc = cp.get(className);
//			cc.defrost();
//			cc.stopPruning(true);
			if (!cc.isFrozen()) classInstrumentation(cc, classPath);
		} catch (NotFoundException | BadBytecode | CannotCompileException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Instrument the specified class
	 * 
	 * @param cc		a class object of Javassist
	 * @param classPath	the base path of the class files to be output (null for not output)
	 */
	public static void classInstrumentation(CtClass cc, String classPath) throws BadBytecode, NotFoundException, CannotCompileException, IOException {
		try {
			classInitializerInstrumentation(cc, cc.getClassInitializer());
			
			for (final CtConstructor c : cc.getDeclaredConstructors()) {
				methodInstrumentation(cc, c);
			}
			for (final CtMethod m : cc.getDeclaredMethods()) {
				methodInstrumentation(cc, m);
			}
			try {
				cc.instrument(conv);
			} catch (CannotCompileException e) {
				e.printStackTrace();
			}
			if (classPath != null) {
				if (classPath.endsWith("/")) {
					classPath = classPath.substring(0, classPath.length() - 1);
				}
				System.out.println(classPath + ":" + cc.getName());
//				cc.rebuildClassFile();
				cc.getClassFile().compact();		// Without this, a runtime error (java.lang.ClassFormatError: Truncated class file) occurs
				cc.debugWriteFile(classPath);
//				cc.defrost();
				cc.detach();
			}
		} catch (MethodSizeExcessException e) {
			e.printStackTrace();
		}
	}

	private static void classInitializerInstrumentation(CtClass cc, CtConstructor classInitializer) throws BadBytecode, NotFoundException, CannotCompileException, MethodSizeExcessException {
		if (classInitializer != null) {
			methodInstrumentation(cc, classInitializer);
		} else {
			classInitializer = cc.makeClassInitializer();
		}
		classInitializer.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForClassDefinition(cc, classInitializer));
		if (classInitializer.getMethodInfo().getCodeAttribute() != null) {
			classInitializer.getMethodInfo().getCodeAttribute().computeMaxStack();		// Without this, a runtime error (java.lang.VerifyError: Stack map does not match the one at exception handler...) occurs
		}
	}

	private static void methodInstrumentation(final CtClass cc, final CtBehavior m) throws BadBytecode, NotFoundException, CannotCompileException, MethodSizeExcessException {
		// Insert the set of output statements at the entry of each basic block in the body of method m. 
		//  (Since every set of output statements is inserted as a new basic block, the sets of output statements for basic blocks are inserted first.)
		Block[] blocks = null;
		if (m instanceof CtMethod && !m.isEmpty()) {
			ControlFlow cf = new ControlFlow((CtMethod)m);
			blocks = cf.basicBlocks();
			List<?> list = m.getMethodInfo().getCodeAttribute().getAttributes();
			LineNumberAttribute attr = null;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof LineNumberAttribute){
					attr = (LineNumberAttribute)list.get(i);
					break;
				}
			}
			int[] lines = new int[blocks.length];
			for (int i = 0; i < blocks.length; i++) {
				int pos = blocks[i].position();
				int line = m.getMethodInfo().getLineNumber(pos);
				if (attr != null && pos != attr.toStartPc(line)) {
					lines[i] = -1;
				} else {
					lines[i] = line;
				}
			}
			for (int i = blocks.length - 1; i >= 0; i--) {
				for (int j = 0; j < i; j++) {
					if (lines[j] == lines[i]) {
						lines[i] = -1;
						break;
					}
					if (blocks[j].catchers().length > 0) {
						for (Catcher c: blocks[j].catchers()) {
							if (m.getMethodInfo().getLineNumber(c.block().position()) == lines[i]) {
								lines[i] = -1;
								break;
							}
						}
						if (lines[i] == i) break;
					}
				}
				if (lines[i] != -1) {
					String code = outputStatementsGenerator.generateInsertStatementsForBlockEntry((CtMethod)m, i, blocks[i], lines[i]);
					try {
						m.insertAt(lines[i], code);
					} catch (CannotCompileException e) {
						System.out.println(m.getLongName() + ":" + lines[i] + ":" + blocks[i].catchers().length + ":" + code);
					}
				}
			}
		}
		
		// Replace field accesses and method invocations in the method body with the sets of output stataments
		m.instrument(new ExprEditor() {
			public void edit(FieldAccess f) throws CannotCompileException {
				try {
					if (f.isReader()) {
						if (!f.getFieldName().contains("$")) {		// For the compatibility with AspectJ version. (Access to a final local variable is ignored in AspectJ.)
							f.replace(outputStatementsGenerator.generateReplaceStatementsForFieldGet(cc, m, f, f.getLineNumber()));
						}
					} else {
						if (!f.getFieldName().contains("$")) {		// Without this condition, for some reason, the instrumented program terminates. 
							f.replace(outputStatementsGenerator.generateReplaceStatementsForFieldSet(cc, f, f.getLineNumber()));
						}
					}
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
			}
			public void edit(MethodCall c) throws CannotCompileException {
				try {
					CtMethod m = c.getMethod();
					String className = m.getDeclaringClass().getName();
					if (!className.startsWith(STANDARD_LIB) && !className.startsWith(STANDARD_LIB2) && !className.startsWith(TRACER)) {
						// Normal method invocation
						c.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, c.getLineNumber(), true));
					} else if (className.matches(STANDARD_CLASSES) && !m.getLongName().matches(EXCEPT_FOR_METHODS)) {
						// Invocation of a method in a standard class （Instrumentation to the callee is not allowed.）							
						c.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, c.getLineNumber(), false));
					}
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
			}
			public void edit(NewExpr n) throws CannotCompileException {
				try {
					CtConstructor m = n.getConstructor();
					String className = m.getDeclaringClass().getName();
					if (!className.startsWith(STANDARD_LIB) && !className.startsWith(STANDARD_LIB2) && !className.startsWith(TRACER)) {
						// Normal constructor invocation
						n.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, n.getLineNumber(), true));
					} else if (m.getDeclaringClass().getName().matches(CONCRETE_STANDARD_CLASSES)) {
						// Invocation of a constructor in a standard class （Instrumentation to the callee is not allowed.）
						n.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, n.getLineNumber(), false));
					}
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
			}
			public void edit(NewArray a) throws CannotCompileException {
				a.replace(outputStatementsGenerator.generateReplaceStatementsForNewArray(a, a.getLineNumber()));
			}
//			public void edit(ConstructorCall c) throws CannotCompileException {
//				try {
//					CtConstructor m = c.getConstructor();
//					if (m.getDeclaringClass().getName().matches(CONCRETE_STANDARD_CLASSES)) {
//						c.replace(generateReplaceStatementsForCall(m.getDeclaringClass(), m));
//					}
//				} catch (NotFoundException e) {
//					e.printStackTrace();
//				}
//			}
		});
		
		// Generate output statements for the method.
		if (!m.isEmpty()) {
			// Insert output statements before and after the method body.
			m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			m.insertAfter(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
		} else {
			// If the method body is empty and the method is a constructor (i.e., a default constructor), generate insert output statements for it.
			if (m instanceof CtConstructor) {
				m.setBody(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
				m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			}
		}
		if (m.getMethodInfo().getCodeAttribute() != null) {
			m.getMethodInfo().getCodeAttribute().computeMaxStack();		// Without this, a runtime error (java.lang.VerifyError: Stack map does not match the one at exception handler...) occurs
		}
		if (!m.isEmpty() && m.getMethodInfo().getCodeAttribute().getCodeLength() > MAX_METHOD_LENGTH) {
			throw new MethodSizeExcessException();
		}
	}
}
