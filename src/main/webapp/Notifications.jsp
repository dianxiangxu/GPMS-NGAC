<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<!-- <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"> -->
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Content-Script-Type" content="text/javascript">
<meta http-equiv="Content-Style-Type" content="text/css">
<meta content="My Notifications" name="DESCRIPTION">
<meta content="My Notifications" name="KEYWORDS">
<meta content="@GPMS" name="COPYRIGHT">
<meta content="GENERATOR" name="GENERATOR">
<meta content="Author" name="AUTHOR">
<meta content="DOCUMENT" name="RESOURCE-TYPE">
<meta content="GLOBAL" name="DISTRIBUTION">
<meta content="INDEX, FOLLOW" name="ROBOTS">
<meta content="1 DAYS" name="REVISIT-AFTER">
<meta content="GENERAL" name="RATING">
<meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE">
<!-- Mimic Internet Explorer 7 -->
<meta content="IE=EmulateIE7" http-equiv="X-UA-Compatible">
<meta content="RevealTrans(Duration=0,Transition=1)"
	http-equiv="PAGE-ENTER">
<link type="icon shortcut" media="icon" href="favicon.ico">
<!--[if IE 9]>
        <link rel="stylesheet" href="css/ie9.css" type="text/css" media="screen"/><![endif]-->
<!--[if lt IE 9]>
        <link rel="stylesheet" href="css/IE.css" type="text/css" media="screen"/><![endif]-->
<!--[if lt IE 7]>
        <script type="text/javascript" src="js/core/IE8.js"></script>
    <![endif]-->
<title>My Notifications</title>

<script src="js/jQuery/jquery-1.11.3.min.js" type="text/javascript"></script>

<script type="text/javascript">
	var _aspx_token = "NWExODgyNDctMzA2OS00MWNhLWJjOWEtNGEyODI5N2FiZWJjOlNhZ2VGcmFtZS5BVVRIanhyMzB3eWNqenZwcWQwanYzdmt5Yng0WkFESlg5U0xPQzE6MjAxNTA2MzAxNTA2NTg5NDM5";
	$.ajaxSetup({
		'beforeSend' : function(xhr) {
			xhr.setRequestHeader("ASPX-TOKEN", _aspx_token);
		}
	});
</script>

<script type="text/javascript">
	//<![CDATA[
	var gpmsAppPath = "";
	           
	var userProfileId = '<%=session.getAttribute("userProfileId")%>';
	var gpmsUserName = '<%=session.getAttribute("gpmsUserName")%>';
	var isAdmin = '<%=session.getAttribute("isAdmin")%>';
	var userPositionType = '<%=session.getAttribute("userPositionType")%>';
	var	userPositionTitle = '<%=session.getAttribute("userPositionTitle")%>';
	var userDepartment = '<%=session.getAttribute("userDepartment")%>';
	var userCollege = '<%=session.getAttribute("userCollege")%>';

	var gpmsServicePath = "REST/";
	var gpmsRootPath = "http://localhost:8181/GPMS/";

	$(function() {
		//For Sidebar active menu
		$('.acitem').find('a').eq(1).prop("class", "active");
	});

	//]]>
</script>

<script type="text/javascript" src="js/jQuery/jquery-ui.js"></script>

<script type="text/javascript" src="js/core/gpmscore.js"></script>

<script type="text/javascript" src="js/core/jquery.disable_with.js"></script>

<!-- For Side Bar Navigation -->
<script type="text/javascript" src="js/core/dashboard.js"></script>
<script type="text/javascript" src="js/sidebar_accordian.js"></script>
<script type="text/javascript" src="js/superfish.js"></script>

<script type="text/javascript"
	src="js/FormValidation/jquery.validate.js"></script>
<script type="text/javascript"
	src="js/FormValidation/jquery.ui.datepicker.validation.js"></script>
<script type="text/javascript"
	src="js/FormValidation/jquery.maskedinput.js"></script>
<script type="text/javascript" src="js/FormValidation/autoNumeric.js"></script>

<script type="text/javascript" src="js/core/json2.js"></script>

<script type="text/javascript" src="js/jquery-browser.js"></script>
<script type="text/javascript" src="js/jquery.uniform.js"></script>

<script type="text/javascript" src="js/GridView/jquery.tablesorter.js"></script>
<script type="text/javascript" src="js/GridView/jquery.grid.js"></script>
<script type="text/javascript" src="js/GridView/SagePaging.js"></script>
<script type="text/javascript" src="js/GridView/jquery.global.js"></script>
<script type="text/javascript" src="js/GridView/jquery-dateFormat.js"></script>

<script type="text/javascript" src="js/MessageBox/jquery.easing.1.3.js"></script>
<script type="text/javascript" src="js/MessageBox/alertbox.js"></script>

<script type="text/javascript" src="js/modules/Notifications.js"></script>

<link type="text/css" rel="stylesheet"
	href="css/Templates/jquery-ui.css" />

<link type="text/css" rel="stylesheet" href="css/MessageBox/style.css" />

<link type="text/css" rel="stylesheet" href="css/GridView/tablesort.css" />
<link type="text/css" rel="stylesheet" href="css/Templates/grid.css" />
<link type="text/css" rel="stylesheet"
	href="css/Templates/topstickybar.css" />
<link type="text/css" rel="stylesheet" href="css/Templates/admin.css" />

</head>
<body>
	<form enctype="multipart/form-data" action="Notifications.jsp"
		method="post" name="form1" id="form1">
		<div style="display: none;" id="UpdateProgress1">
			<div class="sfLoadingbg">&nbsp;</div>
			<div class="sfLoadingdiv">
				<img id="imgProgress" src="./images/ajax-loader.gif"
					style="border-width: 0px;" alt="Loading..." title="Loading..." />
				<br> <span id="lblPrgress">Please wait...</span>
			</div>
		</div>
		<div id="divAdminControlPanel">
			<%@ include file="TopStickyBar.jsp"%>
		</div>
		<noscript>
			<span>This page requires java-script to be enabled. Please
				adjust your browser-settings.</span>
		</noscript>

		<div id="sfOuterwrapper">
			<div class="sfSagewrapper">

				<!--Body Content-->
				<div class="sfContentwrapper clearfix">
					<div id="divCenterContent">
						<!-- Side Bar Starts-->
						<div class="sideBarLeft" id="divSideBar">
							<%@ include file="UserSideBar.jsp"%>
						</div>
						<!-- Side Bar Ends -->

						<div class="sfMaincontent">
							<div style="display: block" class="sfCpanel sfInnerwrapper"
								id="divBottompanel">
								<div class="sfModulecontent clearfix">
									<!-- Grid -->
									<div id="divProposalGrid" style="display: none">
										<div class="cssClassCommonBox Curve">
											<div class="cssClassHeader">
												<h1>
													<span>Manage Your Notifications</span>
												</h1>
												<div class="cssClassHeaderRight">
													<div class="sfButtonwrapper">
														<p>
															<button title="Add New Proposal" type="button"
																id="btnAddNew" class="sfBtn">
																<span class="icon-addnew">Add New Proposal</span>
															</button>
														</p>
														<p>
															<button title="Delete All Selected" type="button"
																id="btnDeleteSelected" class="sfBtn">
																<span class="icon-delete">Delete All Selected</span>
															</button>
														</p>
														<p>
															<button title="Export to Excel" type="button"
																id="btnExportToExcel" class="sfBtn">
																<span class="icon-excel">Export to Excel</span>
															</button>
														</p>
														<!-- <p>
															<button title="Export to CSV" type="button"
																id="btnExportToCSV" class="sfBtn">
																<span class="icon-excel">Export to CSV</span>
															</button>
														</p> -->

														<div class="cssClassClear"></div>
													</div>
													<div class="cssClassClear"></div>
												</div>
												<div class="cssClassClear"></div>
											</div>
											<div class="sfGridwrapper">
												<div class="sfGridWrapperContent">
													<div class="sfFormwrapper sfTableOption">
														<table width="100%" cellspacing="0" cellpadding="0"
															border="0">
															<tbody>
																<tr>
																	<td><label class="cssClassLabel">Project
																			Title:</label> <input title="Project Title" type="text"
																		class="sfTextBoxFix" id="txtSearchProjectTitle"
																		placeholder="Project Title" /></td>
																	<td><label class="cssClassLabel"> Proposed
																			By:</label> <input title="Proposed By"
																		id="txtSearchProposedBy" class="sfTextBoxFix"
																		type="text" placeholder="Proposed By" /></td>

																	<!-- 																		<td><label class="cssClassLabel"> -->
																	<!-- 																				Project Type:</label> <select title="Choose Project Type" id="ddlProjectType" -->
																	<!-- 																			class="sfListmenu" style="width: 100px;"> -->
																	<!-- 																				<option value="0" >--All--</option> -->
																	<!-- 																		</select></td> -->
																	<!-- 																		<td><label class="cssClassLabel"> -->
																	<!-- 																				Type of Request:</label> <select title="Choose Type of Request" id="ddlTypeOfRequest" -->
																	<!-- 																			class="sfListmenu" style="width: 100px;"> -->
																	<!-- 																				<option value="0" >--All--</option> -->
																	<!-- 																		</select></td> -->
																	<!-- 																		<td><label class="cssClassLabel"> -->
																	<!-- 																				Location of Project:</label> <select  title="Choose Location of Project" id="ddlLocationOfProject" -->
																	<!-- 																			class="sfListmenu" style="width: 100px;"> -->
																	<!-- 																				<option value="0" >--All--</option> -->
																	<!-- 																		</select></td> -->

																	<td style="width: 180px; float: left;"><label
																		class="cssClassLabel">Received On:</label>
																		<div>
																			<span class="cssClassLabel">From:</span> <input
																				type="text" title="Received On From"
																				id="txtSearchReceivedOnFrom" class="sfTextBoxFix"
																				placeholder="From">
																		</div>
																		<div>
																			<span class="cssClassLabel">To:</span> <input
																				type="text" title="Received On To"
																				id="txtSearchReceivedOnTo" class="sfTextBoxFix"
																				placeholder="To">
																		</div></td>

																	<td style="width: 180px;"><label
																		class="cssClassLabel">Total Costs:</label>
																		<div>
																			<span class="cssClassLabel">From:</span> <input
																				type="text" title="Total Costs From"
																				id="txtSearchTotalCostsFrom"
																				name="searchTotalCostsFrom" class="sfTextBoxFix"
																				placeholder="From">
																		</div>
																		<div>
																			<span class="cssClassLabel">To:</span> <input
																				type="text" title="Total Costs To"
																				id="txtSearchTotalCostsTo" name="searchTotalCostsTo"
																				class="sfTextBoxFix" placeholder="To">
																		</div></td>

																	<td><label class="cssClassLabel">Proposal
																			Status:</label> <select title="Choose Proposal Status"
																		id="ddlSearchProposalStatus" class="sfListmenu"
																		style="width: 80px;">
																			<option value="0">--All--</option>
																	</select></td>
																	<td><label class="cssClassLabel">&nbsp;</label>
																		<button title="Search Proposal" class="sfBtn"
																			id="btnSearchProposal" type="button">
																			<span class="icon-search">Search</span>
																		</button></td>
																</tr>
															</tbody>
														</table>
													</div>
													<div class="loading">
														<img id="ajaxLoader" src="" alt="Loading..."
															title="Loading..." />
													</div>
													<div class="log"></div>
													<table id="gdvProposals" cellspacing="0" cellpadding="0"
														border="0" width="100%"></table>
												</div>
											</div>
										</div>
									</div>
									<!-- End of Grid -->
									<!-- form -->
									<div id="divNotificationForm">
										<div class="cssClassCommonBox Curve">
											<div class="cssClassHeader">
												<h1>
													<span id="lblFormHeading">Manage Your Notifications</span>
												</h1>
												Goes here Your Notifications Details!
											</div>
										</div>
										<!-- End form -->
									</div>
								</div>
							</div>
							<!-- END sfMaincontent -->
						</div>
					</div>
					<!-- END Body Content sfContentwrapper -->
				</div>
			</div>
	</form>
</body>
</html>