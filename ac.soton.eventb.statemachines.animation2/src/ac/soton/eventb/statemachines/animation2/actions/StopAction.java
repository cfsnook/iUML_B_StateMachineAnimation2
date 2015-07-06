/**
 * Copyright (c) 2011 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.ISourceProviderService;
import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.animation2.AnimationState;
import ac.soton.eventb.statemachines.animation2.DiagramAnimator;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditor;

/**
 * Stops diagram animation with ProB.
 * 
 * @author vitaly
 *
 */

public class StopAction extends AbstractHandler {

	private static final String EVENTB_PERSPECTIVE_ID = "org.eventb.ui.perspective.eventb";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		boolean foundAnimation=false;
		
		IEditorPart activeEditor = HandlerUtil.getActiveEditorChecked(event);
		if (!(activeEditor instanceof DiagramEditor)) return null;

		EObject element = ((DiagramEditor)activeEditor).getDiagram().getElement();
		if (!(element instanceof Statemachine)) return null;

		IEventBRoot root = AnimateAction.getEventBRoot((Machine)((Statemachine)element).getContaining(MachinePackage.Literals.MACHINE));
		
		for(IWorkbenchPage page : HandlerUtil.getActiveWorkbenchWindow(event).getPages()){

			if (page==null) return null;
			
			for (IEditorReference editorRef : page.getEditorReferences()){
				IEditorPart editor = editorRef.getEditor(true);
					
				if (editor instanceof StatemachinesDiagramEditor && ((StatemachinesDiagramEditor)editor).isAnimating() ){
	
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor)editor).getDiagram().getElement();
					if (root.equals(AnimateAction.getEventBRoot(statemachine))) {
						
						foundAnimation = true;
						
						if (DiagramAnimator.getAnimator().isRunning()) DiagramAnimator.getAnimator().stop();
						Resource resource = statemachine.eResource();
						
						// clear animation artifacts
						TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(resource);
						if (editingDomain != null && !editingDomain.isReadOnly(resource)) {
							
							CompoundCommand cc = new CompoundCommand();
							
							// clear active states
							for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
								if (object != null) {
									cc.append(SetCommand.create(editingDomain, object,
										StatemachinesPackage.Literals.STATE__ACTIVE, 
										SetCommand.UNSET_VALUE));
									cc.append(SetCommand.create(editingDomain, object,
											StatemachinesPackage.Literals.STATE__ACTIVE_INSTANCES, 
											SetCommand.UNSET_VALUE));
								}
							}
							
							// clear enabled transitions
							for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
								if (object != null) { // && ((Transition) object).getOperations() != null && !((Transition) object).getOperations().isEmpty())
									cc.append(SetCommand.create(editingDomain, object,
										StatemachinesPackage.Literals.TRANSITION__OPERATIONS,
										SetCommand.UNSET_VALUE));
								}
							}
							
							editingDomain.getCommandStack().execute(cc);
							
						}
						resource.setModified(false);
	
						//tell editor that we are no longer animating
						((StatemachinesDiagramEditor)editor).stopAnimating();
					}
				}
			}
		}
		if (foundAnimation){
			// switch to Event-B perspective
			IPerspectiveDescriptor perspective = PlatformUI.getWorkbench().getPerspectiveRegistry()
					.findPerspectiveWithId(EVENTB_PERSPECTIVE_ID);
			IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
			if (perspective != null && activePage!=null)	activePage.setPerspective(perspective);
		}
		
		// set animation service state to active
	    ISourceProviderService sourceProviderService = (ISourceProviderService) HandlerUtil.getActiveWorkbenchWindow(event).getService(ISourceProviderService.class);
	    AnimationState animationStateService = (AnimationState) sourceProviderService.getSourceProvider(AnimationState.STATE);
	    animationStateService.setActive(false);
		
		return null;
	}

}
