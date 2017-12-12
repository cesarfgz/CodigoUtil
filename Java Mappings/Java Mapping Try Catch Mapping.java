public String contenido  = "";
public String nombreFichero = "";

@Override       
public void transform(TransformationInput in, TransformationOutput out)  
                                  throws StreamTransformationException {

	super.transform(in,out);                      

	String mensaje = "Inicio proceso Vector I450B.";
	// Obtengo el msgID
	String msgID = in.getInputHeader().getMessageId();					
	ErrorLog errorLog = new ErrorLog();
	InfoLog infoLog = new InfoLog();

	String codError = "";
	String respuesta = "";


	try
	{  


           mensaje = mensaje + " ";           
					mensaje = mensaje + "Fichero: " + nombreFichero;
           infoLog.logInfo(mensaje,msgID);
           infoLog = new InfoLog();
        
					String aux  = contenido;

					mensaje = "Contenido: " + aux;

           infoLog.logInfo(mensaje,msgID);
           infoLog = new InfoLog();

       try
       {
     			// Invocamos al iflow_vectorI450B_respuestaBPM_SGM_SOAP
          infoLog = new InfoLog();

          mensaje =  "Se realiza la llamada al iflow_vectorI450B_respuestaBPM_SGM_SOAP";
			   mensaje = mensaje + " ";
          infoLog.logInfo(mensaje,msgID);
          // Pasamos el BS y el CC como entrada
			  Channel channel = LookupService.getChannel("BPM_VECTOR_I450B","CC_R_SOAP_VECTOR_I450B_SGM_SOAP");
			  SystemAccessor accessor = LookupService.getSystemAccessor(channel);
         String SOAPxml ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:repsol.com:marketing:vector\"><soapenv:Header/><soapenv:Body><urn:MT_bdsgm_I450B><Statement><USP_SIES_INS_INTERFASESARC action=\"EXECUTE\">"
               																				+"<NombreArchivo isInput=\"true\" type=\"VARCHAR\">" + nombreFichero + "</NombreArchivo>"
               																				+"<Contenido isInput=\"true\" type=\"VARCHAR\"><![CDATA[" + aux + "]]></Contenido>"
               																				+"<CodigoError isOutput=\"true\" type=\"VARCHAR\"></CodigoError><DescripcionError isOutput=\"true\" type=\"VARCHAR\"></DescripcionError></USP_SIES_INS_INTERFASESARC></Statement></urn:MT_bdsgm_I450B></soapenv:Body></soapenv:Envelope>";

			
          mensaje = "";
          infoLog = new InfoLog();
          mensaje = "Peticion SOAP: " + SOAPxml;
			   infoLog.logInfo(mensaje,msgID);
          
			  InputStream inputStream = new ByteArrayInputStream(SOAPxml.getBytes());
			  XmlPayload payloadIn = LookupService.getXmlPayload(inputStream);
			  Payload soapOutPayload = null;
			  // Hacemos la llamada al WS y obtenemos la respuesta en
			  soapOutPayload = accessor.call(payloadIn);
          // Parseamos la respuesta
			  InputStream inp = soapOutPayload.getContent();

          infoLog = new InfoLog();

				// convertimos la respuesta en un string
         BufferedReader reader = new BufferedReader(new InputStreamReader(inp));
         StringBuilder sb = new StringBuilder();
         String line = null;
         while ((line = reader.readLine()) != null) {
             sb.append(line);
         }
         inp.close();
         respuesta = sb.toString();

          mensaje = "Respuesta: " + respuesta;	
			   infoLog.logInfo(mensaje,msgID);



         // Recuperamos el valor del Código de Error de la respuesta
         int inicioCodError = respuesta.indexOf("<CodigoError>");
         int finCodError = respuesta.indexOf("</CodigoError>");
         if(inicioCodError>0)
         {
							  codError = respuesta.substring(inicioCodError+13,finCodError);
         				mensaje = "Código de Error de la Respuesta: ";
			  				mensaje = mensaje + codError;
                infoLog = new InfoLog();
								infoLog.logInfo(mensaje,msgID);
         }

       }
       catch(Exception e)
      {
        		 mensaje = "Se ha producido una excepción en la invocación a iflow_vectorI450B_respuestaBPM_SGM_SOAP.";
             mensaje = mensaje + e.getMessage();
					  errorLog = new ErrorLog();
             errorLog.logError(mensaje,msgID);
             StreamTransformationException streamTransformationException = new StreamTransformationException("Excepción Proceso Invocación iflow_vectorI450B_respuestaBPM_SGM_SOAP",e);
             throw streamTransformationException;
      }

		 // según el valor del codError
       // si es 130 generamos una respuesta invocando al  iflow_vectorI450B_respuestaBPM_Mail_SOAP
      if("130".equals(codError))
      {

          // Obtenemos el mensaje pasar
				 int inicioStatementResponse = respuesta.indexOf("<Statement_response>");
          int finStatementResponse = respuesta.indexOf("</Statement_response>");
          String res = "";
          if(inicioStatementResponse>0)
          {
					 		res = respuesta.substring(inicioStatementResponse+20,finStatementResponse);
           }
          // Pasamos el BS y el CC como entrada
			  Channel channel = LookupService.getChannel("BPM_VECTOR_I450B","CC_R_SOAP_VECTOR_I450B_MAIL_SOAP");
			  SystemAccessor accessor = LookupService.getSystemAccessor(channel);
         String SOAPxml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:repsol.com:marketing:vector\">"
   																												+"<soapenv:Header/>"
  																												+"<soapenv:Body>"
																												+"<urn:MT_bdsgm_I450B_response>"
         																								+"<Statement_response>"
                                                 + res
            																						+"</Statement_response>"
      																										+"</urn:MT_bdsgm_I450B_response>"
   																												+"</soapenv:Body></soapenv:Envelope>";
					

           mensaje = "Código error 130. Procedemos a invocar al iflow_vectorI450B_respuestaBPM_Mail_SOAP. Petición Soap Mail: ";
	  		  mensaje = mensaje + SOAPxml;
           infoLog = new InfoLog();
    				 infoLog.logInfo(mensaje,msgID);
         


         InputStream inputStream = new ByteArrayInputStream(SOAPxml.getBytes());
			  XmlPayload payloadIn = LookupService.getXmlPayload(inputStream);
			  Payload soapOutPayload = null;
			  // Hacemos la llamada al WS y obtenemos la respuesta en
	
			  // Hacemos la llamada al WS y obtenemos la respuesta en
			  soapOutPayload = accessor.call(payloadIn);
          
      }

       // si  no es 130 finalizamos.
  
  }
  catch(Exception e)
  { 
		mensaje = mensaje + "Se ha producido una excepción.";
      mensaje = mensaje + e.getMessage();
	  errorLog = new ErrorLog();
      errorLog.logError(mensaje,msgID);
      StreamTransformationException streamTransformationException = new StreamTransformationException("Excepción",e);
      throw streamTransformationException;
      
  }


}