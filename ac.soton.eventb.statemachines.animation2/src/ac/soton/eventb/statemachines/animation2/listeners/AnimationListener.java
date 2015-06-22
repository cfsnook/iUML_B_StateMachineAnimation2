/**
 * Copyright (c) 2015 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.TranslationKind;
import ac.soton.eventb.statemachines.animation2.DiagramAnimator;
import de.prob.core.IAnimationListener;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;
/**
 * Diagram animation listener.
 * 
 * @author vitaly
 *
 */
public class AnimationListener implements IAnimationListener {

	@Override
	public void currentStateChanged(State currentState, Operation operation) {
		DiagramAnimator animator = DiagramAnimator.getAnimator();
		
		if (animator.isRunning()) {
			//TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(machine.eResource()); //getEditingDomain(machine);
			for(Statemachine statemachine : animator.getRootStatemachines()){	
				TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(statemachine);
				Map<String, Variable> variables = currentState.getValues();
				List<Operation> operations = currentState.getEnabledOperations();
				Map<String, EList<Operation>> enabledOperations = new HashMap<String, EList<Operation>>();

				CompoundCommand cc = new CompoundCommand();
	
				// retrieve information from proB
				// map of operation names and executable ProB operations
				for (Operation op : operations) {
					if (!enabledOperations.containsKey(op.getName()))
						enabledOperations.put(op.getName(), new BasicEList<Operation>());
					enabledOperations.get(op.getName()).add(op);
				}
				// map of active states - with instances currently in that state if lifted, or just TRUE if not lifted
				Map<String, Object> activeStates = getActiveStates(animator,statemachine, variables);
				
				boolean lifted = statemachine.getInstances()!=null;
				// update states
				for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
					if (object == null) continue;
					String name = ((ac.soton.eventb.statemachines.State) object).getName();	
					EList<String> ins = new BasicEList<String>();					
					if (lifted){
						if (activeStates.get(name) instanceof String){
							ins.add((String)activeStates.get(name));
						}else{
							ins.add("\u2205");	// this may be already done by getActiveStates() but if not default to empty
						}
					}
					boolean active = activeStates.containsKey(name) && !"FALSE".equals(activeStates.get(name)) && !"\u2205".equals(activeStates.get(name));
					cc.append(SetCommand.create(editingDomain, object, StatemachinesPackage.Literals.STATE__ACTIVE_INSTANCES, ins));
					cc.append(SetCommand.create(editingDomain, object, StatemachinesPackage.Literals.STATE__ACTIVE, active));					
				}

				//update transitions so we know which are active below
				for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
					if (object == null) continue;
					Transition transition = (Transition) object;
					// collect enabled operations
					EList<Operation> ops = new BasicEList<Operation>();
					for (Event event : transition.getElaborates()) {
						if (enabledOperations.containsKey(event.getName()))
							ops.addAll(enabledOperations.get(event.getName()));
					}
					// set operations
					cc.append(SetCommand.create(editingDomain, transition, StatemachinesPackage.Literals.TRANSITION__OPERATIONS, ops));
				}	

				editingDomain.getCommandStack().execute(cc);			
			}	
		}
	}
	
	/**
	 * @param animator
	 * @param statemachine
	 * @param variables
	 * @param type
	 * @return
	 */
	private Map<String,Object> getActiveStates(DiagramAnimator animator, Statemachine statemachine, Map<String, Variable> variables) {
		//retrieve a map of active states to instances for all state-machine states	
		Map<String,Object> activeStates = new HashMap<String,Object>();
		TranslationKind type = statemachine.getTranslation();
		boolean lifted = statemachine.getInstances()!=null; 
		
		if (type == TranslationKind.SINGLEVAR) {
			// add state of root statemachine
			if (variables.containsKey(statemachine.getName())){
				String smValue = variables.get(statemachine.getName()).getValue();
				if (lifted){
					activeStates.putAll(parseSmFn(smValue));					
				} else {
					activeStates.put(smValue, "TRUE");
				}
			}
			// add states of all nested statemachines
			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
				if (object == null) continue;
				String statemachineName = ((EventBNamed) object).getName();
				if (variables.containsKey(statemachineName)) {
					String smValue = variables.get(statemachineName).getValue();
					if (lifted){
						activeStates.putAll(parseSmFn(smValue));					
					} else {
						activeStates.put(smValue, "TRUE");
					}
				}
			}
		}
		
		//FIXME: REFINEDVAR is no longer being supported - may remove at some time
		else if (type == TranslationKind.REFINEDVAR) {
			//find refinement level of this machine
			Machine m = animator.getMachine();
			int refinementLevel = 0;
			while(m.getRefines().size() != 0){
				m = m.getRefines().get(0);
				refinementLevel++;
			}
			// add state of root statemachine
			if (variables.containsKey((statemachine.getName() + "_" + refinementLevel))){
				for(Variable variable : variables.values()){
					if(variable.getValue().equals(variables.get(statemachine.getName() + "_" + refinementLevel).getValue()) && !variable.getIdentifier().equals(statemachine.getName() + "_" + refinementLevel))
						activeStates.put(variable.getIdentifier(), "TRUE");
				}
			}
			// add states of all nested statemachines
			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
				if (object == null) continue;
				String statemachineName = ((EventBNamed) object).getName() + "_" + refinementLevel;
				if (variables.containsKey(statemachine))
					activeStates.put(variables.get(statemachineName).getValue(), "TRUE");
			}
			
		} else if (type == TranslationKind.MULTIVAR) {
			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
				if (object == null) continue;
				String stateName = ((ac.soton.eventb.statemachines.State) object).getName();	
				Variable stateStatusVar = variables.get(stateName);
				if (stateStatusVar != null){
					activeStates.put(stateName, stateStatusVar.getValue());					
				}
			}
		} else {
			//un-supported translation kind - do nothing
		}
		return activeStates;
	}

	/**
	 * this parses the input string to convert the string value of a function
	 * into a map from state names to a string representation of the set of instances in that state
	 * 
	 * ASSUMES that the state part is a simple string (i.e. contains no maplet).
	 * (the instance could be any type including maplets etc.)
	 * 
	 * @param smValue
	 * @return
	 */
	private Map<String, String> parseSmFn(String smValue) {
		Map<String,String> ret = new HashMap<String,String>();
		if (smValue.startsWith("{")) smValue = smValue.substring(1);
		if (smValue.startsWith("}",smValue.length()-1)) smValue = smValue.substring(0,smValue.length()-1);		
		String[] result = smValue.split(",");
	     for (int x=0; x<result.length; x++){
	 		if (result[x].startsWith("(")) result[x] = result[x].substring(1);
			if (result[x].startsWith(")",result[x].length()-1)) result[x] = result[x].substring(0,result[x].length()-1);
			int i = result[x].lastIndexOf("\u21a6");
			if (i<0) continue;
	    	String ins = result[x].substring(0,i);
	    	String stateName = result[x].substring(i+1);
	    	String instances = ret.get(stateName);
	    	if (instances==null){
	    		instances = "{"+ins+"}";
	    	}else{
	    		instances = instances.substring(0, instances.length()-1);
	    		instances = instances+","+ins+"}";
	    	}
	    	ret.put(stateName, instances);
	     }
		return ret;
	}
}

