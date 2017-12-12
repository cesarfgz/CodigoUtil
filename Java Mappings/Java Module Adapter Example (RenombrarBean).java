package com.sap.renombrar;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
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
import com.sap.engine.interfaces.messaging.api.Action;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.tc.logging.Location;


@Stateless(mappedName = "RenombrarBean")
@TransactionManagement(TransactionManagementType.BEAN)
public class RenombrarBean implements SessionBean, Module{


    public RenombrarBean() {
    }
	
	
    public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
    	
		String SIGNATURE = "process(ModuleContext moduleContext, ModuleData inputModuleData)";
		Location location = null;
		AuditAccess audit = null;
		
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
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();// creating object for audit log
			key = new MessageKey(msg.getMessageId(), msg.getMessageDirection());
		
		} catch(Exception t) {
			t.printStackTrace();
			ModuleException me = new ModuleException("Error creating audit", t);
			throw me;			
		}
		
		try {
			key = new MessageKey(msg.getMessageId(), msg.getMessageDirection());
		
		} catch(Exception t) {
			t.printStackTrace();
			ModuleException me = new ModuleException("Error creating key", t);
			throw me;			
		}

		audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Inicio RenombrarBean");		
		
		// Ruta donde se encuentran los ficheros que hay que renombrar
    	String ruta = (String) moduleContext.getContextData("directorio");
    	String prefijo = (String) moduleContext.getContextData("prefijo");
    	String extension = (String) moduleContext.getContextData("extension");
    	
    	if(ruta == null) {
    		location.debugT(SIGNATURE,"Parámetro 'directorio' no establecido. Por favor revise la configuración del módulo.");
    		audit.addAuditLogEntry(key, AuditLogStatus.ERROR, "Parámetro 'directorio' no establecido. Por favor revise la configuración del módulo.");
			ModuleException me = new ModuleException();
			throw me;
    	}

    	
    	if(prefijo == null) {
    		location.debugT(SIGNATURE,"Parámetro 'prefijo' no establecido. Por favor revise la configuración del módulo.");
    		audit.addAuditLogEntry(key, AuditLogStatus.ERROR, "Parámetro 'prefijo' no establecido. Por favor revise la configuración del módulo.");
			ModuleException me = new ModuleException();
			throw me;
    	}

    	if(extension == null) {
    		location.debugT(SIGNATURE,"Parámetro 'extension' no establecido. Por favor revise la configuración del módulo.");
    		audit.addAuditLogEntry(key, AuditLogStatus.ERROR, "Parámetro 'extension' no establecido. Por favor revise la configuración del módulo.");
			ModuleException me = new ModuleException();
			throw me;
    	}

		audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Renombrando ficheros del directorio: " + ruta );		
		
		File dir = new File(ruta);		   
		String[] ficheros = dir.list();	
			   
		if (ficheros == null) {
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"No hay ficheros en el directorio especificado.");		
		
		} else {
			int numeroFicheros = ficheros.length;
			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Total ficheros = " + numeroFicheros);		

			String FACT = "FACT";			
			Date fecha1 = new Date();			  			   			  
			Calendar cal1 = Calendar.getInstance();

			int intmes = cal1.get(Calendar.MONTH) + 1;
			int intdia = cal1.get(Calendar.DATE);
			int inthora = cal1.get(Calendar.HOUR_OF_DAY);
			int intminuto = cal1.get(Calendar.MINUTE);
			int intsegundo = cal1.get(Calendar.SECOND);

			String anio = "" + cal1.get(Calendar.YEAR);
			String mes = "" + intmes;
			String dia = "" + intdia;
			String hora = "" + inthora;
			String minuto = "" + intminuto;
			String segundo = "" + intsegundo;
			
			int s = mes.length();
			if(s<2) {
				mes = "0" + mes;
			}
			
			s = dia.length();
			
			if(s<2) {
				dia = "0" + dia;
			}
			
			s = hora.length();
			
			if(s<2) {
				hora = "0" + hora;
			}
			
			s = minuto.length();
			
			if(s<2) {
				minuto = "0" + minuto;
			}
			
			s = segundo.length();
			
			if(s<2) {
				segundo = "0" + segundo;
			}

			String fecha = anio + mes + dia + hora + minuto; 

			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Fecha = " + fecha);		
			
			String snumeroFicheros = "" + numeroFicheros; 
			
			if(numeroFicheros<10) {
				snumeroFicheros = "0000" + snumeroFicheros;
			}
			
			if(9<numeroFicheros && numeroFicheros<100){
				snumeroFicheros = "000" + snumeroFicheros;
			}
			
			if(99<numeroFicheros && numeroFicheros<1000){
				snumeroFicheros = "00" + snumeroFicheros;
			}
			
			if(999<numeroFicheros && numeroFicheros<10000){
				snumeroFicheros = "0" + snumeroFicheros;
			}
			
			String nombreFichero = prefijo + "-" + fecha + "-" + snumeroFicheros + "-";  
			
			for (int x=0;x<ficheros.length;x++) {
				System.out.println(ficheros[x]);				
				// Cambiamos el nombre				
				int l = x+1;
				
				String orden = "" + l;
				if(l<10) {
					orden = "0000" + orden;
				}
				
				if(9<l && l<100){
					orden = "000" + orden;
				}
				
				if(99<l && l<1000){
					orden = "00" + orden;
				}
				
				if(999<l && l<10000){
					orden = "0" + orden;
				}				
								
				String nombreFicheroSalida = nombreFichero + orden + "." + extension;
				audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Nombre fichero de salida = " + nombreFicheroSalida);				
				File fichero = new File(ruta + "/" + ficheros[x]);			
	            File fichero2 = new File(ruta + "/" + nombreFicheroSalida);	 
	            boolean success = fichero.renameTo(fichero2);
	            if (!success) {
	        		location.debugT(SIGNATURE,"Error intentando cambiar el nombre de fichero.");
	        		audit.addAuditLogEntry(key, AuditLogStatus.ERROR, "Error intentando cambiar el nombre de fichero.");
	    			ModuleException me = new ModuleException();
	    			throw me;
	            }
			}
		}
		audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS,"Fin RenombrarBean");		
		return inputModuleData;//sending original payload to call adapter.

	}

	public void ejbActivate() throws EJBException, RemoteException {}
	public void ejbPassivate() throws EJBException, RemoteException {}
	public void ejbRemove() throws EJBException, RemoteException {}
	public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {}
	public void ejbTimeout(Timer arg0) {}
	public void ejbCreate() throws javax.ejb.CreateException {}
}
