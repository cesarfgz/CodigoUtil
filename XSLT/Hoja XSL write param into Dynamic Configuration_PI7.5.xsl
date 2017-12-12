<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
							xmlns:ns0="http://www.innovasport.com/mvp/schemas/retail/2017/v1.0.0/orderVANformatImage"
							xmlns:xsltc="http://xml.apache.org/xalan/xsltc"
							xmlns:map="java.util.Map"
							xmlns:dyn="com.sap.aii.mapping.api.DynamicConfiguration"
							xmlns:key="com.sap.aii.mapping.api.DynamicConfigurationKey"
							exclude-result-prefixes="ns0 xsl xsltc map dyn key">


	<xsl:output method="xml" indent="yes"/>
	

	<xsl:param name="inputparam"/>

	<xsl:template match="/">
	
	<!--Se usa la dynamic configuration para recuperar el parámetro con el idProveedor de Nike-->
	
	<xsl:variable name="dynamic-conf" select="map:get(xsltc:cast('java.util.Map',$inputparam), 'DynamicConfiguration')" />
	<xsl:variable name="dynamic-key" select="key:create('http://sap.com/xi/XI/System/File', 'numProvNike')" />
	<xsl:variable name="numProvNike" select="dyn:get(xsltc:cast('com.sap.aii.mapping.api.DynamicConfiguration',$dynamic-conf), $dynamic-key)" />
	
	
	</xsl:template>

</xsl:stylesheet>