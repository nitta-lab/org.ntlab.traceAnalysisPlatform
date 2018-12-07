package org.ntlab.traceAnalysisPlatform.tracer;

import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * A class loader for load time instrumentation (currently not used)
 * @author Nitta
 *
 */
public class TracerClassLoader extends ClassLoader {
	
	public TracerClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Tracer.initialize(new OutputStatementsGenerator(new JSONTraceGenerator()));		// to specify output format

		// The following packages are the default class loader.
		if (name.startsWith("java") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith(Tracer.TRACER)) {
			return super.loadClass(name, resolve);
		}
		
		try {
			// Get the class to load from the class pool object.
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.get(name);
			if (!cc.isFrozen()) {
				// Instrument only if it is modifiable
				Tracer.classInstrumentation(cc, null);
			}

			// Convert to a JavaVM class by specifying this class loader
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
