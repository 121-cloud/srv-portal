package otocloud.portal;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.core.OtoCloudComponent;
import otocloud.framework.core.OtoCloudServiceForVerticleImpl;
import otocloud.persistence.dao.MongoDataSource;
import otocloud.portal.usermenu.AppMenuDeleteForUserComponent;
import otocloud.portal.usermenu.AppMenuGetForUserComponent;

/**
 * TODO: 
 * @date 2016年11月26日
 * @author lijing@yonyou.com
 */
public class PortalService extends OtoCloudServiceForVerticleImpl {	
	
    private MongoDataSource portalMongoDataSource;
    
	public MongoDataSource getPortalMongoDataSource() {
		return portalMongoDataSource;
	}
	
	@Override
	public void afterInit(Future<Void> initFuture) {		
        //如果有mongo_client配置,放入上下文当中.
        if (this.srvCfg.containsKey("mongo_client")) {
            JsonObject mongoClientCfg = this.srvCfg.getJsonObject("mongo_client");
	        if(mongoClientCfg != null){
	        	portalMongoDataSource = new MongoDataSource();
	        	portalMongoDataSource.init(vertxInstance, mongoClientCfg);				
	        }
        }
        
        super.afterInit(initFuture);        
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<OtoCloudComponent> createServiceComponents() {
		
		List<OtoCloudComponent> components = new ArrayList<OtoCloudComponent>();
		
		AppMenuGetForUserComponent appMenuGetForUserComponent = new AppMenuGetForUserComponent();
		components.add(appMenuGetForUserComponent);		
		
		AppMenuDeleteForUserComponent appMenuDeleteComponent = new AppMenuDeleteForUserComponent();
		components.add(appMenuDeleteComponent);	

				
		return components;
	}  
    
	@Override
	public String getServiceName() {
		return "otocloud-portal";
	}
}
