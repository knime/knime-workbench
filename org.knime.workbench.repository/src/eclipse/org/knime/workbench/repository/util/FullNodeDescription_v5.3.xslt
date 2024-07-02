<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY css SYSTEM "style.css">
]>
<!-- Stylesheet for node descriptions introduced with KNIME 5.3 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:t="http://knime.org/node/v5.3"
                xmlns="http://www.w3.org/1999/xhtml">
    <xsl:param name="css" />
    <xsl:template match="t:knimeNode">
        <html>
            <head>
                <title>
                    Node description for
                    <xsl:value-of select="t:name" />
                </title>
                <style type="text/css"><xsl:value-of select="$css" /></style>
            </head>
            <body>
                <div class="fix-header" id="fix-header"><h1>
                    <xsl:value-of select="t:name" />
                </h1>
                </div>
                
                <xsl:if test="@deprecated = 'true'">
                    <h4 class="deprecated">Deprecated</h4>
                </xsl:if>
                <p>
                    <xsl:apply-templates select="t:fullDescription/t:intro/node()" />
                </p>

                <xsl:if test="t:fullDescription/t:option">
                    <h2>Dialog Options</h2>
                    <dl>
                        <xsl:apply-templates select="t:fullDescription/t:option" />
                    </dl>
                </xsl:if>

                <xsl:if test="t:fullDescription/t:tab">
                    <h2>Dialog Options</h2>
                    <xsl:apply-templates select="t:fullDescription/t:tab" />
                </xsl:if>

                <xsl:if test="t:fullDescription/t:link">
                    <h2>More Information</h2>
                    <dl>
                        Find more information here:
                    </dl>

                    <dd>
                        <ul>
                            <xsl:apply-templates select="t:fullDescription/t:link" />
                        </ul>
                    </dd>
                </xsl:if>

                <xsl:apply-templates select="t:interactiveView" />
                <xsl:apply-templates select="t:ports" />
                <xsl:apply-templates select="t:views" />

                <xsl:if test="osgi-info">
                <div id="origin-bundle">
                    This node is contained in <em><xsl:value-of select="osgi-info/@bundle-name" /></em>
                    provided by <em><xsl:value-of select="osgi-info/@bundle-vendor" /></em>.
                </div>
                </xsl:if>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template match="t:tab">
        <div class="group">
            <div class="groupname">
                <xsl:value-of select="@name" />
            </div>
            <xsl:apply-templates select="t:description" />
            <dl>
                <xsl:apply-templates select="t:option"/>
            </dl>
        </div>
    </xsl:template>

    <xsl:template match="t:description">
        <div class="group-description">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="t:option">
        <dt>
            <xsl:value-of select="@name" />
            <xsl:if test="@optional = 'true'">
                <span style="font-style: normal; font-weight: normal;"> (optional)</span>
            </xsl:if>            
        </dt>
        <dd>
            <xsl:apply-templates select="node()" />
        </dd>
    </xsl:template>

    <xsl:template match="t:link">
        <li>
            <a href="{@href}"><xsl:apply-templates select="node()" /></a>
        </li>
    </xsl:template>

    <xsl:template match="t:interactiveView">
        <h2>Interactive View: <xsl:value-of select="@name" /></h2>
        <div>
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="t:views[t:view]">
        <h2>Views</h2>
        <dl>
            <xsl:for-each select="t:view">
                <xsl:sort select="@index" data-type="number" />
                <dt>
                    <xsl:value-of select="@name" />
                </dt>
                <dd>
                    <xsl:apply-templates />
                </dd>
            </xsl:for-each>
        </dl>
    </xsl:template>


    <xsl:template match="t:ports">
        <h2>Ports</h2>
        <dl>
            <xsl:if test="t:inPort">
                <div class="group dlGroup">
                    <div class="groupname">Input Ports</div>
                    <table>
                        <xsl:for-each select="t:inPort">
                            <xsl:sort select="@index" data-type="number"/>
		                            <tr>
		                                <td class="dt">
		                                    <xsl:value-of select="@index" />
		                                </td>
		                                <td>
		                                    <xsl:apply-templates />
		                                </td>
		                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
            <xsl:if test="t:outPort">
                <div class="group dlGroup">
                    <div class="groupname">Output Ports</div>
                    <table>
                        <xsl:for-each select="t:outPort">
                            <xsl:sort select="@index" data-type="number"/>
                            <tr>
                                <td class="dt">
                                    <xsl:value-of select="@index" />
                                </td>
                                <td>
                                    <xsl:apply-templates />
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
			<xsl:if test="t:dynInPort">
                <div class="group">
                    <div class="groupname">Dynamic Input Ports</div>
                    <xsl:if test="count(t:dynInPort[@configurable-via-menu='false']) = 0">
                        <table>
                            <tr>
                                <td>
                                    Click on the three dots in the bottom left corner of the node or use the context 
                                    menu entry to add additional input ports.
                                </td>
                            </tr>
                        </table>
                    </xsl:if>
                    <table>
                        <xsl:for-each select="t:dynInPort">
                            <xsl:sort select="@insert-before" data-type="number"/>
                            <tr>
                                <td class="dt">
                                    <xsl:value-of select="@group-identifier" />
                                </td>
                                <td>
                                    <xsl:apply-templates />
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
            <xsl:if test="t:dynOutPort">
                <div class="group">
                    <div class="groupname">Dynamic Output Ports</div>
                    <xsl:if test="count(t:dynOutPort[@configurable-via-menu='false']) = 0">
	                    <table>
	                        <tr>
	                            <td>
	                                Click on the three dots in the bottom left corner of the node or use the context 
	                                menu entry to add additional output ports.
	                            </td>
	                        </tr>
	                    </table>
                    </xsl:if>
                    <table>
                        <xsl:for-each select="t:dynOutPort">
                            <xsl:sort select="@insert-before" data-type="number"/>
                            <tr>
                                <td class="dt">
                                    <xsl:value-of select="@group-identifier" />
                                </td>
                                <td>
                                    <xsl:apply-templates />
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
        </dl>
    </xsl:template>


    <xsl:template match="t:intro/t:table">
        <table class="introtable">
            <xsl:apply-templates />
        </table>
    </xsl:template>

    <xsl:template match="t:a[starts-with(@t:href, 'node:')]">
        <a href="http://127.0.0.1:51176/node/?bundle={/t:knimeNode/osgi-info/@bundle-symbolic-name}&amp;package={/t:knimeNode/osgi-info/@factory-package}&amp;file={substring-after(@href, 'node:')}">
        <xsl:apply-templates />
        </a>
    </xsl:template>

    <xsl:template match="t:a[starts-with(@t:href, 'bundle:')]">
        <a href="http://127.0.0.1:51176/bundle/?bundle={/t:knimeNode/osgi-info/@bundle-symbolic-name}&amp;file={substring-after(@href, 'bundle:')}">
        <xsl:apply-templates />
        </a>
    </xsl:template>


    <xsl:template match="*">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{local-name()}">
            <xsl:value-of select="." />
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="@*|node()" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>