package org.ntlab.traceAnalysisPlatform.tracer;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread to output a trace 
 * 
 * @author Nitta
 *
 */
public class OfflineTraceOutput extends Thread {
	private static OfflineTraceOutput theInstance;
	private static ConcurrentLinkedQueue<String> output;
	private static String s = null;
	private static PrintStream sysout = null;
	
	private static OfflineTraceOutput getInstance() {
		if (theInstance == null) {
			theInstance = new OfflineTraceOutput();
			output = new ConcurrentLinkedQueue<String>();
			sysout = System.out;
			Runtime.getRuntime().addShutdownHook(theInstance);		// for shutdown
		}
		return theInstance;
	}
	
	public static void print(int n) {
		getInstance()._print(n);
	}

	public static void print(String s) {
		getInstance()._print(s);
	}		
	
	public static void println() {
		getInstance()._println();
	}

	public static void println(String s) {
		getInstance()._println(s);
	}

	public void run() {
		// Output a trace file after shutdown.
		for (String s: output) {
			sysout.println(s);
		}			
	}
	
	private synchronized void _print(int n) {
		if (s == null) {
			s = Integer.toString(n);
		} else {
			s += n;
		}
	}

	private synchronized void _print(String s1) {
		if (s == null) {
			s = s1;
		} else {
			s += s1;
		}
	}		
	
	private synchronized void _println() {
		if (s == null) {
			s = "";
		}
		output.add(s);
		s = null;
	}

	private synchronized void _println(String s1) {
		if (s == null) {
			s = s1;
		} else {
			s += s1;
		}
		output.add(s);
		s = null;
	}
}
