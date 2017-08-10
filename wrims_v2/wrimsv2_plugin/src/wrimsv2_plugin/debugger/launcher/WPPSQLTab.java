package wrimsv2_plugin.debugger.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import wrimsv2_plugin.debugger.core.DebugCorePlugin;
import wrimsv2_plugin.debugger.dialog.WPPDssToSqlDialog;
import wrimsv2_plugin.debugger.exception.WPPException;

public class WPPSQLTab extends AbstractLaunchConfigurationTab {

	private Text databaseURLText;
	private Text groupText;
	private Button dssConvert;

	@Override
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.verticalSpacing = 0;
		topLayout.numColumns = 7;
		comp.setLayout(topLayout);
		comp.setFont(font);
		
		createVerticalSpacer(comp, 3);
		
		Label databaseURLLabel = new Label(comp, SWT.NONE);
		databaseURLLabel.setText("Database URL:");
		GridData gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		databaseURLLabel.setLayoutData(gd);
		databaseURLLabel.setFont(font);
		
		databaseURLText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		databaseURLText.setLayoutData(gd);
		databaseURLText.setFont(font);
		databaseURLText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		Label groupLabel = new Label(comp, SWT.NONE);
		groupLabel.setText("Group of Scenarios:");
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		groupLabel.setLayoutData(gd);
		groupLabel.setFont(font);
		
		groupText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		groupText.setLayoutData(gd);
		groupText.setFont(font);
		groupText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	
		dssConvert = new Button(comp, SWT.NONE);
		dssConvert.setText("&Convert from DSS");
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan=2;
		dssConvert.setLayoutData(gd);
		dssConvert.setFont(font);
		dssConvert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final IWorkbench workbench=PlatformUI.getWorkbench();
				workbench.getDisplay().asyncExec(new Runnable(){
					public void run(){
						Shell shell=workbench.getActiveWorkbenchWindow().getShell();
						WPPDssToSqlDialog dialog = new WPPDssToSqlDialog(shell, databaseURLText.getText(), groupText.getText());
						dialog.openDialog();
					}
				});
			}
		});
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_DATABASEURL, "none");
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_SQLGROUP, "calsim");		
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		String databaseURL = null;
		String sqlGroup = null;
		try {
			databaseURL = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_DATABASEURL, "none");
			databaseURLText.setText(databaseURL);
			
			sqlGroup = configuration.getAttribute(DebugCorePlugin.ATTR_WPP_SQLGROUP, "calsim");
			groupText.setText(sqlGroup);
		} catch (CoreException e) {
			WPPException.handleException(e);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String databaseURL=databaseURLText.getText();
		if (databaseURL.trim().equals("")){
			databaseURLText.setText("none");
			databaseURL="none";
		}
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_DATABASEURL, databaseURL);
		
		String sqlGroup=groupText.getText();
		if (sqlGroup.trim().equals("")){
			groupText.setText("calsim");
			sqlGroup="calsim";
		}
		configuration.setAttribute(DebugCorePlugin.ATTR_WPP_SQLGROUP, sqlGroup);
	}

	@Override
	public String getName() {
		return "SQL";
	}

}