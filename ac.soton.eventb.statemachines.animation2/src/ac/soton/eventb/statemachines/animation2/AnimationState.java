/**
 * 
 */
package ac.soton.eventb.statemachines.animation2;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/**
 * @author vitaly
 *
 */
public class AnimationState extends AbstractSourceProvider {

	public static final String STATE = "ac.soton.eventb.statemachines.animation2.state";
	public final static String STARTED = "started";
	public final static String STOPPED = "stopped";
	private boolean active;

	@Override
	public void dispose() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getCurrentState() {
	    Map map = new HashMap(1);
	    String value = active ? STARTED : STOPPED;
	    map.put(STATE, value);
	    return map;
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[]{STATE};
	}
	
	public void setActive(boolean active) {
		this.active = active;
	    String value = active ? STARTED : STOPPED;
	    fireSourceChanged(ISources.WORKBENCH, STATE, value);
	}

}
