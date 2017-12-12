package com.sap.emailattachment;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TimedObject;
import javax.ejb.Timer; 
import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.service.auditlog.Audit;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.tc.logging.Location;

/**
 *@ejbHome <{com.sap.aii.af.mp.module.ModuleHome}>
 *@ejbLocal <{com.sap.aii.af.mp.module.ModuleLocal}>
 *@ejbLocalHome <{com.sap.aii.af.mp.module.ModuleLocalHome}>
 *@ejbRemote <{com.sap.aii.af.mp.module.ModuleRemote}>
 *@stateless
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class EmailAttachBean implements SessionBean, Module{

	public static final String VERSION_ID ="$Id://tc/aii/30_REL/src/_adapters/_sample/java/user/module/NGMINB_validateBean.java#1 $";

	static final long serialVersionUID = 7435850550539048633L;
	String fileName = null;
	@SuppressWarnings("deprecation")

	
    /**
     * Default constructor. 
     */
    public EmailAttachBean() {
        // TODO Auto-generated constructor stub
    }
	
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {

		String SIGNATURE = "process(ModuleContext moduleContext, ModuleData inputModuleData)";
		Location location = null;
		AuditAccess audit = null;
		Payload attName =null;
		int attnum=0;
		
		try {
			location = Location.getLocation(this.getClass().getName());

		} catch (Exception t) {
			t.printStackTrace();
			ModuleException me = new ModuleException("Unable to create trace location", t);
			throw me;
		}

		Object obj = null;
		Message msg = null;
		MessageKey key = null;

		try {
			obj = inputModuleData.getPrincipalData();
			msg = (Message) obj;
			key = new MessageKey(msg.getMessageId(), msg.getMessageDirection());
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();// creating object for audit log
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS, "EmailAttach: Module called");
			fileName = msg.getMessageProperty("http://sap.com/xi/XI/System/File", "FileName");
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"RequestFileName:" + fileName);
			
			//Guardo el Payload como attachment
			/*
			Payload payload = msg.getMainPayload();			
			payload.setName(fileName);			
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"setting payload as attachment");
			msg.addAttachment(payload);
			*/
			
			attnum=msg.countAttachments();			
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"number of Attachment: " + attnum);
			
			
			attName = msg.getAttachment("%F");
			msg.removeAttachment("%F");
			attnum=msg.countAttachments();
			if(attName != null) {
				audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Name of Attachment: " + attName.getName());
			
			} else {
				audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Attachment not found" + attName);
			}                   
			
			try {
				attName.setName(fileName);
				audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Attachment name is successfully changed to : " + attName.getName());
			
			} catch(Exception e) {
				audit.addAuditLogEntry(key, AuditLogStatus.ERROR,attName +": attachmentName is not matching");
				ModuleException me = new ModuleException(e);
				throw me;
			}        
			
			msg.addAttachment(attName);
		    
		} catch (Exception e) {
			ModuleException me = new ModuleException(e);
			throw me; 
		}                      

		return inputModuleData;//sending original payload to call adapter.

	}

	public void ejbActivate() throws EJBException, RemoteException {}
	public void ejbPassivate() throws EJBException, RemoteException {}
	public void ejbRemove() throws EJBException, RemoteException {}
	public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {}
	public void ejbTimeout(Timer arg0) {}
	public void ejbCreate() throws javax.ejb.CreateException {}

}
