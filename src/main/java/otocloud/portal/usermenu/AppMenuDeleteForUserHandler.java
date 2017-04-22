package otocloud.portal.usermenu;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.common.IgnoreAuthVerify;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
import otocloud.framework.core.OtoCloudComponentImpl;
import otocloud.framework.core.OtoCloudEventHandlerImpl;
import otocloud.portal.PortalService;

/**
 * TODO: 删除用户应用模块
 * @date 2016年11月15日
 * @author lijing
 */
@IgnoreAuthVerify
public class AppMenuDeleteForUserHandler extends OtoCloudEventHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "delete";

	public AppMenuDeleteForUserHandler(OtoCloudComponentImpl componentImpl) {
		super(componentImpl);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public HandlerDescriptor getHanlderDesc() {		
		
		HandlerDescriptor handlerDescriptor = super.getHanlderDesc();
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		handlerDescriptor.setParamsDesc(paramsDesc);	*/
		
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.DELETE);
		handlerDescriptor.setRestApiURI(uri);
		
		return handlerDescriptor;		

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}


	/**
	 * {
	 *    acct_id: 
	 *    user_id:
	 * }
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
    	JsonObject body = msg.body();
		
        JsonObject content = body.getJsonObject("content"); 
		
		String acctId = content.getString("acct_id");
		String userId = content.getString("user_id");
		
		PortalService portalService = (PortalService) this.componentImpl.getService();
		
		JsonObject appMenuQuery = new JsonObject().put("user_id", userId)
				.put("account", acctId);
				
		//先查询用户级菜单
		portalService.getPortalMongoDataSource().getMongoClient()
			.removeDocument("user_menu_" + acctId, appMenuQuery, resultHandler -> {
				if (resultHandler.succeeded()) {				
					msg.reply(resultHandler.result().toJson());
					return;					
				} else{
					Throwable errThrowable = resultHandler.cause();
					String errMsgString = errThrowable.getMessage();
					componentImpl.getLogger().error(errMsgString, errThrowable);
					msg.fail(100, errMsgString);	
				}
		});

	}

	
}
