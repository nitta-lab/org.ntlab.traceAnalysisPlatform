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

public class TraceJSON extends Trace {
	private HashMap<String, ClassInfo> classes = new HashMap<>();
	private HashMap<String, Stack<String>> stacks = new HashMap<String, Stack<String>>();
	
	private TraceJSON() {
		
	}

	/**
	 * 指定したJSONのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param file トレースファイル
	 */
	public TraceJSON(BufferedReader file) {
		try {
			readJSON(file);
			file.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 指定したJSONのトレースファイルを解読して Trace オブジェクトを生成する
	 * @param traceFile トレースファイルのパス
	 */
	public TraceJSON(String traceFile) {
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
		// トレースファイル読み込み
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
			// トレースファイルの解析
			if (line.startsWith("{\"type\":\"classDef\"")) {
				// クラス定義
				type = line.split(",\"name\":\"");
				classNameData = type[1].split("\",\"path\":\"");
				className = classNameData[0];
				pathData = classNameData[1].split("\",\"loaderPath\":\"");
				classPath = pathData[0].substring(1);								// 先頭の / を取り除く
				loaderPath = pathData[1].substring(1, pathData[1].length() - 3);	// 先頭の / と、末尾の "}, を取り除く
				initializeClass(className, classPath, loaderPath);
			} else if (line.startsWith("{\"type\":\"methodCall\"")) {
				// メソッド呼び出しの呼び出し側
				type = line.split(",\"callerSideSignature\":\"");
				signature = type[1].split("\",\"threadId\":");
				threadId = signature[1].split(",\"lineNum\":");
				lineNum = Integer.parseInt(threadId[1].substring(0, threadId[1].length() - 2));	// 末尾の }, を取り除く
				thread = threads.get(threadId[0]);
				thread.preCallMethod(signature[0], lineNum);
			} else if (line.startsWith("{\"type\":\"methodEntry\"")) {
				// メソッド呼び出し
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
				time = threadId[1].substring(0, threadId[1].length() - 2);			// 末尾の }, を取り除く
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
				// メソッド呼び出しの設定
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// 引数の設定
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"constructorEntry\"")) {
				// コンストラクタ呼び出し
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
				time = threadId[1].substring(0, threadId[1].length() - 2);			// 末尾の }, を取り除く
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
				// メソッド呼び出しの設定
				thread.callMethod(signature[0], null, thisClassName, thisObjectId, isConstractor, isStatic, timeStamp);
				// 引数の設定
				thread.setArgments(parseArguments(arguments));
			} else if (line.startsWith("{\"type\":\"methodExit\"")) {
				// メソッドからの復帰
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く
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
					// メソッドからの復帰の設定
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"constructorExit\"")) {
				// コンストラクタからの復帰
				type = line.split(",\"shortSignature\":\"");
				signature = type[1].split("\",\"returnValue\":");
				returnValue = signature[1].split(",\"threadId\":");
				threadId = returnValue[1].split(",\"time\":");
				returnData = parseClassNameAndObjectId(returnValue[0]);
				thisClassName = returnClassName = returnData[0];
				thisObjectId = returnObjectId = returnData[1];
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く
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
					// メソッドからの復帰の設定
					thread.returnMethod(returnVal, thisObjectId, isCollectionType, timeStamp);
				}
			} else if (line.startsWith("{\"type\":\"fieldGet\"")) {
				// フィールドアクセス
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				// フィールドアクセスの設定
				if (thread != null) thread.fieldAccess(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, thisClassName, thisObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"fieldSet\"")) {
				// フィールド更新
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				// フィールド更新の設定
				if (thread != null) thread.fieldUpdate(fieldData[0], valueClassName, valueObjectId, containerClassName, containerObjectId, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayCreate\"")) {
				// 配列生成
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayCreate(arrayClassName, arrayObjectId, dimension, lineNum, timeStamp);
			} else if (line.startsWith("{\"type\":\"arraySet\"")) {
				// 配列要素への代入
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arraySet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"arrayGet\"")) {
				// 配列要素の参照
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
				time = threadId[1].substring(0, threadId[1].length() - 2);		// 末尾の }, を取り除く					
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.arrayGet(arrayClassName, arrayObjectId, index, valueClassName, valueObjectId, 0, timeStamp);
			} else if (line.startsWith("{\"type\":\"blockEntry\"")) {
				// ブロックの開始
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
				time = lineData[1].substring(0, lineData[1].length() - 2);		// 末尾の }, を取り除く
				timeStamp = Long.parseLong(time);
				if (thread != null) thread.blockEnter(blockId, incomings, lineNum, timeStamp);
			}
		}
	}

	/**
	 * クラス名とオブジェクトIDを表すJSONオブジェクトを解読する
	 * @param classNameAndObjectIdJSON トレースファイル内のJSONオブジェクト
	 * @return
	 */
	protected String[] parseClassNameAndObjectId(String classNameAndObjectIdJSON) {
		// 先頭の {"class":" の10文字と末尾の } を取り除いて分離
		return classNameAndObjectIdJSON.substring(10, classNameAndObjectIdJSON.length() - 1).split("\",\"id\":");
	}
	
	/**
	 * 引数を表すJSON配列を解読する
	 * @param arguments
	 * @return
	 */
	protected ArrayList<ObjectReference> parseArguments(String[] arguments) {
		String[] argData;
		argData = arguments[0].substring(1, arguments[0].length() - 1).split(",");		// 先頭の [ と末尾の ] を取り除く
		ArrayList<ObjectReference> argumentsData = new ArrayList<ObjectReference>();
		for (int k = 0; k < argData.length - 1; k += 2) {
			argumentsData.add(new ObjectReference(argData[k+1].substring(5, argData[k+1].length() - 1), argData[k].substring(10, argData[k].length() - 1)));
		}
		return argumentsData;
	}

	public static void initializeClass(String name, String path, String loaderPath) {
		getInstance().classes.put(name, new ClassInfo(name, path, loaderPath));
	}

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
	 * 実行された全ブロックを取得する
	 * @return 全ブロック(メソッド名:ブロックID)
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
	 * マーク内で実行が開始されたブロックを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するブロック(メソッド名:ブロックID)
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
					if (methodExecution.getExitTime() < markStart) return true;		// 探索終了
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
	 * 実行された全フローを取得する
	 * @return 全フロー(メソッド名:フロー元ブロックID:フロー先ブロックID)
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
	 * マーク内で実行されたフローを取得する
	 * @param markStart マークの開始時刻
	 * @param markEnd マークの終了時刻
	 * @return 該当するフロー(メソッド名:フロー元ブロックID:フロー先ブロックID)
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
					if (methodExecution.getExitTime() < markStart) return true;		// 探索終了
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
	
	public static HashMap<String, Stack<String>> getStacks() {
		return getInstance().stacks;
	}
	
	public static ThreadInstance getThreadInstance(String threadId) {
		return getInstance().threads.get(threadId);
	}
	
	/**
	 * 指定したスレッド上で現在実行中のメソッド実行を取得する
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中のメソッド実行
	 */
	public static MethodExecution getCurrentMethodExecution(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentMethodExecution();
	}
	
	/**
	 * 指定したスレッド上で現在実行中のトレースポイントを取得する
	 * @param thread 対象スレッド
	 * @return thread 上で現在実行中の実行文のトレースポイント
	 */
	public static TracePoint getCurrentTracePoint(Thread thread) {
		ThreadInstance t = getInstance().threads.get(String.valueOf(thread.getId()));
		return t.getCurrentTracePoint();
	}

	/**
	 * 引数で渡したコンテナが持つフィールドの最終更新に対応するFieldUpdateを返す
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
				return null; // 逆方向探索でそれ以上辿れなかった場合(rootメソッド実行の1行目など)　これがないと実行時にTimeoutExceptionで落ちる
			}
		}
		TracePoint tp = getFieldUpdateTracePoint(containerObjId, fieldName, before);
		if (tp != null && tp.getStatement() instanceof FieldUpdate) {
			return (FieldUpdate)tp.getStatement();				
		}
		return null;
	}

	/**
	 * 引数で指定したコンテナの持つ特定のフィールドが最後に更新されたstatementを逆方向に探索して、<br>
	 * 見つかったstatementに対応するTracePointを返す
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
						// コンテナオブジェクトIDとフィールド名が共に一致した場合
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
	 * 引数で渡した配列の指定インデックスの最終更新に対応するArrayUpdateを返す
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
				return null; // 逆方向探索でそれ以上辿れなかった場合(rootメソッド実行の1行目など)　これがないと実行時にTimeoutExceptionで落ちる
			}
		}
		TracePoint tp = getArrayUpdateTracePoint(arrayObjId, index, before);
		if (tp != null && tp.getStatement() instanceof ArrayUpdate) {
			return (ArrayUpdate)tp.getStatement();
		}
		return null;		
	}

	/**
	 * 引数で指定した配列で、かつ指定したインデックスが最後に更新されたstatementを逆方向に探索して、<br>
	 * 見つかったstatementに対応するTracePointを返す
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
						// 配列IDとインデックスが共に一致した場合
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
}