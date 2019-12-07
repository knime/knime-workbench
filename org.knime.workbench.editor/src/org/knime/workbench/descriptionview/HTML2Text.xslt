<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:xhtml="http://www.w3.org/1999/xhtml">

	<xsl:template match="xhtml:title"/>
	<xsl:template match="xhtml:style"/>
	
	<xsl:template match="xhtml:h1">
		<xsl:apply-templates /> 
	</xsl:template>
	
	<xsl:template match="xhtml:h2">
		<xsl:apply-templates />
	</xsl:template>
	
	<xsl:template match="xhtml:h3">
		<xsl:apply-templates /> 
	</xsl:template>
	
	<xsl:template match="xhtml:h4">
		<xsl:apply-templates /> 
	</xsl:template>		
	
	<xsl:template match="xhtml:p">
		<xsl:apply-templates /> 
	</xsl:template>
		
	<xsl:template match="xhtml:ul">
		<xsl:for-each select="xhtml:li">
			<xsl:apply-templates /> 
		</xsl:for-each>
	</xsl:template>
		
	<xsl:template match="xhtml:ol">
		<xsl:for-each select="xhtml:li">
			<xsl:apply-templates /> 
		</xsl:for-each>
	</xsl:template>	

</xsl:stylesheet>