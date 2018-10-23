package org.ntlab.traceAnalysisPlatform.tracer.trace;

import java.util.ArrayList;

/**
 * �I�u�W�F�N�g�̎Q�Ə��(�G�C���A�X)��\���N���X
 * @author Isitani
 *
 */
public class Alias {
	private String objectId;
	private TracePoint occurrencePoint; // ���Y�I�u�W�F�N�g�̎Q�Ƃ��s���Ă�����s�ӏ��ɑΉ�����TracePoint
	/**
	 * ���Y�I�u�W�F�N�g�̎Q�Ƃ�TracePoint�ɂ����Ăǂ��Ɍ���Ă��邩��\��<br>
	 * 0				�t�B�[���h�A�N�Z�X���̃R���e�i �������� ���\�b�h�Ăяo�����̃��V�[�o<br>
	 * 1, 2, 3 �c�c n		�t�B�[���h�A�N�Z�X���̃t�B�[���h(1) �������� ���\�b�h�Ăяo������n�Ԗڂ̎�����  (1���珇�Ԃ�)<br>
	 * -1				���\�b�h�Ăяo�����̖߂�l<br>
	 * <br>
	 * ��1: d = a.m(b, c);<br>
	 * <br>
	 * ��1�̎��s���ɂ�����, a�̓��\�b�h�Ăяo���̃��V�[�o�Ȃ̂�0, b�̓��\�b�h�Ăяo����1�Ԗڂ̎������Ȃ̂�1,<br>
	 * c�̓��\�b�h�Ăяo����2�Ԗڂ̎������Ȃ̂�2, a.m(b, c)�̖߂�l��-1 �ƂȂ�.<br>
	 * <br>
	 * ��2: d = a.f;<br>
	 * ��2�̎��s���ɂ�����, a�̓t�B�[���h�̃R���e�i�Ȃ̂�0, b�̓t�B�[���h�Ȃ̂�1 �ƂȂ�.
	 * 
	 */
	private int occurrenceExp;
	public static final int OCCURRENCE_EXP_CONTAINER = 0;
	public static final int OCCURRENCE_EXP_RECEIVER = 0;
	public static final int OCCURRENCE_EXP_FIELD = 1;
	public static final int OCCURRENCE_EXP_ARRAY = 1;
	public static final int OCCURRENCE_EXP_FIRST_ARG = 1;
	public static final int OCCURRENCE_EXP_RETURN = -1;

	public Alias(String objectId, TracePoint occurrencePoint, int occurrenceExp) {
		this.objectId = objectId;
		this.occurrencePoint = occurrencePoint;
		this.occurrenceExp = occurrenceExp;
	}
	
	public String getObjectId() {
		return objectId;
	}

	public TracePoint getOccurrencePoint() {
		return occurrencePoint;
	}

	public int getOccurrenceExp() {
		return occurrenceExp;
	}
	
	public MethodExecution getMethodExecution() {
		return occurrencePoint.getMethodExecution();
	}
	
	public String getMethodExecutionClassName() {
		MethodExecution methodExecution = occurrencePoint.getMethodExecution();
		return Trace.getDeclaringType(methodExecution.getSignature(), methodExecution.isConstructor());
	}

	public String getMethodSignature() {
		return occurrencePoint.getMethodExecution().getCallerSideSignature();
	}
	
	public int getLineNo() {
		Statement statement = occurrencePoint.getStatement();
		return statement.getLineNo();
	}

	public String getStatementType() {
		Statement statement = occurrencePoint.getStatement();
		String statementType = "";
		if (statement instanceof FieldAccess) {
			FieldAccess fa = (FieldAccess)statement;
			statementType = "FieldAccess";
		} else if (statement instanceof FieldUpdate) {
			FieldUpdate fu = (FieldUpdate)statement;
			statementType = "FieldUpdate";
		} else if (statement instanceof ArrayAccess) {
			ArrayAccess aa = (ArrayAccess)statement;
			statementType = "ArrayAccess";
		} else if (statement instanceof ArrayUpdate) {
			ArrayUpdate au = (ArrayUpdate)statement;
			statementType = "ArrayUpdate";
		} else if (statement instanceof ArrayCreate) {
			ArrayCreate ac = (ArrayCreate)statement;
			statementType = "ArrayCreate";
		} else if (statement instanceof MethodInvocation) {
			MethodExecution me = ((MethodInvocation)statement).getCalledMethodExecution();
			statementType = "MethodInvocation";
		}
		return statementType;
	}
	
	public String getStatementSignature() {
		Statement statement = occurrencePoint.getStatement();
		String statementSignature = "";
		if (statement instanceof FieldAccess) {
			FieldAccess fa = (FieldAccess)statement;
			statementSignature = fa.getFieldName();
		} else if (statement instanceof FieldUpdate) {
			FieldUpdate fu = (FieldUpdate)statement;
			statementSignature = fu.getFieldName();
		} else if (statement instanceof ArrayAccess) {
			ArrayAccess aa = (ArrayAccess)statement;
			statementSignature = aa.getArrayClassName() + "[" + aa.getIndex() + "]";
		} else if (statement instanceof ArrayUpdate) {
			ArrayUpdate au = (ArrayUpdate)statement;
			statementSignature = au.getArrayClassName() + "[" + au.getIndex() + "]";
		} else if (statement instanceof ArrayCreate) {
			ArrayCreate ac = (ArrayCreate)statement;
			statementSignature = ac.getArrayClassName();
		} else if (statement instanceof MethodInvocation) {
			MethodExecution me = ((MethodInvocation)statement).getCalledMethodExecution();
			statementSignature = me.getCallerSideSignature();
		}
		return statementSignature;		
	}
	
	public String getClassName() {
		Statement statement = occurrencePoint.getStatement();
		String className = "";
		if (statement instanceof FieldAccess) {
			if (occurrenceExp == OCCURRENCE_EXP_CONTAINER) {
				className = ((FieldAccess) statement).getContainerClassName();
			} else if (occurrenceExp == OCCURRENCE_EXP_FIELD) {
				className = ((FieldAccess) statement).getValueClassName();				
			}
		} else if (statement instanceof FieldUpdate) {
			if (occurrenceExp == OCCURRENCE_EXP_CONTAINER) {
				className = ((FieldUpdate) statement).getContainerClassName();
			} else if (occurrenceExp == OCCURRENCE_EXP_FIELD) {
				className = ((FieldUpdate) statement).getValueClassName();				
			}
		} else if (statement instanceof ArrayAccess) {
			className = ((ArrayAccess) statement).getValueClassName();
		} else if (statement instanceof ArrayUpdate) {
			className = ((ArrayUpdate) statement).getValueClassName();
		} else if (statement instanceof ArrayCreate) {
			className = ((ArrayCreate) statement).getArrayClassName();
		} else if (statement instanceof MethodInvocation) {
			MethodExecution me = ((MethodInvocation)statement).getCalledMethodExecution();
			if (occurrenceExp == OCCURRENCE_EXP_RETURN) {
				className = me.getReturnValue().getActualType();
			} else if (occurrenceExp == OCCURRENCE_EXP_RECEIVER) {
				className = me.getThisClassName();
			} else {
				int index = occurrenceExp - OCCURRENCE_EXP_FIRST_ARG;
				ArrayList<ObjectReference> args = me.getArguments();
				if (index >= 0 && index < args.size()) {
					className = me.getArguments().get(index).getActualType();	
				}
			}
		}
		return className;
	}
	
	public String getOccurrenceText() {		
		String statementType = getStatementType();
		switch (statementType) {
		case "FieldAccess":
		case "FieldUpdate":
			return (occurrenceExp == 0) ? "container" : "field";
		case "ArrayAccess":
		case "ArrayUpdate":
			return (occurrenceExp == 0) ? "arrayObject" : "arrayValue";
		case "ArrayCreate":
			return "return";
		case "MethodInvocation":
			if (occurrenceExp <= 0) {
				return (occurrenceExp == 0) ? "receiver" : "return";
			}
			final String[] ORDER_TEXT = {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th"}; // 0-13�ɑΉ�
			final String ARG = " arg";
			if (occurrenceExp % 100 >= ORDER_TEXT.length) {
				return occurrenceExp + ORDER_TEXT[occurrenceExp % 10] + ARG; // ��2����14�ȏ�Ȃ�, ��1���̐����ɑΉ�������
			} else if (occurrenceExp % 100 >= 0) {
				return occurrenceExp + ORDER_TEXT[occurrenceExp % 100] + ARG; // ��2����0�ȏ�13�ȉ��Ȃ�, ��2���̐����ɑΉ�������
			}
		}
		return String.valueOf(occurrenceExp);
	}

	@Override
	public String toString() {
		Statement statement = occurrencePoint.getStatement();
		String className = getClassName();
		String methodSignature = getMethodSignature();
		String statementType = getStatementType();
		String statementSigunarure = getStatementSignature();
		String indent = "  ";
		StringBuilder str = new StringBuilder();
		str.append("objId: " + objectId + " (class = " + className + ")" + "\n");
		str.append("tp: " + occurrencePoint + "\n");
		str.append(indent + "signature: " + methodSignature + "\n");
		str.append(indent + "lineNo: " + statement.getLineNo() + "\n");
		str.append(indent + "statementType: " + statementType + " -> " + statementSigunarure + "\n");
		str.append("occurrenceExp: " + occurrenceExp + "\n");
		return str.toString();
	}
}
