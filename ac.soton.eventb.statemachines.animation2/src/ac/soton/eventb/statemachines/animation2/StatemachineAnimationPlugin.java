/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class StatemachineAnimationPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ac.soton.eventb.statemachines.animation2"; //$NON-NLS-1$

	// The shared instance
	private static StatemachineAnimationPlugin plugin;
	
	/**
	 * The constructor
	 */
	public StatemachineAnimationPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static StatemachineAnimationPlugin getDefault() {
		return plugin;
	}

	public static void logError(String message, Exception e) {
		StatemachineAnimationPlugin.getDefault().getLog().log(new Status(
				 IStatus.ERROR,
				 StatemachineAnimationPlugin.PLUGIN_ID,
				 IStatus.ERROR,
				 message,
				 e));
	}
	
	public static void logError(String message) {
		logError(message,null);
	}
}
