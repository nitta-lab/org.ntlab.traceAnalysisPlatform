package org.ntlab.traceAnalysisPlatform.tracer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

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
 * �g���[�X�o�͕���Ώۃp�b�P�[�W���ɐD�荞�ރN���X
 * 
 * @author Nitta
 *
 */
public class Tracer {
	public static int lineNo = 1;
	public static final String TRACER = "org.ntlab.traceAnalysisPlatform.tracer.";
	public static final String TRACER_CLASS_PATH = "org/ntlab/traceAnalysisPlatform/tracer/Tracer.class";
	public static final String JAVASSIST_LIBRARY = "javassist.jar";
	private static final String STANDARD_CLASSES = "java.util.ListIterator|java.util.Iterator|java.util.List|java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.Map|java.util.HashMap|java.util.Set|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.lang.Thread";
	private static final String CONCRETE_STANDARD_CLASSES = "java.util.Vector|java.util.ArrayList|java.util.Stack|java.util.HashMap|java.util.HashSet|java.util.Hashtable|java.util.LinkedList|java.lang.Thread";
	private static final String EXCEPT_FOR_METHODS = "java.lang.Thread.currentThread..|java.lang.Thread.getId..";
	private static final String STANDARD_LIB = "java.";
	private static OutputStatementsGenerator outputStatementsGenerator = null;
	private static ClassPool cp = null;
	private static CodeConverter conv = null;

	public static void main(String[] args) {
		initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// �����ŏo�̓t�H�[�}�b�g���w�肷��		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL resource = loader.getResource("");
		try {
			String classPath = URLDecoder.decode(resource.getPath(), "UTF-8");
			packageInstrumentation("worstCase/", classPath);							// �w�肵���p�b�P�[�W���̑S�N���X�ɃC���X�g�D�������e�[�V�������s��
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * �o�͕���������w�肵�ăC���X�g�D�������e�[�V�����̏��������s��
	 * 
	 * @param outputStatementsGenerator �o�͕�������(�o�̓t�H�[�}�b�g���w��ł���)
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator) {
		initialize(outputStatementsGenerator, ClassPool.getDefault());
	}

	/**
	 * �o�͕���������w�肵�ăC���X�g�D�������e�[�V�����̏��������s��
	 * 
	 * @param outputStatementsGenerator �o�͕�������(�o�̓t�H�[�}�b�g���w��ł���)
	 * @param cp Javassist�̃N���X�v�[��
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator, ClassPool cp) {
		// �z��ւ̃A�N�Z�X�̌��o]
		Tracer.cp = cp;
		Tracer.outputStatementsGenerator = outputStatementsGenerator;	// �����ŏo�̓t�H�[�}�b�g���w�肷��
		Tracer.conv = new CodeConverter();
		if (!(Tracer.outputStatementsGenerator.getGenerator() instanceof PlainTextTraceGenerator)) {
			try {
//				CtClass cc = cp.get(TRACER + "JSONArrayAdvisor");		// JSON�̏ꍇ�̂ݔz��A�N�Z�X���o�͂���
				CtClass cc = cp.get(TRACER + "OnlineArrayAdvisor");		// �I�����C����͂̏ꍇ�ɔz��A�N�Z�X���o�͂���
				conv.replaceArrayAccess(cc, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
			} catch (NotFoundException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * �w�肵���p�b�P�[�W���̃N���X�ɃC���X�g�D�������e�[�V�������s��
	 * 
	 * @param packageName �p�b�P�[�W��
	 * @param classPath �o�͂���N���X�t�@�C���̃N���X�p�X(null �̏ꍇ�͏o�͂��Ȃ�)
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
		}
	}

	/**
	 * �w�肵���N���X�ɃC���X�g�D�������e�[�V�������s��
	 * 
	 * @param className �N���X��
	 * @param classPath �o�͂���N���X�t�@�C���̃N���X�p�X(null �̏ꍇ�͏o�͂��Ȃ�)
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
	 * �w�肵���N���X�ɃC���X�g�D�������e�[�V�������s��
	 * 
	 * @param cc Javassist�̃N���X�I�u�W�F�N�g
	 * @param classPath �o�͂���N���X�t�@�C���̃N���X�p�X(null �̏ꍇ�͏o�͂��Ȃ�)
	 */
	public static void classInstrumentation(CtClass cc, String classPath) throws BadBytecode, NotFoundException, CannotCompileException, IOException {
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
//			cc.rebuildClassFile();
			cc.getClassFile().compact();		// ���ꂪ�Ȃ��ƁA���s���� java.lang.ClassFormatError: Truncated class file �ŗ�����
			cc.debugWriteFile(classPath);
//			cc.defrost();
			cc.detach();
		}
	}

	private static void classInitializerInstrumentation(CtClass cc, CtConstructor classInitializer) throws BadBytecode, NotFoundException, CannotCompileException {
		if (classInitializer != null) {
			methodInstrumentation(cc, classInitializer);
		} else {
			classInitializer = cc.makeClassInitializer();
		}
		classInitializer.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForClassDefinition(cc, classInitializer));
		if (classInitializer.getMethodInfo().getCodeAttribute() != null) {
			classInitializer.getMethodInfo().getCodeAttribute().computeMaxStack();		// ���ꂪ�Ȃ��ƁA���s����  java.lang.VerifyError: Stack map does not match the one at exception handler... �ŗ�����
		}
	}

	private static void methodInstrumentation(final CtClass cc, final CtBehavior m) throws BadBytecode, NotFoundException, CannotCompileException {
		// ���\�b�h�{�̓��̊e�u���b�N�̍ŏ��ɏo�͕���}������(�o�͕��̑}���Ńu���b�N�������Ă��܂��̂ŁA��ɑ}�����Ă���)
		Block[] blocks = null;
		if (m instanceof CtMethod && !m.isEmpty()) {
			ControlFlow cf = new ControlFlow((CtMethod)m);
			blocks = cf.basicBlocks();
			List list = m.getMethodInfo().getCodeAttribute().getAttributes();
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
		
		// ���\�b�h�{�̓��̃t�B�[���h�A�N�Z�X�ƃ��\�b�h�Ăяo����u��������
		m.instrument(new ExprEditor() {
			public void edit(FieldAccess f) throws CannotCompileException {
				try {
					if (f.isReader()) {
						if (!f.getFieldName().contains("$")) {		// AspectJ�ł� final local �ϐ�����̃Q�b�g�͖��������̂ŁA����ɍ��킹�ď��O����
							f.replace(outputStatementsGenerator.generateReplaceStatementsForFieldGet(cc, m, f, f.getLineNumber()));
						}
					} else {
						if (!f.getFieldName().contains("$")) {		// ���̏������Ȃ��ƂȂ���������i�����t�B�[���h?�ւ̃Z�b�g�������āA������E���ė����Ă�?�j
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
					if (!className.startsWith(STANDARD_LIB) && !className.startsWith(TRACER)) {
						// �ʏ�̃��\�b�h�̌Ăяo��
						c.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, c.getLineNumber(), true));
					} else if (className.matches(STANDARD_CLASSES) && !m.getLongName().matches(EXCEPT_FOR_METHODS)) {
						// �W���N���X�̃��\�b�h�Ăяo���i�Ăяo����Ƀo�C�g�R�[�h�𖄂ߍ��߂Ȃ��j							
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
					if (!className.startsWith(STANDARD_LIB) && !className.startsWith(TRACER)) {
						// �ʏ�̃R���X�g���N�^�̌Ăяo��
						n.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, n.getLineNumber(), true));
					} else if (m.getDeclaringClass().getName().matches(CONCRETE_STANDARD_CLASSES)) {
						// �W���N���X�̃R���X�g���N�^�Ăяo���i�Ăяo����Ƀo�C�g�R�[�h�𖄂ߍ��߂Ȃ��j							
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
		
		// ���\�b�h�p�̏o�͕��𐶐�����
		if (!m.isEmpty()) {
			// ���\�b�h�̎��s�O��ɏo�͕���}������
			m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			m.insertAfter(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
		} else {
			// ���\�b�h�{�̂���̂Ƃ��̓R���X�g���N�^�̏ꍇ�̂�(=�f�t�H���g�R���X�g���N�^)�{�̂ɏo�͕���ݒ肷��
			if (m instanceof CtConstructor) {
				m.setBody(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
				m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			}
		}
		if (m.getMethodInfo().getCodeAttribute() != null) {
			m.getMethodInfo().getCodeAttribute().computeMaxStack();		// ���ꂪ�Ȃ��ƁA���s����  java.lang.VerifyError: Stack map does not match the one at exception handler... �ŗ�����
		}
	}
}
