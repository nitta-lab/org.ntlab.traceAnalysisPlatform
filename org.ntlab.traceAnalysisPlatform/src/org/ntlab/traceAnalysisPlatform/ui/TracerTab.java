package org.ntlab.traceAnalysisPlatform.ui;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class TracerTab extends org.eclipse.debug.ui.AbstractLaunchConfigurationTab {

	private static final String TAB_NAME = "Tracer Settings";

	@Override
	public void createControl(Composite parent) {
		Composite composite = 
			SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
		
		((GridLayout)composite.getLayout()).verticalSpacing = 0;
		
	}
	
	private Group createGroup(Composite parent, String title) {
		return SWTFactory.createGroup(parent, title, 2, 1, GridData.FILL_HORIZONTAL);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return TAB_NAME;
	}
	
}
