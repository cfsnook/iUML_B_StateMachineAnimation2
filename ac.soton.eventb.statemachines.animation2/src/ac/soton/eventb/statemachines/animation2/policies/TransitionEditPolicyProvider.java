/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.policies;

import java.util.List;

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

import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.animation2.DiagramAnimator;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionEditPart;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionGhostEditPart;

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
				DiagramAnimator animator = DiagramAnimator.getAnimator();

				// if animation running and operations available
				if (animator.isRunning()) {
					getHost().getViewer().deselectAll(); 	// deselect the transition ready for next interaction
					
					List<?> enabled = transition.getOperations();
					if (enabled.isEmpty())
						return;
					
					// show selection menu
					PopupMenu menu = new PopupMenu(enabled, new LabelProvider() {

						@Override
						public String getText(Object element) {
							de.prob.statespace.Transition transition = (de.prob.statespace.Transition) element;
							List<String> params = transition.getParameterPredicates();
							String text = transition.getName() +
								(params == null || params.isEmpty() ? "" : " " + params.toString());
							return text;
						}});
					menu.show(getHost().getViewer().getControl());
					de.prob.statespace.Transition op = (de.prob.statespace.Transition) menu.getResult();
					
					// execute selected
					if (op != null)
						animator.animate(op);
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
