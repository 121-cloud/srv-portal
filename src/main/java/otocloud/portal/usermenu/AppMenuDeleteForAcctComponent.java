package otocloud.portal.usermenu;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.core.OtoCloudComponentImpl;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * TODO: 
 * @date 2016年11月15日
 * @author lijing
 */
public class AppMenuDeleteForAcctComponent extends OtoCloudComponentImpl {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "acct-menu-del";
	}

	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();
		
		AppMenuDeleteForAcctHandler appMenuDeleteHandler = new AppMenuDeleteForAcctHandler(this);
		ret.add(appMenuDeleteHandler);
		
		
		return ret;
	}

}
