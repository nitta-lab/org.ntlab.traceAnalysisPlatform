package org.ntlab.traceAnalysisPlatform.tracer;

import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;

public class TracerClassLoader extends ClassLoader {
	
	public TracerClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Tracer.initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// 引数で出力フォーマットを指定する

		// "java"で始まるパッケージはシステム系なのでデフォルトのクラスローダーに任せる
		if (name.startsWith("java") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith(Tracer.TRACER)) {
			return super.loadClass(name, resolve);
		}
		
		try {
			// ロードしたいクラスをJavassist内から取得
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.get(name);
			if (!cc.isFrozen()) {
				// 変更可能なときだけインストゥルメンテーションを行う
				Tracer.classInstrumentation(cc, null);
			}

			// 自分のクラスローダーを指定してJavaVMのクラスに変換
			ProtectionDomain pd = this.getClass().getProtectionDomain();
			Class<?> c = cc.toClass(this, pd);

			if (resolve) {
				resolveClass(c);
			}
			return c;

		} catch (Exception e) {
			return super.loadClass(name, resolve);
		}
	}
}
