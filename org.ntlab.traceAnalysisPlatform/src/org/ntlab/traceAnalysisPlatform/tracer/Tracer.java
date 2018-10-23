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
 * トレース出力文を対象パッケージ内に織り込むクラス
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
		initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// 引数で出力フォーマットを指定する		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL resource = loader.getResource("");
		try {
			String classPath = URLDecoder.decode(resource.getPath(), "UTF-8");
			packageInstrumentation("worstCase/", classPath);							// 指定したパッケージ内の全クラスにインストゥルメンテーションを行う
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * 出力文生成器を指定してインストゥルメンテーションの初期化を行う
	 * 
	 * @param outputStatementsGenerator 出力文生成器(出力フォーマットを指定できる)
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator) {
		initialize(outputStatementsGenerator, ClassPool.getDefault());
	}

	/**
	 * 出力文生成器を指定してインストゥルメンテーションの初期化を行う
	 * 
	 * @param outputStatementsGenerator 出力文生成器(出力フォーマットを指定できる)
	 * @param cp Javassistのクラスプール
	 */
	public static void initialize(OutputStatementsGenerator outputStatementsGenerator, ClassPool cp) {
		// 配列へのアクセスの検出]
		Tracer.cp = cp;
		Tracer.outputStatementsGenerator = outputStatementsGenerator;	// 引数で出力フォーマットを指定する
		Tracer.conv = new CodeConverter();
		if (!(Tracer.outputStatementsGenerator.getGenerator() instanceof PlainTextTraceGenerator)) {
			try {
//				CtClass cc = cp.get(TRACER + "JSONArrayAdvisor");		// JSONの場合のみ配列アクセスを出力する
				CtClass cc = cp.get(TRACER + "OnlineArrayAdvisor");		// オンライン解析の場合に配列アクセスを出力する
				conv.replaceArrayAccess(cc, new CodeConverter.DefaultArrayAccessReplacementMethodNames());
			} catch (NotFoundException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 指定したパッケージ内のクラスにインストゥルメンテーションを行う
	 * 
	 * @param packageName パッケージ名
	 * @param classPath 出力するクラスファイルのクラスパス(null の場合は出力しない)
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
	 * 指定したクラスにインストゥルメンテーションを行う
	 * 
	 * @param className クラス名
	 * @param classPath 出力するクラスファイルのクラスパス(null の場合は出力しない)
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
	 * 指定したクラスにインストゥルメンテーションを行う
	 * 
	 * @param cc Javassistのクラスオブジェクト
	 * @param classPath 出力するクラスファイルのクラスパス(null の場合は出力しない)
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
			cc.getClassFile().compact();		// これがないと、実行時に java.lang.ClassFormatError: Truncated class file で落ちる
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
			classInitializer.getMethodInfo().getCodeAttribute().computeMaxStack();		// これがないと、実行時に  java.lang.VerifyError: Stack map does not match the one at exception handler... で落ちる
		}
	}

	private static void methodInstrumentation(final CtClass cc, final CtBehavior m) throws BadBytecode, NotFoundException, CannotCompileException {
		// メソッド本体内の各ブロックの最初に出力文を挿入する(出力文の挿入でブロックが増えてしまうので、先に挿入しておく)
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
		
		// メソッド本体内のフィールドアクセスとメソッド呼び出しを置き換える
		m.instrument(new ExprEditor() {
			public void edit(FieldAccess f) throws CannotCompileException {
				try {
					if (f.isReader()) {
						if (!f.getFieldName().contains("$")) {		// AspectJでは final local 変数からのゲットは無視されるので、それに合わせて除外する
							f.replace(outputStatementsGenerator.generateReplaceStatementsForFieldGet(cc, m, f, f.getLineNumber()));
						}
					} else {
						if (!f.getFieldName().contains("$")) {		// この条件がないとなぜか落ちる（無名フィールド?へのセットがあって、それを拾って落ちてる?）
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
						// 通常のメソッドの呼び出し
						c.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, c.getLineNumber(), true));
					} else if (className.matches(STANDARD_CLASSES) && !m.getLongName().matches(EXCEPT_FOR_METHODS)) {
						// 標準クラスのメソッド呼び出し（呼び出し先にバイトコードを埋め込めない）							
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
						// 通常のコンストラクタの呼び出し
						n.replace(outputStatementsGenerator.generateReplaceStatementsForCall(m.getDeclaringClass(), m, n.getLineNumber(), true));
					} else if (m.getDeclaringClass().getName().matches(CONCRETE_STANDARD_CLASSES)) {
						// 標準クラスのコンストラクタ呼び出し（呼び出し先にバイトコードを埋め込めない）							
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
		
		// メソッド用の出力文を生成する
		if (!m.isEmpty()) {
			// メソッドの実行前後に出力文を挿入する
			m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			m.insertAfter(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
		} else {
			// メソッド本体が空のときはコンストラクタの場合のみ(=デフォルトコンストラクタ)本体に出力文を設定する
			if (m instanceof CtConstructor) {
				m.setBody(outputStatementsGenerator.generateInsertAfterStatementsForMethodBody(cc, m));
				m.insertBefore(outputStatementsGenerator.generateInsertBeforeStatementsForMethodBody(cc, m));
			}
		}
		if (m.getMethodInfo().getCodeAttribute() != null) {
			m.getMethodInfo().getCodeAttribute().computeMaxStack();		// これがないと、実行時に  java.lang.VerifyError: Stack map does not match the one at exception handler... で落ちる
		}
	}
}
