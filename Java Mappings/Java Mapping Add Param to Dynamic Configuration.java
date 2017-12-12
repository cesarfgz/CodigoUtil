package com.repsol.integracion.dynconf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class AddParameterDynConf extends AbstractTransformation {

	private DynamicConfiguration DynConfig;
	Logger logger;

	public void transform(TransformationInput arg0, TransformationOutput arg1)
			throws StreamTransformationException {

		try {
			logger = Logger.getLogger(AddParameterDynConf.class.getName());
			logger.severe("START MAPPING");

			// Get Mapping Params
			String dunsOrigen = arg0.getInputParameters().getString("DUNS_ORIGEN");

			// Create Dyn Config Key for Path
			DynConfig = arg0.getDynamicConfiguration(); // get theDynamicConfiguration
			DynamicConfigurationKey dunsOrigenKey = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/File", "dunsOrigen");
			DynConfig.put(dunsOrigenKey, dunsOrigen);

			execute(arg0.getInputPayload().getInputStream(), arg1
					.getOutputPayload().getOutputStream());
			logger.severe("END MAPPING");

		} catch (StreamTransformationException e) {
			throw e;
		}
	}

	public void execute(InputStream is, OutputStream os) throws StreamTransformationException {

		int n;
		byte[] buffer = new byte[1024];
		try {
			while ((n = is.read(buffer)) > -1) {
				os.write(buffer, 0, n);
			}

			os.close();
		} catch (IOException e) {
			throw new StreamTransformationException(e.getMessage());
		}

	}

}
