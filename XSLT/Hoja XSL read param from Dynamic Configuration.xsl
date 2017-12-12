<?xml version='1.0' ?>
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:map="java:java.util.Map"
  xmlns:dyn="java:com.sap.aii.mapping.api.DynamicConfiguration"
  xmlns:key="java:com.sap.aii.mapping.api.DynamicConfigurationKey">

	<xsl:output indent="no" />
	<xsl:param name="inputparam"/>


	<xsl:template match="/">

		<!-- change dynamic configuration -->
		<xsl:variable name="dynamic-conf" select="map:get($inputparam, 'DynamicConfiguration')" />
		<xsl:variable name="dynamic-key" select="key:create('http://sap.com/xi/XI/System/File', 'dunsOrigen')" />
		<xsl:variable name="dynamic-value" select="dyn:get($dynamic-conf, $dynamic-key)" />

		<ns0:MT_DUMMY_FACTURAE xmlns:ns0="urn:repsol.com:marketplace:comun">
			<Dummy>
				<xsl:value-of select="$dynamic-value"/>
			</Dummy>
		</ns0:MT_DUMMY_FACTURAE>
	</xsl:template>

</xsl:stylesheet>