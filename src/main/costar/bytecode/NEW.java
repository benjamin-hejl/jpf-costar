package costar.bytecode;

import costar.CoStarMethodExplorer;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import starlib.formula.Utilities;
import starlib.formula.Variable;
import starlib.formula.expression.Expression;

public class NEW extends gov.nasa.jpf.jvm.bytecode.NEW {

	public NEW(String clsDescriptor) {
		super(clsDescriptor);
	}

	@Override
	public Instruction execute(ThreadInfo ti) {
		CoStarMethodExplorer analysis = CoStarMethodExplorer.getCurrentAnalysis(ti);

		if (analysis == null)
			return super.execute(ti);

		Heap heap = ti.getHeap();
		ClassInfo ci;

		// resolve the referenced class
		try {
			ci = ti.resolveReferencedClass(cname);
		} catch (LoadOnJPFRequired lre) {
			return ti.getPC();
		}

		if (ci.initializeClass(ti)) {
			// continue with the topframe and re-exec this insn once the clinits
			// are done
			return ti.getPC();
		}

		if (heap.isOutOfMemory()) { // simulate OutOfMemoryError
			return ti.createAndThrowException("java.lang.OutOfMemoryError", "trying to allocate new " + cname);
		}

		Variable newNode = Utilities.freshVar(new Variable("newNode"));

		Expression sym_v = new Variable(newNode);

		ElementInfo ei = heap.newObject(ci, ti);
		ei.setObjectAttr(sym_v);

		int objRef = ei.getObjectRef();
		newObjRef = objRef;

		// pushes the return value onto the stack
		StackFrame sf = ti.getModifiableTopFrame();
		sf.pushRef(objRef);
		sf.setOperandAttr(sym_v);

		return getNext(ti);
	}

}
