package org.ntlab.traceAnalysisPlatform.tracer.trace;

/**
 * An object link within a trace
 * @author Nitta
 *
 */
public class Reference {
	private String id;
	private ObjectReference srcObj;			// the referring object
	private ObjectReference dstObj;			// the referred object
	private boolean bCreation = false;		// whether the referred object was created by the referring object?
	private boolean bArray = false;			// whether the referring object is an array and the referred object is its element?
	private boolean bCollection = false;	// whether the referring object is a correction and the referred object is its element?
	private boolean bFinalLocal = false;	// whether the referring object is an instance of an inner or anonymous class and the referred object is referred to by a final local variable of its enclosing object?

	public Reference(String srcObjectId, String dstObjectId, String srcClassName,
			String dstClassName) {
		srcObj = new ObjectReference(srcObjectId, srcClassName);
		dstObj = new ObjectReference(dstObjectId, dstClassName);
	}
	
	public Reference(ObjectReference srcObj, ObjectReference dstObj) {
		this.srcObj = srcObj;
		this.dstObj = dstObj;
	}
	
	public ObjectReference getSrcObject() {
		return srcObj;
	}
	
	public ObjectReference getDstObject() {
		return dstObj;
	}

	public void setSrcClassName(String srcClassName) {
		this.srcObj.setActualType(srcClassName);
	}

	public void setDstClassName(String dstClassName) {
		this.dstObj.setActualType(dstClassName);
	}

	public void setSrcObjectId(String srcObjectId) {
		this.srcObj.setId(srcObjectId);
	}

	public void setDstObjectId(String dstObjectId) {
		this.dstObj.setId(dstObjectId);
	}

	public String getSrcClassName() {
		return srcObj.getActualType();
	}

	public String getDstClassName() {
		return dstObj.getActualType();
	}

	public String getSrcObjectId() {
		return srcObj.getId();
	}

	public String getDstObjectId() {
		return dstObj.getId();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}	

	public void setCreation(boolean bCreation) {
		this.bCreation  = bCreation;
	}
	
	public boolean isCreation(){
		return bCreation;
	}

	public void setArray(boolean bArray) {
		this.bArray = bArray;
	}
	
	public boolean isArray() {
		return bArray;
	}
	
	public void setCollection(boolean bCollection) {
		this.bCollection = bCollection;
	}
	
	public boolean isCollection() {
		return bCollection;
	}
	
	public void setFinalLocal(boolean bFinalLocal) {
		this.bFinalLocal = bFinalLocal;
	}

	public boolean isFinalLocal() {
		return bFinalLocal;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Reference) {
			if (this.hashCode() != o.hashCode()) return false;
			return (this.srcObj.getId().equals(((Reference)o).srcObj.getId()) && this.dstObj.getId().equals(((Reference)o).dstObj.getId()));
		}
		return false;
	}
	
	public int hashCode() {
		return Integer.parseInt(srcObj.getId()) + Integer.parseInt(dstObj.getId());
	}
	
	public String toString() {
		return srcObj.getId() + "(" + srcObj.getActualType() + ")" + "->" + dstObj.getId() + "(" + dstObj.getActualType() + ")";
	}
}
