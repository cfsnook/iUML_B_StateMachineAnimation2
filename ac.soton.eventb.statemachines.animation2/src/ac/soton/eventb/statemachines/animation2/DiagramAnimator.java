/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation2.listeners.AnimationListener;

import com.google.inject.Injector;
//import com.thoughtworks.xstream.XStream;
//import com.thoughtworks.xstream.io.xml.DomDriver;
//import com.thoughtworks.xstream.mapper.MapperWrapper;

//import de.bmotionstudio.gef.editor.Animation;
//import de.bmotionstudio.gef.editor.BMotionEditorPlugin;
//import de.bmotionstudio.gef.editor.BMotionStudioEditor;
//import de.bmotionstudio.gef.editor.model.Visualization;
import de.prob.Main;
import de.prob.exception.ProBError;
import de.prob.model.eventb.EventBModel;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.eclipse.VersionController;

/**
 * @author vitaly
 *
 */
public class DiagramAnimator {

	private static DiagramAnimator animator;
	private static final IAnimationChangeListener animationListener = new AnimationListener();
	private Machine machine;
	private List<Statemachine> rootStatemachines = new ArrayList<Statemachine>();
	private boolean bms = false;
	private AnimationSelector selector;
	private Trace trace;
	
	/**
	 * @return
	 */
	public static DiagramAnimator getAnimator() {
		if (animator == null)
			animator = new DiagramAnimator();
		return animator;
	}

	/**
	 * @return
	 */
	public List<Statemachine> getRootStatemachines() {
		return rootStatemachines;
	}

	/**
	 * @return
	 */
	public Machine getMachine() {
		return machine;
	}
	
	/**
	 * @return
	 */
	public Trace getTrace() {
		return trace;
	}
	
	/**
	 * @param trace
	 */
	public void setTrace(Trace trace) {
		this.trace = trace;
	}
	
	/**
	 * @param op
	 */
	public void animate(Transition op) {
		trace = trace.add(op);
		selector.traceChange(trace);
	}

	/**
	 * Starts the diagram animations including starting ProB animator for the given eventBRoot
	 * 
	 * rootStatemachines must be obtained from the elements of the open editors otherwise the diagrams will not update
	 * 
	 * 
	 * @throws ProBException 
	 * 
	 */
	public boolean start(Machine machine, List<Statemachine> rootStatemachines, IEventBRoot root, List<IFile> bmsFiles) {
		this.machine = machine;
		this.rootStatemachines = rootStatemachines;
		System.out.println("Starting ProB 2 for " + machine);

		return startProB2(root);
		
//		bms = false;
//		// if BMotionStudio files supplied, run them.
//		for (IFile bmsFile : bmsFiles){
//			bms = runBMotionStudio(bmsFile, probAnimator);
//		}
	}
	
	
	private boolean startProB2(IEventBRoot rootElement) {
		VersionController.ensureInstalled();

		String fileName = rootElement.getResource().getRawLocation().makeAbsolute().toOSString();
		if (fileName.endsWith(".buc")) {
			fileName = fileName.replace(".buc", ".bcc");
		} else {
			fileName = fileName.replace(".bum", ".bcm");
		}
		
		// load a machine
		Injector injector = Main.getInjector();
		final EventBFactory instance = injector.getInstance(EventBFactory.class);
		EventBModel model = null;
		try {
			model = instance.load(fileName);
			StateSpace s = model.getStateSpace();

			trace = new Trace(s);
			selector = injector.getInstance(AnimationSelector.class);
			selector.clearUnprotected();
			selector.addNewAnimation(trace, false);
			selector.registerAnimationChangeListener(animationListener);

			System.gc();

			// ProB perspective
			final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						workbenchWindow.getWorkbench().showPerspective("de.prob2.perspective", workbenchWindow);
					} catch (WorkbenchException e) {}
				}
			});
		    return true;
		} catch (IOException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Loading of the model failed."
							+ " Please check to make sure that the Rodin static checker has "
					+ "produced a valid static checked file (.bcc or .bcm)."
					+ " If not, try cleaning the project.");
		} catch (ModelTranslationError e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Was not able to translate the model because of the following error: "
					+ e.getMessage());
		} catch (ProBError e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"ProB was not able to load the model.\n"
					+ "This is because: " + e.getMessage());
		}
		return false;
	}

	public void stop() {
		machine = null;
		rootStatemachines.clear();
		bms = false;
		
		trace = null;
		if (selector != null)
			selector.deregisterAnimationChangeListener(animationListener);
		selector = null;
	}

	/**
	 * @return
	 */
	public boolean isRunning() {
		return machine != null 
				&& selector != null && selector.getTrace(trace.getUUID()) != null;	//TODO: add check on the ProB status
	}

	public boolean isRunningBMotionStudio(){
		return bms;
	}
	
//	/**
//	 * 
//	 * @param bmsFile
//	 * @param animator
//	 * @return 
//	 */
//	private boolean runBMotionStudio(IFile bmsFile, Animator animator){
//		Visualization visualization;
//		try {
//			visualization = createVisualizationRoot(bmsFile);
//			Animation animation = new Animation(animator, visualization);
//			BMotionStudioEditor bmsEditor = getBmotionStudioEditor(bmsFile);
//			bmsEditor.createRunPage(visualization, animation);
//			return true;
//		} catch (CoreException e) {
//			StatemachineAnimationPlugin.logError("Eclipse Core Exception while attempting to launch BMotion Studio", e);
//		} catch (IOException e) {
//			StatemachineAnimationPlugin.logError("IO Exception while attempting to launch BMotion Studio", e);
//		} catch (ParserConfigurationException e) {
//			StatemachineAnimationPlugin.logError("Parser Configuration Exception while attempting to launch BMotion Studio", e);
//		} catch (SAXException e) {
//			StatemachineAnimationPlugin.logError("SAX Exception while attempting to launch BMotion Studio", e);
//		}
//		return false;
//
//	}
//	
//	/**
//	 * given a BMotionStudio bms file, opens it in an editor and returns the editor
//	 * @param bmsFile
//	 * @return BMotionStudioEditor
//	 * @throws PartInitException
//	 */
//	private BMotionStudioEditor getBmotionStudioEditor(IFile bmsFile) throws PartInitException{
//	    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(bmsFile.getName());
//		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(bmsFile), desc.getId());
//	    if (part instanceof BMotionStudioEditor) {
//	        return (BMotionStudioEditor) part;
//	    }else{
//	    	return null;
//	    }
//}
//	
//	/**
//	 * Return a visualisation object for the given BMotion Studio bms file
//	 * 
//	 * @param bmsFile
//	 * @return Visualization
//	 * @throws CoreException
//	 * @throws IOException
//	 * @throws ParserConfigurationException
//	 * @throws SAXException
//	 */
//    private Visualization createVisualizationRoot(IFile bmsFile) throws CoreException, IOException, ParserConfigurationException, SAXException {
//            XStream xstream = new XStream(new DomDriver()) {
//                    @Override
//                    protected MapperWrapper wrapMapper(MapperWrapper next) {
//                            return new MapperWrapper(next) {                      
//                            		@Override
//                                    public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn,String fieldName) {
//                                            if (definedIn == Object.class) {
//                                                    return false;
//                                            }
//                                            return super.shouldSerializeMember(definedIn, fieldName); 
//                                    }
//                            };
//                    }
//            };
//            BMotionEditorPlugin.setAliases(xstream);
//            Visualization visualization = (Visualization) xstream.fromXML(bmsFile.getContents());
//            visualization.setProjectFile(bmsFile);
//            visualization.setIsRunning(true);
//            return visualization;
//    }
    
}
