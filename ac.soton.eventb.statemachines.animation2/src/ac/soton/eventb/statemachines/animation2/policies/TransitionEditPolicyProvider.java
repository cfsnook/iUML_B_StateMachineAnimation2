/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.policies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.eclipse.gmf.runtime.common.core.service.IOperation;
import org.eclipse.gmf.runtime.common.core.service.IProviderChangeListener;
import org.eclipse.gmf.runtime.diagram.ui.menus.PopupMenu;
import org.eclipse.gmf.runtime.diagram.ui.services.editpolicy.CreateEditPoliciesOperation;
import org.eclipse.gmf.runtime.diagram.ui.services.editpolicy.IEditPolicyProvider;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.viewers.LabelProvider;
import org.eventb.emf.core.machine.Event;

import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.animation2.StatemachineAnimationPlugin;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionEditPart;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionGhostEditPart;
import de.prob.core.Animator;
import de.prob.core.command.ExecuteOperationCommand;
import de.prob.core.domainobjects.Operation;
import de.prob.exceptions.ProBException;

/**
 * Edit policy that adds animation context menu to enabled transitions.
 * 
 * @author vitaly
 *
 */
public class TransitionEditPolicyProvider implements IEditPolicyProvider {

	@Override
	public void createEditPolicies(EditPart editPart) {
		editPart.installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
			
			@Override
			protected void showSelection() {
				Transition transition = (Transition) ((View) getHost().getModel()).getElement();
				Animator animator = Animator.getAnimator();

				//FIXME: more elaborate check required to test if concrete diagram is animated
				// if animation running and operations available
				if (animator.isRunning()
						&& transition.getOperations() != null 
						&& !transition.getOperations().isEmpty()) {
					
					getHost().getViewer().deselectAll(); 	//deselect the transition ready for next interaction
					
					List<Operation> enabledOperations = animator.getCurrentState().getEnabledOperations();
					List<Operation> operations = new ArrayList<Operation>();
					EList<Event> events = transition.getElaborates();
//					EList<?> operations = transition.getOperations();
					for (Operation op : enabledOperations){
						String opName = op.getName();
						for (Event ev : events){
							if (opName.equals(ev.getName()) ){
								operations.add(op);
							}	
						}
					}
					// show selection menu
					PopupMenu menu = new PopupMenu(operations, new LabelProvider() {

						@Override
						public String getText(Object element) {
							Operation operation = (Operation) element;
							List<String> arguments = operation.getArguments();
							String text = operation.getName() +
								(arguments == null || arguments.isEmpty() ? "" : " " + arguments.toString());
							return text;
						}});
					menu.show(getHost().getViewer().getControl());
					Object operation = menu.getResult();
					
					// execute selected
					if (operation != null) {
						try {
							ExecuteOperationCommand.executeOperation(animator, (Operation) operation);
						} catch (ProBException e) {
									StatemachineAnimationPlugin.getDefault().getLog().log(
											new Status(IStatus.ERROR,
													StatemachineAnimationPlugin.PLUGIN_ID,
													"Execution of ProB operation failed for: " + operation.toString(),
													e));
						}
					}
				}
			}
			
			@Override
			protected void hideSelection() {
			}
		});
	}

	@Override
	public boolean provides(IOperation operation) {
		if (operation instanceof CreateEditPoliciesOperation) {
			EditPart editPart = ((CreateEditPoliciesOperation) operation).getEditPart();
			if (editPart instanceof TransitionEditPart ||
					editPart instanceof TransitionGhostEditPart)
				return true;
		}
		return false;
	}

	@Override
	public void addProviderChangeListener(IProviderChangeListener listener) {
	}

	@Override
	public void removeProviderChangeListener(IProviderChangeListener listener) {
	}

}
