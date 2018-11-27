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
		Tracer.initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// �����ŏo�̓t�H�[�}�b�g���w�肷��

		// "java"�Ŏn�܂�p�b�P�[�W�̓V�X�e���n�Ȃ̂Ńf�t�H���g�̃N���X���[�_�[�ɔC����
		if (name.startsWith("java") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith(Tracer.TRACER)) {
			return super.loadClass(name, resolve);
		}
		
		try {
			// ���[�h�������N���X��Javassist������擾
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.get(name);
			if (!cc.isFrozen()) {
				// �ύX�\�ȂƂ������C���X�g�D�������e�[�V�������s��
				Tracer.classInstrumentation(cc, null);
			}

			// �����̃N���X���[�_�[���w�肵��JavaVM�̃N���X�ɕϊ�
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
