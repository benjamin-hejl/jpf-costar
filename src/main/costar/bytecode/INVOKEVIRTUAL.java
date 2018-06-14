package costar.bytecode;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import costar.CoStarMethodExplorer;
import costar.constrainsts.CoStarConstrainstTree;
import costar.constrainsts.CoStarNode;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import starlib.formula.Formula;
import starlib.formula.Utilities;
import starlib.formula.Variable;
import starlib.formula.expression.Comparator;
import starlib.formula.expression.Expression;
import starlib.formula.expression.LiteralExpression;
import starlib.formula.expression.NullExpression;

public class INVOKEVIRTUAL extends gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL {

	public INVOKEVIRTUAL(String clsDescriptor, String methodName, String signature) {
		super(clsDescriptor, methodName, signature);
	}
	
	public Instruction execute(ThreadInfo ti) {
		CoStarMethodExplorer analysis = CoStarMethodExplorer.getCurrentAnalysis(ti);
		
		if (analysis == null)
			return super.execute(ti);
		
		Object[] syms = getArgumentAttrs(ti);
		Object[] vals = getArgumentValues(ti);
		
		Instruction nextIns = super.execute(ti);
		
		StackFrame sf = ti.getTopFrame();
		
		Map<LocalVarInfo,String> newMap = new HashMap<LocalVarInfo,String>();
		Stack<Map<LocalVarInfo,String>> indexMap = analysis.getNameMap();
		indexMap.push(newMap);
		
		CoStarConstrainstTree tree = analysis.getConstrainstTree();
		CoStarNode current = tree.getCurrent();
		
		Formula formula = current.formula;
		
		MethodInfo mi = sf.getMethodInfo();
		
		LocalVarInfo[] lvis = mi.getLocalVars();
		
		// index 0 is "this"
		if (syms[0] == null) {
			newMap.put(lvis[0], "this");
		} else {
			newMap.put(lvis[0], syms[0].toString());
		}
		
		for (int i = 1; i < sf.getMethodInfo().getArgumentsSize(); i++) {
			Expression exp = null;
			Object sym_v = syms[i];
			
			if (sym_v != null) {
				if (sym_v instanceof gov.nasa.jpf.constraints.api.Expression<?>) {
					String name = ((gov.nasa.jpf.constraints.api.Expression<?>) sym_v).toString(0);
					exp = new Variable(name);
				} else {
					exp = (Expression) sym_v;
				}
			} else if (vals[i - 1] != null) {
				exp = new LiteralExpression(vals[i - 1] + "");
			} else {
				exp = NullExpression.getInstance();
			}
			
			String name = lvis[i].getName() + "_" + Utilities.freshIndex();
			newMap.put(lvis[i], name);
			
			Variable var = new Variable(name);
			// eq or assign ?
			formula.addComparisonTerm(Comparator.EQ, var, exp);
		}
		
		return nextIns;
	}
	
}
