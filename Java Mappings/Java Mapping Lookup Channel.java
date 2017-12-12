package com.bplus.telefonica.aeat.consulta.ws;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.lookup.Channel;
import com.sap.aii.mapping.lookup.LookupService;
import com.sap.aii.mapping.lookup.Payload;
import com.sap.aii.mapping.lookup.SystemAccessor;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;

public class ConsultaAEAT extends AbstractTransformation {

	// Tracing
	private MessageKey msgKey;
	private AuditAccess msgAuditAccessor;

	// Dinamyc Configuration
	DynamicConfiguration conf = null;
	private String directory;
	private String filename;

	private String id_version_sii = "";
	private String tipo_lt = "";
	private String id_lote = "";
	private String origen = "";
	private String tipo_comunicacion = "";
	private String juridica = "";
	private String nombreRazon;

	// Parametros del mapeo
	private static String filenameNombreRazon;
	private static String filenameTypeId;
	private static String channelName;
	private static String responseFolderPath;

	// En estos atributos se guardan los datos de las siguientes llamadas, Clave de
	// Paginacion del ultimo registro recuperado
	private String paginacionNombreRazon;
	private String paginacionNIF;
	private String paginacionPAIS;
	private String paginacionID;
	private String paginacionTypeID;
	private String paginacionNumSerie;
	private String paginacionFecha;
	private String paginacionIdBien;

	public void transform(TransformationInput arg0, TransformationOutput arg1) throws StreamTransformationException {

		String msgID = arg0.getInputHeader().getMessageId();
		iniAuditLog(msgID);
		logTrace("START MAPPING");

		conf = arg0.getDynamicConfiguration();

		// Get Filename from Dynamic
		DynamicConfigurationKey key = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/File", "FileName");
		filename = conf.get(key);
		logTrace("Filename :" + filename);

		// Get Path from Dynamic
		key = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/File", "Directory");
		directory = conf.get(key);
		logTrace("Directory :" + directory);

		// Get nombreRazon path
		filenameNombreRazon = arg0.getInputParameters().getString("nombreRazonPath");
		logTrace("NOMBRE RAZON PROPERTIES PATH: " + filenameNombreRazon);

		// Get TypeID path
		filenameTypeId = arg0.getInputParameters().getString("typeIdPath");
		logTrace("TYPE ID PROPERTIES PATH: " + filenameTypeId);

		// Get ChannelName for Consulta
		channelName = arg0.getInputParameters().getString("channelName");
		logTrace("Channel Name Value: " + channelName);

		// Get responseFolder for Consulta
		responseFolderPath = arg0.getInputParameters().getString("responseFolderPath");
		logTrace("Response Folder Path Name Value: " + responseFolderPath);

		this.execute(arg0.getInputPayload().getInputStream(), arg1.getOutputPayload().getOutputStream());
		logTrace("END MAPPING");
	}// end of transform

	public void execute(InputStream in, OutputStream out) throws StreamTransformationException {
		try {
			resetClavePaginacion();

			// Save doc in B64
			byte[] readFully = readFully(in);
			String base64 = new String(DatatypeConverter.printBase64Binary(readFully));
			String originalPayload = new String(readFully, "UTF-8");
			in.reset();

			// Save CSV Header Data
			getCSVData(in);
			in.reset();

			// Create B+ XML
			String request = generateBPlusXML(id_version_sii, origen, juridica, id_lote, tipo_lt, tipo_comunicacion,
					base64, juridica, filename, nombreRazon);

			// Con esta variable se indica si es necesario seguir llamando al WS
			boolean reCall = true;

			// Hacemos un lookup u otro en funcion de libro.
			logTrace("Recuperando canal comunicaciones consulta");
			Channel channel = LookupService.getChannel("BC_SII_BROKER", channelName);
			SystemAccessor accessor = null;
			accessor = LookupService.getSystemAccessor(channel);
			logTrace("Recuperando ...");
			if (accessor != null) {
				logTrace("Canal recuperado correctamente. " + channelName);
			}

			// Creamos un inputStream que pasar al lookup, asi generamos el payload
			InputStream inputStream = new ByteArrayInputStream(request.getBytes("UTF-8"));
			Payload callPayload = LookupService.getXmlPayload(inputStream);
			Payload SOAPOutPayload = null;
			DocumentBuilder builder = null;
			Document document = null;
			int i = 1;
			while (reCall) {
				boolean parseException = false;
				logTrace("Llamando al canal. " + channelName);
				SOAPOutPayload = accessor.call(callPayload);
				callPayload = null;
				logTrace("Recuperando payload del canal. " + channelName);
				InputStream inp = SOAPOutPayload.getContent();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				builder = factory.newDocumentBuilder();
				try {
					logTrace("PARSING RESPONSE START\n");
					document = builder.parse(inp);
					inp.reset();
					logTrace("PARSING RESPONSE END \n");
				} catch (Exception e) {
					// TODO: handle exception
					parseException = true;
				}
				if (!parseException) {
					NodeList resultNode = document.getElementsByTagName("siiLRRC:ResultadoConsulta");
					Node node = resultNode.item(0);
					if (node != null) {
						node = node.getFirstChild();
						if (node != null) {
							String status = node.getTextContent();
							if (status.equals("ConDatos")) {
								logTrace("RESPUESTA AEAT VALIDA. HAY MAS DATOS: " + status);
								String response = getXMLDocument(document);
								writeFile(responseFolderPath + "/" + filename.substring(0, filename.length() - 4)
										+ "_Response_" + i + ".csv", response);

								NodeList isFacturaNodes = document.getElementsByTagName("siiLRRC:IDFactura");
								int length = isFacturaNodes.getLength();
								logTrace("Nodos Recuperados: " + length);
								Node lastNode = isFacturaNodes.item(length - 1);

								for (int cont1 = 0; cont1 < lastNode.getChildNodes().getLength(); cont1++) {
									Node child = lastNode.getChildNodes().item(cont1);
									if (child.getNodeName().contains("IDEmisorFactura")) {
										for (int cont2 = 0; cont2 < child.getChildNodes().getLength(); cont2++) {
											Node child2 = child.getChildNodes().item(cont2);
											if (child2.getNodeName().contains("NIF")) {
												paginacionNIF = child2.getTextContent();
											} else if (child2.getNodeName().contains("IDOtro")) {
												for (int cont3 = 0; cont3 < child.getChildNodes()
														.getLength(); cont3++) {
													Node child3 = child2.getChildNodes().item(cont3);
													if (child3.getNodeName().contains("CodigoPais")) {
														paginacionPAIS = child3.getTextContent();
													} else if (child3.getNodeName().contains("IDType")) {
														paginacionTypeID = child3.getTextContent();
													} else if (child3.getNodeName().contains("ID")) {
														paginacionID = child3.getTextContent();
													}
												}
											}
										}
									} else if (child.getNodeName().contains("NumSerieFacturaEmisor")) {
										paginacionNumSerie = child.getTextContent();
									} else if (child.getNodeName().contains("FechaExpedicionFacturaEmisor")) {
										paginacionFecha = child.getTextContent();
									}
								}
								String payload = originalPayload + "|" + paginacionNombreRazon + "|" + paginacionNIF
										+ "|" + paginacionPAIS + "|" + paginacionID + "|" + paginacionTypeID + "|"
										+ paginacionNumSerie + "|" + paginacionFecha + "|" + paginacionIdBien;
								base64 = new String(DatatypeConverter.printBase64Binary(payload.getBytes("UTF-8")));
								request = generateBPlusXML(id_version_sii, origen, juridica, id_lote, tipo_lt,
										tipo_comunicacion, base64, juridica, filename, nombreRazon);
								resetClavePaginacion();
								InputStream stream = new ByteArrayInputStream(request.getBytes("UTF-8"));
								logTrace("Llamada Recursiva " + i + ": " + payload);
								callPayload = LookupService.getXmlPayload(stream);
								reCall = true;
							} else {
								logTrace("RESPUESTA AEAT VALIDA. NO HAY MAS DATOS: " + status);
								writeFile(responseFolderPath + "/" + filename.substring(0, filename.length() - 4)
										+ "_Response_" + i + ".csv", "CONSULTA FINALIZADA CON EXITO");
								reCall = false;
							}
						}
					} else {
						logTrace("RESPUESTA AEAT XML. NO VALIDA");
						writeFile(responseFolderPath + "/" + filename.substring(0, filename.length() - 4) + "_Response_"
								+ i + ".csv", "CONSULTA FINALIZADA CON ERROR");
						logTrace("Respuesta: " + new String(readFully(inp), "UTF-8"));
						reCall = false;
					}
				} else {
					logTrace("RESPUESTA AEAT NO XML. PARSE EXCEPTION");
					writeFile(responseFolderPath + "/" + filename.substring(0, filename.length() - 4) + "_Response_" + i
							+ ".csv", "CONSULTA FINALIZADA CON ERROR");
					logTrace("Respuesta: " + new String(readFully(inp), "UTF-8"));
					reCall = false;
				}
				i++;
			}

			// Document targetDoc = builder.newDocument();
			// Element targetRoot = (Element) targetDoc
			// .createElement("ns0:MT_Output");
			// targetRoot.setAttribute("xmlns:ns0",
			// "http://xxxxxxx/pi/468470/RestLookUp");
			// Element stat = (Element) targetDoc.createElement("status");
			// stat.setTextContent(status);
			// targetRoot.appendChild(stat);
			// targetDoc.appendChild(targetRoot);
			// DOMSource domSource = new DOMSource(targetDoc);
			// StreamResult result = new StreamResult(out);
			// TransformerFactory tf = TransformerFactory.newInstance();
			// Transformer transformer = tf.newTransformer();
			// transformer.transform(domSource, result);
		} catch (Exception e) {
			throw new StreamTransformationException("Error en JM ConsultaAEAT: " + e.getMessage());
		}
	} // end of execute

	private String generateBPlusXML(String version, String origen, String sociedad, String GUID, String operacion,
			String comunicacion, String base64, String nif, String filename, String nombreRazon)
			throws StreamTransformationException {
		logTrace("GenerateBPlusXML");
		String operacionDef = "NONE";
		String resultXMLAsString = "";
		try {
			operacionDef = getTipoOperacion(operacion, comunicacion);

			logTrace("Setting OperacionDef :" + operacion + " " + comunicacion);

			resultXMLAsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<n0:MT_Lote xmlns:n0=\"urn:techedgegroup.com:sii:lotes\">" + "<origen>" + origen + "</origen>"
					+ "<id_sociedad>" + sociedad + "</id_sociedad>" + "<versionSII>" + version + "</versionSII>"
					+ "<etiqueta_externa>"// Utilizamos este campo para guardar el filename original.
					+ filename + "</etiqueta_externa>" + "<guid>" + GUID + "</guid>" + "<titularNIF>" + nif
					+ "</titularNIF>" + "<titularNombreRazon>" + nombreRazon + "</titularNombreRazon>"
					+ "<tipoOperacion>" + operacionDef + "</tipoOperacion>" + "<tipoComunicacion>" + comunicacion
					+ "</tipoComunicacion>" + "<importeTotal>" + "</importeTotal>" + "<numRegistros>"
					+ "</numRegistros>" + "<docLoteB64>" + base64 + "</docLoteB64>" + "<folder>" + directory
					+ "</folder>" + "<filename>" + filename + "</filename>" + "</n0:MT_Lote>";
		} catch (Exception e) {
			// TODO: handle exception
			throw new StreamTransformationException("Error en generateBPlusXML: " + e.getMessage());
		}
		logTrace(resultXMLAsString);
		return resultXMLAsString;
	}

	private String getTipoOperacion(String operacion, String comunicacion) throws StreamTransformationException {
		String tipoOperacion = "";
		logTrace(operacion + " / " + comunicacion);
		if (operacion.contains("LRBI") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRBienesInversion";
		} else if (operacion.contains("LRDOI") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRDetOperacionIntracomunitaria";
		} else if (operacion.contains("LRFE") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRFacturasEmitidas";
		} else if (operacion.contains("LRFR") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRFacturasRecibidas";
		} else if (operacion.contains("LRFRPA") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRPagosRecibidas";
		} else if (operacion.contains("LRFECO") && comunicacion.equals("CONSULTA")) {
			tipoOperacion = "ConsultaLRCobrosEmitidas";
		} else {
			throw new StreamTransformationException(
					"Error en getTipoOperacion: IMPOSIBLE DETERMINAR OPERACION. NO ES CONSULTA");
		}

		return tipoOperacion;
	}

	private void getCSVData(InputStream in) throws StreamTransformationException {
		// Parse CSV Header
		int line = 0;
		Scanner scanner = new Scanner(in);
		// Linea a Linea
		while (scanner.hasNextLine() == true) {
			line++;
			String str = scanner.nextLine();
			// Contando columnas CSV
			if (line == 1) {
				// Leyendo Cabecera
				logTrace("GetCSVData");
				int cont = 0;
				for (String column : str.split("\\|")) {
					switch (cont) {
					case 1:
						id_version_sii = column;
						break;
					case 2:
						tipo_lt = column;
						break;
					case 3:
						id_lote = column;
						break;
					case 4:
						origen = column;
						break;
					case 5:
						tipo_comunicacion = column;
						break;
					case 6:
						juridica = column;
						break;
					}
					cont++;
				}
				break;
			}
		}
		nombreRazon = getProperty(juridica, filenameNombreRazon);
		if (nombreRazon == null || nombreRazon.equals("NotFound")) {
			throw new StreamTransformationException("Imposible obtener nombreRazon del : " + juridica);
		}
	}

	public static String getProperty(String propName, String filename) {
		Properties prop = new Properties();
		InputStream input = null;
		String value = "NotFound";
		try {
			input = new FileInputStream(filename);
			// Load Properties
			prop.load(input);
			// Get Property
			value = prop.getProperty(propName);

		} catch (Exception ex) {

		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return value;
	}

	public static byte[] readFully(InputStream input) throws IOException {
		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		return output.toByteArray();
	}

	public String getXMLDocument(Document doc) throws IOException, TransformerException {

		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(domSource, result);
		return writer.toString();
	}

	public void writeFile(String path, String payload) throws StreamTransformationException {
		try {
			// Whatever the file path is.
			File statText = new File(path);
			FileOutputStream is = new FileOutputStream(statText);
			OutputStreamWriter osw = new OutputStreamWriter(is);
			Writer w = new BufferedWriter(osw);
			w.write(payload);
			w.close();
		} catch (IOException e) {
			throw new StreamTransformationException("Error en writeFile: " + e.getMessage());
		}
	}

	public void resetClavePaginacion() {
		paginacionNombreRazon = "";
		paginacionNIF = "";
		paginacionPAIS = "";
		paginacionID = "";
		paginacionTypeID = "";
		paginacionNumSerie = "";
		paginacionFecha = "";
		paginacionIdBien = "";
	}

	/*
	 * Funciones para usar el message log
	 */
	private void iniAuditLog(String msgID) {

		// Para probar el mapping desde el ESR sin msgID
		if (msgID.length() < 32)
			msgID = "01234567890123456789012345678901";

		final String DASH = "-";
		String uuidTimeLow = msgID.substring(0, 8);
		String uuidTimeMid = msgID.substring(8, 12);
		String uuidTimeHighAndVersion = msgID.substring(12, 16);
		String uuidClockSeqAndReserved = msgID.substring(16, 18);
		String uuidClockSeqLow = msgID.substring(18, 20);
		String uuidNode = msgID.substring(20, 32);
		String msgUUID = uuidTimeLow + DASH + uuidTimeMid + DASH + uuidTimeHighAndVersion + DASH
				+ uuidClockSeqAndReserved + uuidClockSeqLow + DASH + uuidNode;

		msgKey = new MessageKey(msgUUID, MessageDirection.OUTBOUND);

		try {
			msgAuditAccessor = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void logTrace(String message) {
		msgAuditAccessor.addAuditLogEntry(msgKey, AuditLogStatus.SUCCESS, message);
	}
}