<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:map="java:java.util.Map"
  xmlns:dyn="java:com.sap.aii.mapping.api.DynamicConfiguration"
  xmlns:key="java:com.sap.aii.mapping.api.DynamicConfigurationKey">

	<xsl:output indent="no" />
	<xsl:param name="inputparam"/>


	<xsl:template match="/">

		<!-- change dynamic configuration -->
		<xsl:variable name="dynamic-conf"  
        select="map:get($inputparam, 'DynamicConfiguration')" />
		<xsl:variable name="dynamic-key"   
        select="key:create('http://sap.com/xi/XI/System/File', 'Directory')" />
		<xsl:variable name="dynamic-value" 
        select="dyn:get($dynamic-conf, $dynamic-key)" />
		<xsl:variable name="new-value"     
        select="concat($dynamic-value, 'subfolder\')" />
		<xsl:variable name="dummy" 
        select="dyn:put($dynamic-conf, $dynamic-key, $new-value)" /> 

		<!-- copy payload -->
		<xsl:copy-of select="." />
	</xsl:template>

</xsl:stylesheet>