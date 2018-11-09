package org.ntlab.traceAnalysisPlatform.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.ntlab.traceAnalysisPlatform.tracer.ITraceGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.JSONTraceGenerator;

public class InstrumentationHandlerForOfflineAnalysis extends InstrumentationHandler  {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("InstrumentationHandlerForOfflineAnalysis#execute(ExecutionEvent)");
		return super.execute(event);
	}
	
	@Override
	public ITraceGenerator getGenerator() {
		return new JSONTraceGenerator();
	}
}
