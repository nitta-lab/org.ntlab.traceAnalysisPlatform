package org.ntlab.traceAnalysisPlatform.tracer.trace;

public interface IBoundaryChecker {
	abstract public boolean withinBoundary(String methodSignature);
}
