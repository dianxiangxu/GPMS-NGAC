var savePolicy = '';

$(function() {

	if (isAdmin == "false") {
		if (userProfileId == "null") {
			window.location = 'Login.jsp';
		} else {
			window.location = 'Home.jsp';
		}
	} else {
		if (userProfileId == "null") {
			window.location = 'Login.jsp';
		}
	}

	jQuery.fn.exists = function() {
		return this.length > 0;
	}
	
	
	var gpmsCommonObj = function() {
		var gpmsCommonInfo = {
			UserProfileID : GPMS.utils.GetUserProfileID(),
			UserName : GPMS.utils.GetUserName(),
			UserIsAdmin : GPMS.utils.IsAdmin(),
			UserPositionType : GPMS.utils.GetUserPositionType(),
			UserPositionTitle : GPMS.utils.GetUserPositionTitle(),
			UserDepartment : GPMS.utils.GetUserDepartment(),
			UserCollege : GPMS.utils.GetUserCollege()
		};
		return gpmsCommonInfo;
	};
	
	savePolicy = {
			config : {
				isPostBack : false,
				async : false,
				cache : false,
				type : 'POST',
				contentType : "application/json; charset=utf-8",
				data : '{}',
				dataType : 'json',
				baseURL : GPMS.utils.GetGPMSServicePath() + "users/",
				method : "",
				url : "",
				ajaxCallMode : 0
			},

			ajaxCall : function(config) {
				$
						.ajax({
							type : savePolicy.config.type,
							beforeSend : function(request) {
								request.setRequestHeader('GPMS-TOKEN', _aspx_token);
								request.setRequestHeader("UName", GPMS.utils
										.GetUserName());
								request.setRequestHeader("PID", GPMS.utils
										.GetUserProfileID());
								request.setRequestHeader("PType", "v");
								request.setRequestHeader('Escape', '0');
							},
							contentType : savePolicy.config.contentType,
							cache : savePolicy.config.cache,
							async : savePolicy.config.async,
							url : savePolicy.config.url,
							data : savePolicy.config.data,
							dataType : savePolicy.config.dataType,
							success : savePolicy.ajaxSuccess,
							error : savePolicy.ajaxFailure
						});
			},
			
			savePolicy : function() {
				this.config.url = this.config.baseURL + "SavePolicy";
				this.config.data = JSON2.stringify({
					gpmsCommonObj : gpmsCommonObj()
				});
				this.config.ajaxCallMode = 11;
				this.ajaxCall(this.config);
				return false;
			},
			
			ajaxSuccess : function(msg) {
				switch (savePolicy.config.ajaxCallMode) {
				case 0:
					break;
				
				case 11:
					csscody.alert("<h2>" + 'Information Message' + "</h2><p>"
								+ 'Policy saved successfully.' + "</p>");
					break;	
				}
			},

			ajaxFailure : function(msg) {
				switch (savePolicy.config.ajaxCallMode) {
				case 0:
					break;
									
				case 11:
					csscody.error("<h2>" + 'Error Message' + "</h2><p>"
							+ 'Cannot save policy!' + "</p>");
					break;	
				}
			},
			
			init : function() {
			
			
			$('#btnSavePolicy').click(function(e) {
				$(this).disableWith('Saving...');
				savePolicy.savePolicy();
				
				$(this).enableAgain();
				e.preventDefault();
				return false;
			});
	
		}
	};
	savePolicy.init();
});