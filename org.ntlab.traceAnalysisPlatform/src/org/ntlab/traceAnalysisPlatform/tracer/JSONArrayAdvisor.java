package org.ntlab.traceAnalysisPlatform.tracer;

/**
 * Library to output runtime information of array accesses (to a JSON trace file)
 * @author Nitta
 *
 */
public class JSONArrayAdvisor {
	public static void arrayWriteInt(Object array, int index, int value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "int", Integer.toString(value), threadId, timeStamp);
		((int [])array)[index] = value;
	}

	public static int arrayReadInt(Object array, int index) {
		int value = ((int [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "int", Integer.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteLong(Object array, int index, long value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "long", Long.toString(value), threadId, timeStamp);
		((long [])array)[index] = value;
	}

	public static long arrayReadLong(Object array, int index) {
		long value = ((long [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "long", Long.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteFloat(Object array, int index, float value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "float", Float.toString(value), threadId, timeStamp);
		((float [])array)[index] = value;
	}

	public static float arrayReadFloat(Object array, int index) {
		float value = ((float [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "float", Float.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteDouble(Object array, int index, double value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "double", Double.toString(value), threadId, timeStamp);
		((double [])array)[index] = value;
	}

	public static double arrayReadDouble(Object array, int index) {
		double value = ((double [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "double", Double.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteShort(Object array, int index, short value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "short", Short.toString(value), threadId, timeStamp);
		((short [])array)[index] = value;
	}

	public static short arrayReadShort(Object array, int index) {
		short value = ((short [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "short", Short.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteChar(Object array, int index, char value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "char", Integer.toString(Character.getNumericValue(value)), threadId, timeStamp);
		((char [])array)[index] = value;
	}

	public static char arrayReadChar(Object array, int index) {
		char value = ((char [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "char", Integer.toString(Character.getNumericValue(value)), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteByteOrBoolean(Object array, int index, byte value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "byte", Byte.toString(value), threadId, timeStamp);
		((byte [])array)[index] = value;
	}

	public static byte arrayReadByteOrBoolean(Object array, int index) {
		byte value = ((byte [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, "byte", Byte.toString(value), threadId, timeStamp);
		return value;
	}
	
	public static void arrayWriteObject(Object array, int index, Object value) {
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arraySetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, 
				((value !=null)?value.getClass().getName():"---"), ((value != null)?Integer.toString(System.identityHashCode(value)):"0"), threadId, timeStamp);
		((Object [])array)[index] = value;
	}

	public static Object arrayReadObject(Object array, int index) {
		Object value = ((Object [])array)[index];
		long threadId = Thread.currentThread().getId();
		long timeStamp = System.nanoTime();
		JSONTraceGenerator.arrayGetOutput(array.getClass().getName(), Integer.toString(System.identityHashCode(array)), index, 
				((value !=null)?value.getClass().getName():"---"), ((value != null)?Integer.toString(System.identityHashCode(value)):"0"), threadId, timeStamp);
		return value;
	}
}
