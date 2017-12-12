package com.sap.adaptermodule;


import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;


import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;

//XML parsing and transformation classes
import javax.xml.parsers.*;


//Classes for Module development & Trace
import com.sap.aii.af.lib.mp.module.*;
import com.sap.engine.interfaces.messaging.api.*;
import com.sap.engine.interfaces.messaging.api.auditlog.*;
import com.sap.tc.logging.*;


/**
 * @author Srinivas Vanamala
 *
 */
public class SavePayloadBean implements SessionBean, Module  {

	private static final String VERSION_ID = "$Id://tc/aii/30_REL/src/_adapters/_sample/java/user/module/SavePayloadBean.java#1 $";
	private static final long serialVersionUID = 7435850550539048631L;
	private final String USER_AGENT = "Mozilla/5.0";
	
	private SessionContext myContext;
	Location TRACE = null;
	MessageKey key = null;
	AuditAccess audit = null;
	int MAX_KEYS = 500;
	
	public void ejbRemove(){}
	public void ejbActivate(){}
	public void ejbPassivate(){}
	public void setSessionContext(SessionContext context){myContext = context;}
	public void ejbCreate() throws CreateException{}
	
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
		String SIGNATURE = "process(ModuleContext, ModuleData)";
		
		//These are two parameters that can be passed to the module in adapter parameters.
		boolean skipExecution = getBooleanParam(moduleContext, "skipExecution"); //skipexecution of this module if set to 'true'
		boolean failOnError = getBooleanParam(moduleContext, "failOnError"); //Will fail the module and stop process if set to 'true'
		
		
		// Open the LOG File to write the TRACE
		try {
			TRACE = Location.getLocation(this.getClass().getName());
			
		}catch (Exception t) {
			t.printStackTrace();
			if (failOnError) {
				ModuleException me = new ModuleException("Unable to create trace location(TRACE)", t);
				throw me;
		    }
			return inputModuleData;
		}
		
		TRACE.entering(SIGNATURE, new Object[] { moduleContext, inputModuleData });
		
		//Check if the message is empty or not
	    Object o = inputModuleData.getPrincipalData();
	    if (o == null){
	      String error = "[E1] ModuleData contains null XI message";
	      TRACE.errorT(SIGNATURE, error);
	      
		  if (failOnError){
			ModuleException me = new ModuleException(error);
	        TRACE.throwing(SIGNATURE, me);
	        throw me;
	      }

	      TRACE.warningT(SIGNATURE, "failOnError false, ignore missing PI message and return");
	      return inputModuleData;
	    }
		
	    //Extract the message details and actual message
	    Message message = null;
	    
		try {
	      message = (Message)o;     
	    
		} catch (ClassCastException e) {
	      TRACE.catching(SIGNATURE, e);
	      String error = "[E2] ModuleData does not contain an object that implements the XI message interface; object class is: " + o.getClass().getName();

	      TRACE.errorT(SIGNATURE, error);
	      if (failOnError) {
	        ModuleException me = new ModuleException("SavePayloadBean - " + error, e);
	        TRACE.throwing(SIGNATURE, me);
	        throw me;
	      }

	      TRACE.warningT(SIGNATURE, "failOnError false, " + error);
	      return inputModuleData;
	    }		

	    //Get the instance for Audit
	    try{
		      key = new MessageKey(message.getMessageId(), message.getMessageDirection());
		      audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();	 
		      info(SIGNATURE, "SAVEPAYLOAD: Module called");		      
	    } catch(Exception ee) {
	    	  TRACE.catching(SIGNATURE, ee);
		      String error = "[E3] Unable to get the audit trace instance" + ee;

		      TRACE.errorT(SIGNATURE, error);
		      if (failOnError) {
		        ModuleException me = new ModuleException("SavePayloadBean - " + error, ee);
		        TRACE.throwing(SIGNATURE, me);
		        throw me;
		      }

		      TRACE.warningT(SIGNATURE, "failOnError false, " + error);
		      return inputModuleData;	    	
	    }
	    
		/*
		 * ACTUAL CODE STARTS FROM HERE
		 */	    
	    
	    try{
			if(skipExecution){
		      info(SIGNATURE, "SavePayload - Skipping execution");
		      TRACE.exiting(SIGNATURE, inputModuleData);
		      return inputModuleData;
			} /********** [skipExecution = TRUE] - END OF PROGRAM **********/
			
			//Validation completed
			info(SIGNATURE, "SavePayload: Started Logic");
			/************ MODULE LOGIC STARTS HERE ********/
			
			
	    } catch (Exception ee){
		    	
	    	  TRACE.catching(SIGNATURE, ee);
		      String error = "[E4] Failed: Error in the Main Logic" + ee;

		      TRACE.errorT(SIGNATURE, error);
		      info(SIGNATURE, error);
		      
		      if (failOnError)
		      {
		        ModuleException me = new ModuleException("SavePayloadBean - " + error, ee);
		        TRACE.throwing(SIGNATURE, me);
		        throw me;
		      }
		      
		      return inputModuleData;
		}
			
	    info(SIGNATURE, "SavePayload: End Logic Successfully!");    		
	    TRACE.exiting(SIGNATURE, inputModuleData);
		return inputModuleData;	    
		
	}
	
	
	public boolean getBooleanParam(ModuleContext moduleContext, String paramField){
		boolean bparamValue = false;
		try{
			String paramValue = moduleContext.getContextData(paramField);
			if( paramValue != null && !paramValue.equals( "" )){
				bparamValue = Boolean.parseBoolean( paramValue );
			}
		} catch(Exception ee){	bparamValue=false;	}
		return bparamValue;
	}
	
	public static String getParameter(ModuleContext context, String name, String sDefault){
	    if ((name == null) || (name.equals(""))) {
	      return sDefault;
	    }
		
	    String val = context.getContextData(name);
	    if (val == null || val.equals("") ) {
	      return sDefault;
	    }
		
	    return val;
	 }	
	
	private void info(String signature, String msg){
	    TRACE.infoT(signature, msg);
	    this.audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS, "SAVE PAYLOAD Module: INFO: " + msg);
	}
	
	private void warning(String signature, String msg){
	    TRACE.warningT(signature, msg);
	    this.audit.addAuditLogEntry(key, AuditLogStatus.WARNING, "SAVE PAYLOAD Module: WARNING: " + msg);
	}
	
	private void error(String signature, String msg){
	    TRACE.errorT(signature, msg);
	    this.audit.addAuditLogEntry(key, AuditLogStatus.ERROR, "SAVE PAYLOAD Module: ERROR: " + msg);
	}	

}