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
public class AppMenuGetForUserComponent extends OtoCloudComponentImpl {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "user-menu-get";
	}

	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();
		
		AppMenuGetHandler appMenuGetHandler = new AppMenuGetHandler(this);
		ret.add(appMenuGetHandler);
		
		
		return ret;
	}

}
