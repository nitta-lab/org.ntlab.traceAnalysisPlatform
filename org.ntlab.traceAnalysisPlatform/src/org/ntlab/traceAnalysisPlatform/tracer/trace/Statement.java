package org.ntlab.traceAnalysisPlatform.tracer.trace;

/**
 * An execution of a statement in a trace
 * @author Nitta
 *
 */
public class Statement {
	protected int lineNo;
	protected String threadNo;
	protected long timeStamp = 0L;

	public Statement(int lineNo, String threadNo) {
		this.lineNo = lineNo;
		this.threadNo = threadNo;
	}

	public Statement(int lineNo, String threadNo, long timeStamp) {
		this(lineNo, threadNo);
		this.timeStamp = timeStamp;
	}

	public int getLineNo() {
		return lineNo;
	}

	public String getThreadNo() {
		return threadNo;
	}

	public long getTimeStamp() {
		return timeStamp;
	}
}