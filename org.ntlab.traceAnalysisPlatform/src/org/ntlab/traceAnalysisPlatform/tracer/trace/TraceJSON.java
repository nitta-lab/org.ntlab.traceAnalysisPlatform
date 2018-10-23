package org.ntlab.traceAnalysisPlatform.tracer.trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.ntlab.traceAnalysisPlatform.tracer.OnlineTraceOutput;

//import com.sun.jdi.ObjectReference;
//import com.sun.jdi.Value;

public class TraceJSON extends Trace {
//	private static TraceJSON theTrace = null;
	private HashMap<String, ClassInfo> classes = new HashMap<>();
	private HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
	private ThreadInstance thread = null;
	
	private TraceJSON() {
		
	}

	/**
	 * �w�肵��JSON�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param file �g���[�X�t�@�C��
	 */
	private TraceJSON(BufferedReader file) {
		try {
			readJSON(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * �w�肵��JSON�̃g���[�X�t�@�C������ǂ��� Trace �I�u�W�F�N�g�𐶐�����
	 * @param traceFile �g���[�X�t�@�C���̃p�X
	 */
	private TraceJSON(String traceFile) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(traceFile));
			readJSON(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static TraceJSON getInstance() {
		if (theTrace == null) {
			theTrace = new TraceJSON();
		}
		return (TraceJSON)theTrace;
	}

	private void readJSON(BufferedReader file) throws IOException {
		// �g���[�X�t�@�C���ǂݍ���
		String line = null;
		String[] type;
		String[] classNameData;
		String[] pathData;
		String[] signature;
		String[] receiver;
		String[] arguments;
		String[] lineData;
		String[] threadId;
		String[] thisObj;
		String[] containerObj;
		String[] valueObj;
		String[] returnValue;
		String[] arrayObj;
		String[] thisData;
		String[] containerData;
		String[] valueData;
		String[] returnData;
		String[] fieldData;
		String[] arrayData;
		String[] blockIdData;
		String[] incomingsData;
		String[] dimensionData;
		String[] indexData;
		String className;
		String classPath;
		String loaderPath;
		String time;
		String thisObjectId;
		String thisClassName;
		String containerObjectId;
		String containerClassName;
		String valueObjectId;
		String valueClassName;
		String returnClassName;
		String returnObjectId;
		String arrayObjectId;
		String arrayClassName;
		String shortSignature;
		boolean isConstractor = false;
		boolean isCollectionType = false;
		boolean isStatic = false;
		int dimension;
		int index;
		int blockId;
		int incomings;
		int lineNum;
		long timeStamp = 0L;
		ThreadInstance thread = null;
		HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
		while ((line = file.readLine()) != null) {
			// �g���[�X�t�@�C���̉��
			if (line.startsWith("{\"type\":\"classDef\"")) {
				// �N���X��`
				type = line.split(",\"name\":\"");
				classNameData = type[1].split("\",\"path\":\"");
				className = classNameData[0];
				pathData = classNameData[1].split("\",\"loaderPath\":\"");
				classPath = pathData[0].substring(1);								// �擪�� / ����菜��
				loaderPath = pathData[1].substring(1, pathData[1].length() - 3);	// �擪�� / �ƁA������ "}, ����菜��
				initializeClass(className, classPath, loaderPath);
			} else if (line.startsWith("{\"type\":\"methodCall\"")) {
				// ���\�b�h�Ăяo���̌Ăяo����
				type = line.split(",\"callerSideSignature\":\"");
				signature = type[1].split("\",\"threadId\":");
				threadId = signature[1].split(",\"lineNum\":");
				lineNum = Integer.parseInt(threadId[1].substring(0, threadId[1].length() - 2));	// ������ }, ����菜��
				thread = threads.get(threadId[0]);
				thread.preCallMethod(signature[0], lineNum);
			} else if (line.startsWith("{\"type\":\"methodEntry\"")) {
				// ���\�b�h�Ăяo��
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				isConstractor = false;
				isStatic = false;
				if (signature[0].contains("static ")) {
					isStatic = true;
				}
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// ���\�b�h�Ăяo���̐ݒ�
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// �����̐ݒ�
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"constructorEntry\"")) {
				// �R���X�g���N�^�Ăяo��
				type = line.split("\"signature\":\"");
				signature = type[1].split("\",\"class\":\"");
				receiver = signature[1].split("\",\"args\":");
				arguments = receiver[1].split(",\"threadId\":");
				threadId = arguments[1].split(",\"time\":");
				thisClassName = receiver[0];
				thisObjectId = "0";
				isConstractor = true;
				isStatic = false;
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);			// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				Stack<String> stack;
				if (thread == null) {
					thread = new ThreadInstance(threadId[0]);
					threads.put(threadId[0], thread);
					stack = new Stack<String>();
					stacks.put(threadId[0], stack);
				} else {
					stack = stacks.get(threadId[0]);
				}
				stack.push(signature[0]);
				// ���\�b�h�Ăяo���̐ݒ�
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// �����̐ݒ�
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"methodExit\"")) {
				// ���\�b�h����̕��A
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"receiver\":");
				receiver = signature[1].split(",\"returnValue\":");
				returnValue = receiver[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(receiver[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				returnData = parseClassNameAndObjectId(returnValue[0]);
				returnClassName = returnData[0];
				returnObjectId = returnData[1];
				shortSignature = signature[0];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// ���\�b�h����̕��A�̐ݒ�
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"constructorExit\"")) {
				// �R���X�g���N�^����̕��A
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"returnValue\":");
				returnValue = signature[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				returnData = parseClassNameAndObjectId(returnValue[0]);
				thisClassName = returnClassName = returnData[0];
				thisObjectId = returnObjectId = returnData[1];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				Stack<String> stack = stacks.get(threadId[0]);
				shortSignature = signature[0];
				if (!stack.isEmpty()) {
					String line2 = stack.peek();
					if (line2.endsWith(shortSignature)) {
						stack.pop();
					} else {
						do {
							stack.pop();
							thread.terminateMethod();
							line2 = stack.peek();
						} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
						if (!stack.isEmpty()) stack.pop();
					}
					thread = threads.get(threadId[0]);
					ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
					isCollectionType = false;
					if(thisClassName.contains("java.util.List")
							|| thisClassName.contains("java.util.Vector")
							|| thisClassName.contains("java.util.Iterator")
							|| thisClassName.contains("java.util.ListIterator")
							|| thisClassName.contains("java.util.ArrayList")
							|| thisClassName.contains("java.util.Stack")
							|| thisClassName.contains("java.util.Hash")
							|| thisClassName.contains("java.util.Map")
							|| thisClassName.contains("java.util.Set")
							|| thisClassName.contains("java.util.Linked")
							|| thisClassName.contains("java.lang.Thread")) {
						isCollectionType = true;
					}
					// ���\�b�h����̕��A�̐ݒ�
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"fieldGet\"")) {
				// �t�B�[���h�A�N�Z�X
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"this\":");
				thisObj = fieldData[1].split(",\"container\":");
				containerObj = thisObj[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				thisData = parseClassNameAndObjectId(thisObj[0]);
				thisClassName = thisData[0];
				thisObjectId = thisData[1];
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				// �t�B�[���h�A�N�Z�X�̐ݒ�
				if (thread != null) thread.fieldAccess(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"fieldSet\"")) {
				// �t�B�[���h�X�V
				type = line.split(",\"fieldName\":\"");
				fieldData = type[1].split("\",\"container\":");
				containerObj = fieldData[1].split(",\"value\":");
				valueObj = containerObj[1].split(",\"threadId\":");
				threadId = valueObj[1].split(",\"lineNum\":");
				lineData = threadId[1].split(",\"time\":");
				containerData = parseClassNameAndObjectId(containerObj[0]);
				containerClassName = containerData[0];
				containerObjectId = containerData[1];
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				thread = threads.get(threadId[0]);
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				// �t�B�[���h�X�V�̐ݒ�
				if (thread != null) thread.fieldUpdate(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayCreate\"")) {
				// �z�񐶐�
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"dimension\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				dimensionData = arrayObj[1].split(",\"threadId\":");
				dimension = Integer.parseInt(dimensionData[0]);
				threadId = dimensionData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayCreate(arrayClassName, arrayObjectId, dimension, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arraySet\"")) {
				// �z��v�f�ւ̑��
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayGet\"")) {
				// �z��v�f�̎Q��
				type = line.split(",\"array\":");
				arrayObj = type[1].split(",\"index\":");
				arrayData = parseClassNameAndObjectId(arrayObj[0]);
				arrayClassName = arrayData[0];
				arrayObjectId = arrayData[1];
				indexData = arrayObj[1].split(",\"value\":");
				index = Integer.parseInt(indexData[0]);
				valueObj = indexData[1].split(",\"threadId\":");
				valueData = parseClassNameAndObjectId(valueObj[0]);
				valueClassName = valueData[0];
				valueObjectId = valueData[1];
				threadId = valueObj[1].split(",\"time\":");
				thread = threads.get(threadId[0]);
				time = threadId[1].substring(0, threadId[1].length() - 2);		// ������ }, ����菜��					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"blockEntry\"")) {
				// �u���b�N�̊J�n
				type = line.split(",\"methodSignature\":\"");
				signature = type[1].split("\",\"blockId\":");
				blockIdData = signature[1].split(",\"incomings\":");
				blockId = Integer.parseInt(blockIdData[0]);
				incomingsData = blockIdData[1].split(",\"threadId\":");
				incomings = Integer.parseInt(incomingsData[0]);
				threadId = incomingsData[1].split(",\"lineNum\":");
				thread = threads.get(threadId[0]);
				lineData = threadId[1].split(",\"time\":");
				lineNum = Integer.parseInt(lineData[0]);
				time = lineData[1].substring(0, lineData[1].length() - 2);		// ������ }, ����菜��
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.blockEnter(blockId, incomings, lineNum, timeStamp);
			}
		}
	}

	/**
	 * �N���X���ƃI�u�W�F�N�gID��\��JSON�I�u�W�F�N�g����ǂ���
	 * @param classNameAndObjectIdJSON �g���[�X�t�@�C������JSON�I�u�W�F�N�g
	 * @return
	 */
	protected String[] parseClassNameAndObjectId(String classNameAndObjectIdJSON) {
		// �擪�� {"class":" ��10�����Ɩ����� } ����菜���ĕ���
		return classNameAndObjectIdJSON.substring(10, classNameAndObjectIdJSON.length() - 1).split("\",\"id\":");
	}
	
	/**
	 * ������\��JSON�z�����ǂ���
	 * @param arguments
	 * @return
	 */
	protected ArrayList<ObjectReference> parseArguments(String[] arguments) {
		String[] argData;
		argData = arguments[0].substring(1, arguments[0].length() - 1).split(",");		// �擪�� [ �Ɩ����� ] ����菜��
		ArrayList<ObjectReference> argumentsData = new ArrayList<ObjectReference>();
		for (int k = 0; k < argData.length - 1; k += 2) {
			argumentsData.add(new ObjectReference(argData[k+1].substring(5, argData[k+1].length() - 1), argData[k].substring(10, argData[k].length() - 1)));
		}
		return argumentsData;
	}
	
//	public void initializeClass(String name, String path, String loaderPath) {
//		classes.put(name, new ClassInfo(name, path, loaderPath));
//	}
	
	public static void initializeClass(String name, String path, String loaderPath) {
		getInstance().classes.put(name, new ClassInfo(name, path, loaderPath));

		// �m�F�p
		System.out.println("name = " + name);
		System.out.println("path = " + path);
		System.out.println("loaderPath = " + loaderPath);
		System.out.println();
	}
	
//	public ClassInfo getClassInfo(String className) {
//		return classes.get(className);
//	}
	
	public static ClassInfo getClassInfo(String className) {
		return getInstance().classes.get(className);
	}
	
	public TracePoint getArraySetTracePoint(final Reference ref, TracePoint before) {
		final TracePoint start = before.duplicate();
		before = traverseStatementsInTraceBackward(new IStatementVisitor() {
				@Override
				public boolean preVisitStatement(Statement statement) {
					if (statement instanceof ArrayUpdate) {
						ArrayUpdate arraySet = (ArrayUpdate)start.getStatement();
						String srcObjId = ref.getSrcObjectId();
						String dstObjId = ref.getDstObjectId();
						String srcClassName = ref.getSrcClassName();
						String dstClassName = ref.getDstClassName();
						if ((srcObjId != null && srcObjId.equals(arraySet.getArrayObjectId())) 
							|| (srcObjId == null || isNull(srcObjId)) && srcClassName.equals(arraySet.getArrayClassName())) {
							if ((dstObjId != null && dstObjId.equals(arraySet.getValueObjectId()))
								|| ((dstObjId == null || isNull(dstObjId)) && dstClassName.equals(arraySet.getValueClassName()))) {
								if (srcObjId == null) {
									ref.setSrcObjectId(arraySet.getArrayObjectId());
								} else if (srcClassName == null) {
									ref.setSrcClassName(arraySet.getArrayClassName());
								}
								if (dstObjId == null) {
									ref.setDstObjectId(arraySet.getValueObjectId());
								} else if (dstClassName == null) {
									ref.setDstClassName(arraySet.getValueClassName());
								}
								return true;
							}
						}
					}
					return false;
				}
				@Override
				public boolean postVisitStatement(Statement statement) { return false; }
			}, start);
		if (before != null) {
			return before;
		}
		return null;
	}
	
	/**
	 * ���s���ꂽ�S�u���b�N���擾����
	 * @return �S�u���b�N(���\�b�h��:�u���b�NID)
	 */
	public HashSet<String> getAllBlocks() {
		final HashSet<String> blocks = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * �}�[�N���Ŏ��s���J�n���ꂽ�u���b�N���擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y������u���b�N(���\�b�h��:�u���b�NID)
	 */
	public HashSet<String> getMarkedBlocks(final long markStart, final long markEnd) {
		final HashSet<String> blocks = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					if (methodExecution.getExitTime() < markStart) return true;		// �T���I��
					if (methodExecution.getEntryTime() > markEnd) return false;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							if (entryTime >= markStart && entryTime <= markEnd) {
								blocks.add(methodExecution.getSignature() + ":" + ((BlockEnter)s).getBlockId());
							}
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return blocks;
	}
	
	/**
	 * ���s���ꂽ�S�t���[���擾����
	 * @return �S�t���[(���\�b�h��:�t���[���u���b�NID:�t���[��u���b�NID)
	 */
	public HashSet<String> getAllFlows() {
		final HashSet<String> flows = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (prevBlockId != -1) {
								flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
							} else {
								flows.add(methodExecution.getSignature() + ":" + curBlockID);
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}
	
	/**
	 * �}�[�N���Ŏ��s���ꂽ�t���[���擾����
	 * @param markStart �}�[�N�̊J�n����
	 * @param markEnd �}�[�N�̏I������
	 * @return �Y������t���[(���\�b�h��:�t���[���u���b�NID:�t���[��u���b�NID)
	 */
	public HashSet<String> getMarkedFlows(final long markStart, final long markEnd) {
		final HashSet<String> flows = new HashSet<String>();
		Iterator<String> threadsIterator = threads.keySet().iterator();
		for (; threadsIterator.hasNext();) {
			ThreadInstance thread = threads.get(threadsIterator.next());
			thread.traverseMethodExecutionsBackward(new IMethodExecutionVisitor() {
				@Override
				public boolean preVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean postVisitThread(ThreadInstance thread) {
					return false;
				}
				@Override
				public boolean preVisitMethodExecution(MethodExecution methodExecution) {
					if (methodExecution.getExitTime() < markStart) return true;		// �T���I��
					if (methodExecution.getEntryTime() > markEnd) return false;
					int prevBlockId = -1;
					for (Statement s: methodExecution.getStatements()) {
						if (s instanceof BlockEnter) {
							long entryTime = ((BlockEnter)s).getTimeStamp();
							int curBlockID = ((BlockEnter)s).getBlockId();
							if (entryTime >= markStart && entryTime <= markEnd) {
								if (prevBlockId != -1) {
									flows.add(methodExecution.getSignature() + ":" + prevBlockId + ":" + curBlockID);
								} else {
									flows.add(methodExecution.getSignature() + ":" + curBlockID);
								}
							}
							prevBlockId = curBlockID;
						}
					}
					return false;
				}
				@Override
				public boolean postVisitMethodExecution(MethodExecution methodExecution, ArrayList<MethodExecution> children) {
					return false;
				}
			});
		}	
		return flows;
	}

	public static HashMap<String, ClassInfo> getClasses() {
		return getInstance().classes;
	}
	
	public static HashMap<String, ThreadInstance> getThreads() {
		return getInstance().threads;
	}
	
	public static synchronized void onlineTraceClassDefinition(String className, String classPath, String loaderPath) {
		// classPath��loaderPath�ɂ��Ă͐擪�� / ����菜���ċL�^���� (readJSON���ł̏����Ɠ����ɂȂ�)
		initializeClass(className, classPath.substring(1), loaderPath.substring(1));
	}
	
	public static synchronized void onlineTracePreCallMethod(String signature, String threadId, String lineNum) {
		getInstance().thread = getInstance().threads.get(threadId);
		getInstance().thread.preCallMethod(signature, Integer.parseInt(lineNum));
	}
	
	public static synchronized void onlineTraceMethodEntry(String signature, String thisClassName, String thisObjectId, 
			String threadId, long timeStamp, String argList) {
		boolean isConstractor = false;
		boolean isStatic = false;
		if (signature.contains("static ")) {
			isStatic = true;
		}
		getInstance().thread = getInstance().threads.get(threadId);		
		Stack<String> stack;
		if (getInstance().thread  == null) {
			getInstance().thread = new ThreadInstance(threadId);
			getInstance().threads.put(threadId, getInstance().thread);
			stack = new Stack<String>();
			getInstance().stacks.put(threadId, stack);
		} else {
			stack = getInstance().stacks.get(threadId);
		}
		stack.push(signature);
		// ���\�b�h�Ăяo���̐ݒ�
		getInstance().thread.callMethod(signature, null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
		// �����̐ݒ�
		ArrayList<ObjectReference> arguments = new ArrayList<>();
		String[] args = argList.split(",");
		for (int i = 0; i < args.length - 1; i += 2) {
			arguments.add(new ObjectReference(args[i+1], args[i]));
		}
		getInstance().thread.setArgments(arguments);
	}
	
	public static synchronized void onlineTraceConstructorEntry(String signature, String thisClassName, String thisObjectId, 
			String threadId, long timeStamp, String argList) {
		boolean isConstractor = true;
		boolean isStatic = false;
		getInstance().thread = getInstance().threads.get(threadId);
		Stack<String> stack;
		if (getInstance().thread == null) {
			getInstance().thread = new ThreadInstance(threadId);
			getInstance().threads.put(threadId, getInstance().thread);
			stack = new Stack<String>();
			getInstance().stacks.put(threadId, stack);
		} else {
			stack = getInstance().stacks.get(threadId);
		}
		stack.push(signature);
		// ���\�b�h�Ăяo���̐ݒ�
		getInstance().thread.callMethod(signature, null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
		// �����̐ݒ�
		ArrayList<ObjectReference> arguments = new ArrayList<>();
		String[] args = argList.split(",");
		for (int i = 0; i < args.length - 1; i += 2) {
			arguments.add(new ObjectReference(args[i+1], args[i]));
		}
		getInstance().thread.setArgments(arguments);
	}
	
	public static synchronized void onlineTraceMethodExit(String shortSignature, String thisClassName, String thisObjectId, 
			String returnClassName, String returnObjectId, String threadId, long timeStamp) {
		Stack<String> stack = getInstance().stacks.get(threadId);
		if (!stack.isEmpty()) {
			String line2 = stack.peek();
			if (line2.endsWith(shortSignature)) {
				stack.pop();
			} else {
				do {
					stack.pop();
					getInstance().thread.terminateMethod();
					if (stack.isEmpty()) break;
					line2 = stack.peek();
				} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
				if (!stack.isEmpty()) stack.pop();
			}
			getInstance().thread = getInstance().threads.get(threadId);
			ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
			boolean isCollectionType = false;
			if(thisClassName.contains("java.util.List")
					|| thisClassName.contains("java.util.Vector")
					|| thisClassName.contains("java.util.Iterator")
					|| thisClassName.contains("java.util.ListIterator")
					|| thisClassName.contains("java.util.ArrayList")
					|| thisClassName.contains("java.util.Stack")
					|| thisClassName.contains("java.util.Hash")
					|| thisClassName.contains("java.util.Map")
					|| thisClassName.contains("java.util.Set")
					|| thisClassName.contains("java.util.Linked")
					|| thisClassName.contains("java.lang.Thread")) {
				isCollectionType = true;
			}
			// ���\�b�h����̕��A�̐ݒ�
			getInstance().thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
		}
	}
	
	public static synchronized void onlineTraceConstructorExit(String shortSignature, String returnClassName, String returnObjectId, 
			String threadId, long timeStamp) {
		String thisClassName = returnClassName;
		String thisObjectId = returnObjectId;
		Stack<String> stack = getInstance().stacks.get(threadId);
		if (!stack.isEmpty()) {
			String line2 = stack.peek();
			if (line2.endsWith(shortSignature)) {
				stack.pop();
			} else {
				do {
					stack.pop();
					getInstance().thread.terminateMethod();
					if (stack.isEmpty()) break; // ���̈ꕶ�����ɒǉ�(MethodExit�̕������l)
					line2 = stack.peek();
				} while (!stack.isEmpty() && !line2.endsWith(shortSignature));
				if (!stack.isEmpty()) stack.pop();
			}
			getInstance().thread = getInstance().threads.get(threadId);
			ObjectReference returnVal = new ObjectReference(returnObjectId, returnClassName);					
			boolean isCollectionType = false;
			if(thisClassName.contains("java.util.List")
					|| thisClassName.contains("java.util.Vector")
					|| thisClassName.contains("java.util.Iterator")
					|| thisClassName.contains("java.util.ListIterator")
					|| thisClassName.contains("java.util.ArrayList")
					|| thisClassName.contains("java.util.Stack")
					|| thisClassName.contains("java.util.Hash")
					|| thisClassName.contains("java.util.Map")
					|| thisClassName.contains("java.util.Set")
					|| thisClassName.contains("java.util.Linked")
					|| thisClassName.contains("java.lang.Thread")) {
				isCollectionType = true;
			}
			// ���\�b�h����̕��A�̐ݒ�
			getInstance().thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
		}
	}
	
	public static synchronized void onlineTraceFieldGet(String fieldName, String thisClassName, String thisObjectId, 
			String containerClassName, String containerObjectId, String valueClassName, String valueObjectId,
			String threadId, String lineNum, long timeStamp) {
		getInstance().thread = getInstance().threads.get(threadId);
		// �t�B�[���h�A�N�Z�X�̐ݒ�
		if (getInstance().thread != null) getInstance().thread.fieldAccess(fieldName, valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, Integer.parseInt(lineNum), timeStamp);
	}
	
	public static synchronized void onlineTraceFieldSet(String fieldName, String containerClassName, String containerObjectId, 
			String valueClassName, String valueObjectId, String threadId, String lineNum, long timeStamp) {
		getInstance().thread = getInstance().threads.get(threadId);
		// �t�B�[���h�X�V�̐ݒ�
		if (getInstance().thread != null) getInstance().thread.fieldUpdate(fieldName, valueClassName, valueObjectId, containerClassName, containerObjectId, Integer.parseInt(lineNum), timeStamp);
	}
	
	public static synchronized void onlineTraceArrayCreate(String arrayClassName, String arrayObjectId, String dimension, 
			String threadId, String lineNum, long timeStamp) {
		getInstance().thread = getInstance().threads.get(threadId);
		if (getInstance().thread != null) getInstance().thread.arrayCreate(arrayClassName, arrayObjectId, Integer.parseInt(dimension), Integer.parseInt(lineNum), timeStamp);
	}
			
	public static synchronized void onlineTraceArraySet(String arrayClassName, String arrayObjectId, int index, 
			String valueClassName, String valueObjectId, String threadId, long timeStamp) {
		// �z��v�f�ւ̑��
		getInstance().thread = getInstance().threads.get(threadId);
		if (getInstance().thread != null) getInstance().thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
	}
	
	public static synchronized void onlineTraceArrayGet(String arrayClassName, String arrayObjectId, int index, 
			String valueClassName, String valueObjectId, String threadId, long timeStamp) {
		// �z��v�f�̎Q��
		getInstance().thread = getInstance().threads.get(threadId);
		if (getInstance().thread != null) getInstance().thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
	}

	public static synchronized void onlineTraceBlockEntry(String blockId, String incomings, 
			String threadId, String lineNum, long timeStamp) {
		// �u���b�N�̊J�n
		getInstance().thread = getInstance().threads.get(threadId);
		if (getInstance().thread != null) getInstance().thread.blockEnter(Integer.parseInt(blockId), Integer.parseInt(incomings), Integer.parseInt(lineNum), timeStamp);
	}

	public static ThreadInstance getThreadInstance(String threadId) {
		return getInstance().threads.get(threadId);
	}
	
	/**
	 * �w�肵���X���b�h��Ō��ݎ��s���̃��\�b�h���s���擾����(�I�����C����͗p)
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̃��\�b�h���s
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * �w�肵���X���b�h��Ō��ݎ��s���̃g���[�X�|�C���g���擾����(�I�����C����͗p)
	 * @param thread �ΏۃX���b�h
	 * @return thread ��Ō��ݎ��s���̎��s���̃g���[�X�|�C���g
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}

	/**
	 * �����œn�����R���e�i�����t�B�[���h�̍ŏI�X�V�ɑΉ�����FieldUpdate��Ԃ�
	 * @param containerObjId
	 * @param fieldName
	 * @param thread
	 * @return
	 */
	public static FieldUpdate getRecentlyFieldUpdate(String containerObjId, String fieldName, Thread thread) {
		TracePoint before = getCurrentTracePoint(thread);
		if (!before.isValid()) {
			before.stepBackOver();
			if (!before.isValid()) {
				return null; // �t�����T���ł���ȏ�H��Ȃ������ꍇ(root���\�b�h���s��1�s�ڂȂ�)�@���ꂪ�Ȃ��Ǝ��s����TimeoutException�ŗ�����
			}
		}
		TracePoint tp = getFieldUpdateTracePoint(containerObjId, fieldName, before);
		if (tp != null && tp.getStatement() instanceof FieldUpdate) {
			return (FieldUpdate)tp.getStatement();				
		}
		return null;
	}
	
//	private static TracePoint getRecentlyFieldUpdate(TracePoint tp) {
//		Statement statement = tp.getStatement();
//		if (statement instanceof FieldAccess) {
//			FieldAccess fa = (FieldAccess)statement;
//			return getFieldUpdateTracePoint(fa.getContainerObjId(), fa.getFieldName(), tp);
//		}
//		return null;
//	}

	/**
	 * �����Ŏw�肵���R���e�i�̎�����̃t�B�[���h���Ō�ɍX�V���ꂽstatement���t�����ɒT�����āA<br>
	 * ��������statement�ɑΉ�����TracePoint��Ԃ�
	 * @param containerObjId
	 * @param fieldName
	 * @param before
	 * @return
	 */
	public static TracePoint getFieldUpdateTracePoint(final String containerObjId, final String fieldName, TracePoint before) {		
		before = before.duplicate();
		before = getInstance().traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof FieldUpdate) {
					FieldUpdate fu = (FieldUpdate)statement;					
					if (fu.getContainerObjId().equals(containerObjId)
							&& fu.getFieldName().equals(fieldName)) {
						// �R���e�i�I�u�W�F�N�gID�ƃt�B�[���h�������Ɉ�v�����ꍇ
						return true;
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return before;			
		}
		return null;
	}
	
	/**
	 * �����œn�����z��̎w��C���f�b�N�X�̍ŏI�X�V�ɑΉ�����ArrayUpdate��Ԃ�
	 * @param arrayObjId
	 * @param index
	 * @param thread
	 * @return
	 */
	public static ArrayUpdate getRecentlyArrayUpdate(String arrayObjId, int index, Thread thread) {
		TracePoint before = getCurrentTracePoint(thread);
		if (!before.isValid()) {
			before.stepBackOver();
			if (!before.isValid()) {
				return null; // �t�����T���ł���ȏ�H��Ȃ������ꍇ(root���\�b�h���s��1�s�ڂȂ�)�@���ꂪ�Ȃ��Ǝ��s����TimeoutException�ŗ�����
			}
		}
		TracePoint tp = getArrayUpdateTracePoint(arrayObjId, index, before);
		if (tp != null && tp.getStatement() instanceof ArrayUpdate) {
			return (ArrayUpdate)tp.getStatement();
		}
		return null;		
	}
	
//	private static TracePoint getRecentlyArrayUpdate(TracePoint tp) {
//		Statement statement = tp.getStatement();
//		if (statement instanceof ArrayAccess) {
//			ArrayAccess aa = (ArrayAccess)statement;
//			return getArrayUpdateTracePoint(aa.getArrayObjectId(), aa.getIndex(), tp);
//		}
//		return null;
//	}

	/**
	 * �����Ŏw�肵���z��ŁA���w�肵���C���f�b�N�X���Ō�ɍX�V���ꂽstatement���t�����ɒT�����āA<br>
	 * ��������statement�ɑΉ�����TracePoint��Ԃ�
	 * @param arrayObjId
	 * @param index
	 * @param before
	 * @return
	 */
	public static TracePoint getArrayUpdateTracePoint(final String arrayObjId, final int index, TracePoint before) {		
		before = before.duplicate();
		before = getInstance().traverseStatementsInTraceBackward(new IStatementVisitor() {
			@Override
			public boolean preVisitStatement(Statement statement) {
				if (statement instanceof ArrayUpdate) {
					ArrayUpdate au = (ArrayUpdate)statement;
					if (au.getArrayObjectId().equals(arrayObjId)
							&& au.getIndex() == index) {
						// �z��ID�ƃC���f�b�N�X�����Ɉ�v�����ꍇ
						return true;
					}
				}
				return false;
			}
			@Override
			public boolean postVisitStatement(Statement statement) { return false; }
		}, before);
		if (before != null) {
			return before;			
		}
		return null;
	}
	
//	public static ArrayList<Alias> findAllStartAlias(MethodExecution me) {
//		ArrayList<Alias> startAliasList = new ArrayList<>();
//		List<Statement> statements = me.getStatements();
//		String[] primitives = {"byte", "short", "int", "long", "float", "double", "char", "boolean"};
//		List<String> primitiveList = Arrays.asList(primitives);
//		for (int i = 0; i < statements.size(); i++) {
//			TracePoint tp = me.getTracePoint(i);
//			Statement statement = statements.get(i);
//			if (statement instanceof FieldAccess) {
//				FieldAccess fa = (FieldAccess)statement;
//				String objId = fa.getContainerObjId();
//				if (objId != null && !(objId.equals("0")) && !(primitiveList.contains(fa.getContainerClassName()))) {
//					startAliasList.add(new Alias(objId, tp, Alias.OCCURRENCE_EXP_CONTAINER));
//				}
//				objId = fa.getValueObjId();
//				if (objId != null && !(objId.equals("0")) && !(primitiveList.contains(fa.getValueClassName()))) {
//					startAliasList.add(new Alias(objId, tp, Alias.OCCURRENCE_EXP_FIELD));
//				}
//			} else if (statement instanceof FieldUpdate) {
//				FieldUpdate fu = (FieldUpdate)statement;
//				String objId = fu.getContainerObjId();
//				if (objId != null && !(objId.equals("0")) && !(primitiveList.contains(fu.getContainerClassName()))) {
//					startAliasList.add(new Alias(objId, tp, Alias.OCCURRENCE_EXP_CONTAINER));
//				}
//				objId = fu.getValueObjId();
//				if (objId != null && !(objId.equals("0")) && !(primitiveList.contains(fu.getValueClassName()))) {
//					startAliasList.add(new Alias(objId, tp, Alias.OCCURRENCE_EXP_FIELD));
//				}
//			} else if (statement instanceof ArrayAccess) {
//				ArrayAccess aa = (ArrayAccess)statement;
//				String valueObjId = aa.getValueObjectId();
//				if (valueObjId != null && !(valueObjId.equals("0")) && !(primitiveList.contains(aa.getValueClassName()))) {
//					startAliasList.add(new Alias(valueObjId, tp, Alias.OCCURRENCE_EXP_ARRAY));
//				}				
//			} else if (statement instanceof ArrayUpdate) {
//				ArrayUpdate au = (ArrayUpdate)statement;
//				String valueObjId = au.getValueObjectId();
//				if (valueObjId != null && !(valueObjId.equals("0")) && !(primitiveList.contains(au.getValueClassName()))) {
//					startAliasList.add(new Alias(valueObjId, tp, Alias.OCCURRENCE_EXP_ARRAY));
//				}
//			} else if (statement instanceof ArrayCreate) {
//				ArrayCreate ac = (ArrayCreate)statement;
//				String arrayObjId = ac.getArrayObjectId();
//				if (arrayObjId != null && !(arrayObjId.equals("0")) && !(primitiveList.contains(ac.getArrayClassName()))) {
//					startAliasList.add(new Alias(arrayObjId, tp, Alias.OCCURRENCE_EXP_RETURN));
//				}
//			} else if (statement instanceof MethodInvocation) {
//				MethodExecution calledMe = ((MethodInvocation)statement).getCalledMethodExecution();
//				String thisObjId = calledMe.getThisObjId();
//				if (thisObjId != null && !(thisObjId.equals("0"))) {
//					startAliasList.add(new Alias(thisObjId, tp, Alias.OCCURRENCE_EXP_RECEIVER));
//				}
//				List<ObjectReference> args = calledMe.getArguments();
//				for (int j = 0; j < args.size(); j++) {
//					ObjectReference arg = args.get(j);
//					String argValueId = arg.getId();
//					if (argValueId != null && !(argValueId.equals("0")) && !(primitiveList.contains(arg.getActualType()))) {
//						startAliasList.add(new Alias(argValueId, tp, (j + Alias.OCCURRENCE_EXP_FIRST_ARG)));
//					}
//				}
//				ObjectReference returnValue = calledMe.getReturnValue();
//				if (returnValue != null) {
//					String returnValueId = returnValue.getId();
//					if (returnValueId != null && !(returnValueId.equals("0") && !(primitiveList.contains(returnValue.getActualType())))) {
//						startAliasList.add(new Alias(returnValueId, tp, Alias.OCCURRENCE_EXP_RETURN));
//					}
//				}
//			}
//		}
//		return startAliasList;
//	}
//
//	public static Alias getAlias(String objectId, TracePoint occurrencePoint, int occurrenceExp) {
//		return new Alias(objectId, occurrencePoint, occurrenceExp);
//	}
//	
//	public static ArrayList<ArrayList<Alias>> getObjectFlow(Alias startAlias) {
//		ArrayList<ArrayList<Alias>> aliasLists = new ArrayList<>();
//		ArrayList<Alias> aliasList = new ArrayList<>();
//		aliasLists.add(aliasList);
////		aliasList.add(alias);
//		String objId = startAlias.getObjectId();
//		TracePoint tp = startAlias.getOccurrencePoint().duplicate();
//		ArrayList<ArrayList<Alias>> resultLists = getObjectFlow(aliasLists, objId, tp, 0);
////		for (int i = 0; i < resultLists.size(); i++) {
////			ArrayList<Alias> resultList = resultLists.get(i);
////			System.out.println("---------------------------------------------------------");		// �m�F�p
////			for (Alias alias : resultList) System.out.println(alias);								// �m�F�p
////			int lastAliasOccurrenceEXP = resultList.get(resultList.size() - 1).getOccurrenceExp();
////			if (lastAliasOccurrenceEXP != Alias.OCCURRENCE_EXP_RETURN) {
////				resultLists.remove(resultList); // �����̃G�C���A�X���z�񐶐���R���X�g���N�^�Ăяo���ł͂Ȃ����X�g���폜����
////			}
////		}
//		return resultLists;
//	}
//
//	private static ArrayList<ArrayList<Alias>> getObjectFlow(ArrayList<ArrayList<Alias>> aliasLists, 
//			String objId, TracePoint tp, int side) {
//		ArrayList<Alias> aliasList = aliasLists.get(aliasLists.size() - 1); // ����getObjectFlow���\�b�h���s���Ō��������G�C���A�X�����Ă������X�g
//		do {
//			Statement statement = tp.getStatement();
//			if (statement instanceof FieldAccess) {
//				// �t�B�[���h�Q�Ƃ̏ꍇ
//				FieldAccess fa = (FieldAccess)statement;
//				if (fa.getValueObjId().equals(objId)) {
//					// ���Y�n�_�ł̃G�C���A�X�����X�g�ɒǉ��������, �t�B�[���h�ŏI�X�V�ɔ�ԃp�^�[���Ƃ��̂܂ܑk��p�^�[���Ƃŕ���
//					aliasList.add(new Alias(objId, tp.duplicate(), Alias.OCCURRENCE_EXP_FIELD));
//					aliasList = new ArrayList<>(aliasList); // ���X�g���̂��f�B�[�v�R�s�[���Ă���(�t�B�[���h�ŏI�X�V�ɔ�ԍċA�����I�����, ���̂܂ܑk��p�^�[���ŗp����)
//					TracePoint fieldUpdateTp = getRecentlyFieldUpdate(tp);
//					aliasLists = getObjectFlow(aliasLists, objId, fieldUpdateTp, 0);
//					aliasLists.add(aliasList); // �ċA�����ɓ���O�Ƀf�B�[�v�R�s�[���Ă������X�g���Ō���ɒǉ� (�ȍ~�̑k��ɂ���Č������G�C���A�X�͂��̃��X�g�ɓ������)
//				}
//			} else if (statement instanceof ArrayAccess) {
//				// �z��v�f�Q�Ƃ̏ꍇ
//				ArrayAccess aa = (ArrayAccess)statement;
//				if (aa.getValueObjectId().equals(objId)) {
//					aliasList.add(new Alias(objId, tp.duplicate(), Alias.OCCURRENCE_EXP_ARRAY));
//					aliasList = new ArrayList<>(aliasList);
//					TracePoint arrayUpdateTp = getRecentlyArrayUpdate(tp);
//					aliasLists = getObjectFlow(aliasLists, objId, arrayUpdateTp, 0);
//					aliasLists.add(aliasList);
//				}
//			} else if (statement instanceof ArrayCreate) {
//				// �z�񐶐��̏ꍇ
//				ArrayCreate ac = (ArrayCreate)statement;
//				if (ac.getArrayObjectId().equals(objId)) {
//					aliasList.add(new Alias(objId, tp.duplicate(), Alias.OCCURRENCE_EXP_RETURN)); // �z�񐶐��� new �^��[] �̖߂�l
//					return aliasLists; // �z�񐶐��ӏ��̓G�C���A�X�̋N���Ȃ̂ł���ȑO�ɂ͂����Ȃ��͂�
//				}
//			} else if (statement instanceof MethodInvocation) {
//				// ���\�b�h�Ăяo���̏ꍇ
//				MethodExecution calledMethodExecution = ((MethodInvocation)statement).getCalledMethodExecution();
//				ObjectReference returnValue = calledMethodExecution.getReturnValue();
//				if (returnValue.getId().equals(objId)) {
//					// �߂�l�ɃG�C���A�X�̃I�u�W�F�N�gID����v�����ꍇ
//					aliasList.add(new Alias(objId, tp.duplicate(), Alias.OCCURRENCE_EXP_RETURN));
//					if (calledMethodExecution.isConstructor()) {
//						return aliasLists; // �R���X�g���N�^�Ăяo���ӏ��̓G�C���A�X�̋N���Ȃ̂ł���ȑO�ɂ͂����Ȃ��͂�
//					}
//					TracePoint exitTp = calledMethodExecution.getExitPoint(); // �Ăяo�����\�b�h���s�̍ŏI�X�e�[�g�����g���w��tp���擾
//					aliasLists = getObjectFlow(aliasLists, objId, exitTp, side + 1); // �Ăяo����̃��\�b�h���s�ɐ���
//					aliasList = aliasLists.get(aliasLists.size() - 1);
//				}
//			}
//		} while (tp.stepBackOver()); // �Ăяo�����ɖ߂邩����ȏ�H��Ȃ��Ȃ�܂Ń��[�v
//		if (!tp.isValid()) {
//			return aliasLists; // ����ȏチ�\�b�h���s��k��Ȃ��ꍇ(main���\�b�h�̂���ɑO�Ȃ�)�͂��̎��_�ŏI��
//		}
//		// --- ���̎��_�� tracePoint�� �Ăяo�������w���Ă��� (���O�܂ők���Ă������\�b�h���s�ɂ��Ẵ��\�b�h�Ăяo�����w���Ă���) ---
//		MethodExecution calledMethodExecution = ((MethodInvocation)tp.getStatement()).getCalledMethodExecution();
//		ArrayList<ObjectReference> args = calledMethodExecution.getArguments();
//		for (int i = 0; i < args.size(); i++) {
//			if (args.get(i).getId().equals(objId)) {
//				// ���\�b�h�Ăяo���̎������ɃG�C���A�X�̃I�u�W�F�N�gID����v�����ꍇ
//				aliasList.add(new Alias(objId, tp.duplicate(), (i + Alias.OCCURRENCE_EXP_FIRST_ARG)));
//				if (side == 0) {
//					// �T���J�n���\�b�h���s�܂��̓t�B�[���h��z��v�f�̍ŏI�X�V�T���Ŕ�񂾐�̃��\�b�h���s����, �X�^�b�N�g���[�X�ł��ǂ��S���\�b�h���s�̏ꍇ
//					TracePoint previousTp = tp.duplicate();
//					previousTp.stepBackOver();
//					aliasLists = getObjectFlow(aliasLists, objId, previousTp, 0); // �Ăяo�����̃��\�b�h���s�ɖ߂�
//				}
//			}
//		}
//		return aliasLists;
//	}
//	
//	public static int countMethodExecutionInTraceCollector(List<MethodExecution> methodExecutions, String targetSignature, int count, String indent) {
//		if (methodExecutions == null || methodExecutions.isEmpty()) {
//			return count;
//		}
//		for (int i = 0; i < methodExecutions.size(); i++) {
//			MethodExecution me = methodExecutions.get(i);
//			String signature = me.getSignature();
////			System.out.println(indent + signature);
//			if (targetSignature.equals(signature)) {
//				count++;
//			}
//			List<MethodExecution> children = me.getChildren();
//			count = countMethodExecutionInTraceCollector(children, targetSignature, count, indent + "--------");
//		}
//		return count;
//	}
	
	public static void test() {
		System.out.println("Hello TraceJSON");
		System.out.println(getInstance().classes);
		System.out.println("Bye TraceJSON");
	}
}
