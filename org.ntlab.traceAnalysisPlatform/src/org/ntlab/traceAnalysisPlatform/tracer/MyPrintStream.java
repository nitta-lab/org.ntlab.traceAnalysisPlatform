package org.ntlab.traceAnalysisPlatform.tracer;

import java.io.PrintStream;
import java.util.LinkedList;

/**
 * �g���[�X�o�͗p���[�e�B���e�B
 * 
 * @author Nitta
 *
 */
public class MyPrintStream extends Thread {
	private static MyPrintStream theInstance;
	private static LinkedList<String> output;
	private static String s = null;
	private static PrintStream sysout = null;
//	private static boolean bFlushed = false;
//	private static int count = 0;
	
	private static MyPrintStream getInstance() {
		if (theInstance == null) {
			theInstance = new MyPrintStream();
			output = new LinkedList<String>();
			sysout = System.out;
			Runtime.getRuntime().addShutdownHook(theInstance);		// �V���b�g�_�E���p
//			theInstance.start();
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
//		if (count == 0) {
//			// �ʏ�̃g���[�X�o��
//			count++;
//			String s;
//			Runtime.getRuntime().addShutdownHook(new MyPrintStream());		// �V���b�g�_�E���p�ɂ����ЂƂC���X�^���X���쐬����
//			while(!bFlushed) {
//				try {
//					Thread.sleep(10);
//					if (output.size() > 0) {
//						synchronized (output) {
//							s = output.remove(0);
//						}
//						System.out.println(s);
//					}
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		} else {
			// �V���b�g�_�E�����Ƀo�b�t�@�Ɏc�����g���[�X���o�͂��؂�
//			bFlushed = true;
		int size = output.size();
		String s;
		for (int i = 0; i < size; i++) {
			synchronized (output) {
				s = output.get(i);
			}
			sysout.println(s);
			synchronized (output) {
				size = output.size();	
			}
		}			
//		}
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
