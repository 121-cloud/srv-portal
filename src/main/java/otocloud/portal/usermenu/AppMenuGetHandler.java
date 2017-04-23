package otocloud.portal.usermenu;


import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import otocloud.common.ActionURI;
import otocloud.common.OtoCloudDirectoryHelper;
import otocloud.framework.common.IgnoreAuthVerify;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudComponentImpl;
import otocloud.framework.core.OtoCloudEventHandlerImpl;
import otocloud.portal.PortalService;

/**
 * TODO: 应用模块查询
 * @date 2016年11月15日
 * @author lijing
 */
@IgnoreAuthVerify
public class AppMenuGetHandler extends OtoCloudEventHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "get";

	public AppMenuGetHandler(OtoCloudComponentImpl componentImpl) {
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
		
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
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


	//处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject sessionInfo = msg.getSession();	
		String acctId = sessionInfo.getString("acct_id");
		String userId = sessionInfo.getString("user_id");
		
		PortalService portalService = (PortalService) this.componentImpl.getService();
		
		JsonObject appMenuQuery = new JsonObject().put("user_id", userId);
				
		//先查询用户级菜单
		portalService.getPortalMongoDataSource().getMongoClient()
			.find("user_menu_" + acctId, appMenuQuery, resultHandler -> {
				if (resultHandler.succeeded()) {
					if(resultHandler.result().size() > 0){
						msg.reply(resultHandler.result().get(0));
						return;
					}
				} else{
					Throwable errThrowable = resultHandler.cause();
					String errMsgString = errThrowable.getMessage();
					componentImpl.getLogger().error(errMsgString, errThrowable);
				}
				//如果没有用户级菜单，则查询租户级菜单，生成用户级菜单
				JsonObject acctAppMenuQuery = new JsonObject().put("account", acctId.toString());
				portalService.getPortalMongoDataSource().getMongoClient()
					.find("acct_menu", acctAppMenuQuery, resultHandler2 -> {
					if (resultHandler2.succeeded()) {
						if(resultHandler2.result().size() > 0){
							JsonObject acctMenu = resultHandler2.result().get(0);
							
							Future<JsonObject> buildUserMenuFuture = Future.future();
							
							//基于租户级菜单生成用户级菜单
							buildUserMenu(acctMenu, Long.parseLong(acctId), Long.parseLong(userId), buildUserMenuFuture);
							
							buildUserMenuFuture.setHandler(buildUserMenuRet->{
								if(buildUserMenuRet.succeeded()){
									msg.reply(buildUserMenuRet.result());			
								}else{
									Throwable errThrowable = buildUserMenuRet.cause();
									String errMsgString = errThrowable.getMessage();
									componentImpl.getLogger().error(errMsgString, errThrowable);
									msg.fail(100, errMsgString);	
								}
							});			
							
							return;
						}
					}else{
						Throwable errThrowable = resultHandler2.cause();
						String errMsgString = errThrowable.getMessage();
						componentImpl.getLogger().error(errMsgString, errThrowable);
						//msg.fail(100, errMsgString);	
					}
					
					//读模版菜单
					String menusFilePath = OtoCloudDirectoryHelper.getConfigDirectory() + "app_modules.json";					
					this.componentImpl.getVertx().fileSystem().readFile(menusFilePath, result -> {
			    	    if (result.succeeded()) {
			    	    	String fileContent = result.result().toString(); 			    	        
			    	    	JsonObject tmpMenus = new JsonObject(fileContent);			    	        
			    	    	
							//先生成租户级菜单
							Future<JsonObject> buildAcctMenuFuture = Future.future();					
							
							buildAcctMenu(tmpMenus, Long.parseLong(acctId), buildAcctMenuFuture);
							
							buildAcctMenuFuture.setHandler(buildAcctMenuRet->{
								if(buildAcctMenuRet.succeeded()){							
									
									JsonObject acctMenu = buildAcctMenuRet.result();
									
									Future<JsonObject> buildUserMenuFuture = Future.future();
									
									//基于租户级菜单生成用户级菜单
									buildUserMenu(acctMenu, Long.parseLong(acctId), Long.parseLong(userId), buildUserMenuFuture);
									
									buildUserMenuFuture.setHandler(buildUserMenuRet->{
										if(buildUserMenuRet.succeeded()){
											msg.reply(buildUserMenuRet.result());			
										}else{
											Throwable errThrowable = buildUserMenuRet.cause();
											String errMsgString = errThrowable.getMessage();
											componentImpl.getLogger().error(errMsgString, errThrowable);
											msg.fail(100, errMsgString);	
										}
									});
									
								}else{
									Throwable errThrowable = buildAcctMenuRet.cause();
									String errMsgString = errThrowable.getMessage();
									componentImpl.getLogger().error(errMsgString, errThrowable);
									msg.fail(100, errMsgString);	
								}
							});
			    	    	
			    	        
			    	    } else {
							Throwable errThrowable = result.cause();
							String errMsgString = errThrowable.getMessage();
							componentImpl.getLogger().error(errMsgString, errThrowable);
							msg.fail(100, errMsgString);		
			   
			    	    }	
					});				


				});				
		});

	}
	
	
	private void buildAcctMenu(JsonObject templateMenu, Long acctId, Future<JsonObject> result){
		JsonArray menu_items = templateMenu.getJsonArray("menu_items");
		
		JsonArray acctMenuItems = new JsonArray();
		
		List<Future> futures = new ArrayList<Future>();
		
		menu_items.forEach(item->{
			JsonObject menu_item = (JsonObject)item;
			Long appId = menu_item.getLong("app_id");
			
			String authSrvName = componentImpl.getDependencies().getJsonObject("acct_service").getString("service_name","");
			String address = authSrvName + ".acct-app.app-permission-verfication";
			
			
			JsonObject reqContent = new JsonObject();
			reqContent.put("acct_id", acctId);
			reqContent.put("app_id", appId);
			
			JsonObject reqMsg = new JsonObject();
			reqMsg.put("content", reqContent);
			
			Future<Void> retFuture = Future.future();
			futures.add(retFuture);
			
			componentImpl.getEventBus().send(address,
					reqMsg, regUserRet->{
						if(regUserRet.succeeded()){
							JsonObject JsonObject = (JsonObject)regUserRet.result().body();
							if(JsonObject.getBoolean("result")){
								acctMenuItems.add(menu_item.copy());
							}
							retFuture.complete();											
						}else{		
							Throwable errThrowable = regUserRet.cause();
							String errMsgString = errThrowable.getMessage();
							componentImpl.getLogger().error(errMsgString, errThrowable);
							retFuture.fail(errThrowable);
						}	
						
			});	
			
			
		});
		
		CompositeFuture.join(futures).setHandler(ar -> { // 合并所有for循环结果，返回外面
			JsonObject acctMenu = new JsonObject()
										.put("menu_type", "app_menu")
										.put("account", acctId.toString());
			
			acctMenu.put("menu_items", acctMenuItems);
			
			PortalService portalService = (PortalService) this.componentImpl.getService();
			portalService.getPortalMongoDataSource().getMongoClient().save("acct_menu", acctMenu, resultHandler->{
				if (resultHandler.succeeded()) {
					result.complete(acctMenu);
				}else{
					Throwable errThrowable = resultHandler.cause();
					String errMsgString = errThrowable.getMessage();
					componentImpl.getLogger().error(errMsgString, errThrowable);	
					result.fail(errThrowable);
				}
			});

			
		});

	}
	
	private void buildUserMenu(JsonObject acctMenu, Long acctId, Long userId, Future<JsonObject> result){
		JsonArray menu_items = acctMenu.getJsonArray("menu_items");
		
		JsonArray userMenuItems = new JsonArray();
		
		List<Future> futures = new ArrayList<Future>();
		
		String authSrvName = componentImpl.getDependencies().getJsonObject("auth_service").getString("service_name","");
		String address = authSrvName + ".authentication.app-permission-verfication";
		String address2 = authSrvName + ".authentication.activity-permission-verfication";
		
		menu_items.forEach(item->{
			JsonObject menu_item = (JsonObject)item;
			Long appId = menu_item.getLong("app_id");			
			
			JsonObject reqContent = new JsonObject();
			reqContent.put("acct_id", acctId);
			reqContent.put("app_id", appId);
			reqContent.put("user_id", userId);
			
			JsonObject reqMsg = new JsonObject();
			reqMsg.put("content", reqContent);
			
			Future<Void> retFuture = Future.future();
			futures.add(retFuture);
			
			componentImpl.getEventBus().send(address,
					reqMsg, regUserRet->{
						if(regUserRet.succeeded()){
							JsonObject JsonObject = (JsonObject)regUserRet.result().body();
							if(JsonObject.getBoolean("result")){
								userMenuItems.add(menu_item.copy());
							}
							retFuture.complete();											
						}else{		
							Throwable errThrowable = regUserRet.cause();
							String errMsgString = errThrowable.getMessage();
							componentImpl.getLogger().error(errMsgString, errThrowable);
							retFuture.fail(errThrowable);
						}	
						
			});	
			
			
		});
		
		CompositeFuture.join(futures).setHandler(ar -> { // 合并所有for循环结果，返回外面			
			
			if(userMenuItems.size() > 0){				
				
				List<JsonObject> childRemovedItems = new ArrayList<>();			
				
				List<Future> childfutures = new ArrayList<Future>();
				
				userMenuItems.forEach(item->{
					Future<Void> childFuture = Future.future();
					childfutures.add(childFuture);	
					
					buildSubMenu(address2, childRemovedItems, 
							(JsonObject)item, acctId, userId, childFuture);
					
				});
				
				CompositeFuture.join(childfutures).setHandler(ar2 -> { // 合并所有for循环结果，返回外面
					if(childRemovedItems.size() > 0){
						for(JsonObject removedItem : childRemovedItems){
							userMenuItems.remove(removedItem);
						}
					}
					
					JsonObject userMenu = new JsonObject()
						.put("menu_type", "app_menu")
						.put("user_id", userId.toString())
						.put("account", acctId.toString());

					userMenu.put("menu_items", userMenuItems);
					
					PortalService portalService = (PortalService) this.componentImpl.getService();
					portalService.getPortalMongoDataSource().getMongoClient().save("user_menu_" + acctId, userMenu, resultHandler->{
						if (resultHandler.succeeded()) {
							result.complete(userMenu);
						}else{
							Throwable errThrowable = resultHandler.cause();
							String errMsgString = errThrowable.getMessage();
							componentImpl.getLogger().error(errMsgString, errThrowable);	
							result.fail(errThrowable);
						}
					});
				});
				
			}else{
				result.fail("无任何权限");
			}
			
		});

	}

	
	/**
	{
		user_id
		activity_id
		acct_id
	}
	*/
	private void buildSubMenu(String address, List<JsonObject> removedItems,
			JsonObject menuItem, Long acctId, Long userId, Future<Void> parentFuture){
		
		if(menuItem.containsKey("activity_id")){
			Long activityId = menuItem.getLong("activity_id");			
			
			JsonObject reqContent = new JsonObject();
			reqContent.put("acct_id", acctId);
			reqContent.put("activity_id", activityId);
			reqContent.put("user_id", userId);
			
			JsonObject reqMsg = new JsonObject();
			reqMsg.put("content", reqContent);			
	
			componentImpl.getEventBus().send(address,
					reqMsg, regUserRet->{
						if(regUserRet.succeeded()){
							JsonObject JsonObject = (JsonObject)regUserRet.result().body();
							if(JsonObject.getBoolean("result")){								
								
							}else{
								removedItems.add(menuItem);
								//subMenus.remove(menuItem);
							}
							parentFuture.complete();											
						}else{	
							//subMenus.remove(menuItem);
							removedItems.add(menuItem);
							
							Throwable errThrowable = regUserRet.cause();
							String errMsgString = errThrowable.getMessage();
							componentImpl.getLogger().error(errMsgString, errThrowable);
							parentFuture.fail(errThrowable);
						}	
						
			});	
			
		}else{		
			if(menuItem.containsKey("children")){		
				JsonArray childMenus =  menuItem.getJsonArray("children");
				
				List<JsonObject> childRemovedItems = new ArrayList<>();			
				
				List<Future> childfutures = new ArrayList<Future>();
				
				childMenus.forEach(item->{
					Future<Void> childFuture = Future.future();
					childfutures.add(childFuture);	
					
					buildSubMenu(address, childRemovedItems, 
							(JsonObject)item, acctId, userId, childFuture);
					
				});
				
				CompositeFuture.join(childfutures).setHandler(ar -> { // 合并所有for循环结果，返回外面
					if(childRemovedItems.size() > 0){
						for(JsonObject removedItem : childRemovedItems){
							childMenus.remove(removedItem);
						}
						if(childMenus.size() <= 0){
							removedItems.add(menuItem);
						}
					}
					parentFuture.complete();					
				});
				
			}else{
				removedItems.add(menuItem);
				parentFuture.complete();	
			}			
			
		}	
		
	}
	
	
}
