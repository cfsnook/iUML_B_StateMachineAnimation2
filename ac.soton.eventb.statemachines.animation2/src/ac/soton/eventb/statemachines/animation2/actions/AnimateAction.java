/**
 * Copyright (c) 2011 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.ISourceProviderService;
import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation2.AnimationState;
import ac.soton.eventb.statemachines.animation2.DiagramAnimator;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditor;
//import de.bmotionstudio.gef.editor.BMotionStudioEditor;
//import de.prob.exceptions.ProBException;

/**
 * Starts diagram animation with ProB.
 * 
 * @author vitaly
 *
 */
public class AnimateAction extends AbstractHandler {

//	private static final String BMOTION_STUDIO_EXT = "bmso";
//	private static final String BMS_RUN_PERSPECTIVE_ID = "de.bmotionstudio.perspective.run";
//	private static final String PROB_PERSPECTIVE_ID = "de.prob.ui.perspective";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (DiagramAnimator.getAnimator().isRunning()) return null;
		List<IFile> bmsFiles = new ArrayList<IFile>();
		
		IEditorPart activeEditor = HandlerUtil.getActiveEditorChecked(event);
		if (!(activeEditor instanceof DiagramEditor)) return null;

		EObject element = ((DiagramEditor)activeEditor).getDiagram().getElement();
		if (!(element instanceof Statemachine)) return null;

		Machine machine = (Machine) ((Statemachine)element).getContaining(MachinePackage.Literals.MACHINE);
		IEventBRoot root = getEventBRoot(machine);
		
		List<Statemachine> statemachines = new ArrayList<Statemachine>();
		List<StatemachinesDiagramEditor> editors = new ArrayList<StatemachinesDiagramEditor>();
		// Find all the statemachines of the SAME machine that are open as diagrams 
		// (these must come from the editors as each editor has a different local copy)
		for(IWorkbenchPage page : HandlerUtil.getActiveWorkbenchWindow(event).getPages()){
			
	    	for(IEditorReference editorRef: page.getEditorReferences()){
	    		IEditorPart editor = editorRef.getEditor(true);
	    		
				if (editor instanceof StatemachinesDiagramEditor ){
	
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor)editor).getDiagram().getElement();
					if (root.equals(AnimateAction.getEventBRoot(statemachine))) {
						
		    			if (editor.isDirty()){
		    				editor.doSave(new NullProgressMonitor());
		    			}
	    				statemachines.add(statemachine);
	    				
	    				// let the editor know that we are animating so that it doesn't try to save animation artifacts
			    		((StatemachinesDiagramEditor)editor).startAnimating();
			    		editors.add((StatemachinesDiagramEditor)editor);
					}
		    	}
				
	    		//also look for BMotionStudio editors on the same machine
//	    		if (editor instanceof BMotionStudioEditor) {
//	    			BMotionStudioEditor bmsEditor = (BMotionStudioEditor) editor;
//	    			Object pf = bmsEditor.getVisualization().getProjectFile();
//	
//	    			if (pf instanceof IFile){
//		    			String machineName = bmsEditor.getVisualization().getMachineName();
//		    			IProject project = ((IFile)pf).getProject();
//	    				if ( 
//	    					BMOTION_STUDIO_EXT.equals(((IFile)pf).getFileExtension()) &&
//	    					machineName.startsWith(machine.getName()) &&
//	    					root.getRodinProject().getProject().equals(project)
//	    					){
//	    					if (!bmsFiles.contains(pf)) bmsFiles.add(((IFile)pf));
//	    				}
//	    			}
//		    	}
	    	}
    	}

//		try {
			// run animation
			DiagramAnimator diagramAnimator = DiagramAnimator.getAnimator();
			if(diagramAnimator.start(machine, statemachines, root, bmsFiles)) {
				// set animation service state to active
			    ISourceProviderService sourceProviderService = (ISourceProviderService) HandlerUtil.getActiveWorkbenchWindow(event).getService(ISourceProviderService.class);
			    AnimationState animationStateService = (AnimationState) sourceProviderService.getSourceProvider(AnimationState.STATE);
			    animationStateService.setActive(true);
			}
			
//			//switch to a suitable perspective
//			IPerspectiveDescriptor perspective = null;
//			IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
//			if (diagramAnimator.isRunningBMotionStudio()){
//				perspective = registry.findPerspectiveWithId(BMS_RUN_PERSPECTIVE_ID);
//			}
//			if (perspective==null){
//				perspective = registry.findPerspectiveWithId(PROB_PERSPECTIVE_ID);
//			}
//			if (perspective != null) {
//				activeEditor.getSite().getPage().setPerspective(perspective);
//				activeEditor.getSite().getPage().setEditorAreaVisible(true);
//			}
//		} catch (ProBException e) {
//			for (StatemachinesDiagramEditor editor : editors){
//				editor.stopAnimating();
//			}
//			StatemachineAnimationPlugin.getDefault().getLog().log(
//					new Status(IStatus.ERROR, StatemachineAnimationPlugin.PLUGIN_ID,
//							"Animation startup failed for: " , e));
//		}
		return null;
	}

	public static IEventBRoot getEventBRoot(EventBElement element){
		Resource resource = element.eResource();
		if (resource != null && resource.isLoaded()) {
			IFile file = WorkspaceSynchronizer.getFile(resource);
			IRodinProject rodinProject = RodinCore.getRodinDB().getRodinProject(file.getProject().getName());
			IEventBRoot root = (IEventBRoot) rodinProject.getRodinFile(file.getName()).getRoot();
			return root;
		}
		return null;
   	}
	
}
