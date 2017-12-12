<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope">
	<xsl:output indent="yes" encoding="UTF-8" />
	<xsl:strip-space elements="*"/>

	<xsl:template match="/soapenv:Envelope">
		<xsl:copy-of select="/soapenv:Envelope/soapenv:Body/*"/>
	</xsl:template>

</xsl:stylesheet>
