<?xml version="1.0" encoding="UTF-8"?>
<!--
  raxRoles.xsl

  This stylesheet is responsible for transforming rax:roles found in
  resource or method attributes in a WADL into header params.  Header
  params are always placed at the method level.

  The header checks are instructed to always return a 403 with the
  message: "You are forbidden to perform the operation"

  Copyright 2014 Rackspace US, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:wadl="http://wadl.dev.java.net/2009/02"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:rax="http://docs.rackspace.com/api">
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!--
        Specifiing assertions on X-ROLES request header can really
        mess things up. Don't allow it.
    -->
    <xsl:template match="wadl:param[upper-case(@name)='X-ROLES' and @style='header' and not(ancestor::*/wadl:response)]">
      <xsl:message>[WARNING] When the rax-roles extension is enabled, you are not allowed to specify an X-ROLES header request parameter.</xsl:message>
      <xsl:message>[WARNING] The X-ROLES header parameter will be ignored.</xsl:message>
    </xsl:template>

    <xsl:template match="wadl:resource">
        <xsl:param name="roles" as="xsd:string*" select="()"/>

        <xsl:variable name="allRoles" as="xsd:string*">
            <xsl:sequence select="$roles"/>
            <xsl:if test="@rax:roles">
                <xsl:sequence select="tokenize(normalize-space(@rax:roles),' ')"/>
            </xsl:if>
        </xsl:variable>

        <xsl:copy>
            <xsl:apply-templates select="@* | node()">
                <xsl:with-param name="roles" select="$allRoles"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="wadl:method">
        <xsl:param name="roles" as="xsd:string*" select="()"/>

        <xsl:variable name="allRoles" as="xsd:string*">
          <xsl:sequence select="$roles"/>
          <xsl:if test="@rax:roles">
              <xsl:sequence select="tokenize(normalize-space(@rax:roles),' ')"/>
          </xsl:if>
        </xsl:variable>
        <xsl:copy>
          <!--
              If this method is a copy generated by normalization and
              it contains roles, then you *do not* want to continue to
              reference the source method.  This is because the method
              is now going te be different than the source!
          -->
          <xsl:choose>
              <xsl:when test="count($allRoles) != 0">
                  <xsl:apply-templates select="@*[not(local-name() = 'id') and not(namespace-uri() = 'http://docs.rackspace.com/api')]"/>
                  <!--
                      The label may be based of a rax:id so make sure
                      we copy it into a doc since we are removing rax:id.
                  -->
                  <xsl:if test="not(wadl:doc/@title) and @rax:id">
                      <wadl:doc title="{@rax:id}"/>
                  </xsl:if>
                  <xsl:if test="not(wadl:request)">
                      <wadl:request>
                          <xsl:call-template name="generateRoles">
                              <xsl:with-param name="roles" select="$allRoles"/>
                          </xsl:call-template>
                      </wadl:request>
                  </xsl:if>
              </xsl:when>
              <xsl:otherwise>
                  <xsl:apply-templates select="@*"/>
              </xsl:otherwise>
          </xsl:choose>
          <xsl:apply-templates select="node()">
              <xsl:with-param name="roles" select="$allRoles"/>
          </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="wadl:request">
        <xsl:param name="roles" as="xsd:string*" select="()"/>
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:if test="count($roles) != 0">
               <xsl:call-template name="generateRoles">
                  <xsl:with-param name="roles" select="$roles"/>
               </xsl:call-template>
            </xsl:if>
           <xsl:apply-templates select="node()">
              <xsl:with-param name="roles" select="$roles"/>
           </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="generateRoles">
        <xsl:param name="roles" as="xsd:string*" select="()"/>
        <xsl:choose>
            <xsl:when test="not('#all' = $roles)">
                <xsl:variable name="escaped" as="xsd:string*" select="for $r in $roles return replace($r, '&apos;&apos;', '&apos;&apos;&apos;&apos;')"/>
                <xsl:variable name="quoted" as="xsd:string*" select="for $r in $escaped return concat('&apos;&apos;', rax:transformNBSP($r), '&apos;&apos;')"/>
                <xsl:variable name="ordered" as="xsd:string*"><xsl:perform-sort select="$quoted"><xsl:sort/></xsl:perform-sort></xsl:variable>
                <xsl:variable name="sequence" as="xsd:string"><xsl:value-of select="$ordered" separator=", "/></xsl:variable>
                <rax:captureHeader name="X-RELEVANT-ROLES" path="for $h in req:headers('X-ROLES', true()) return if ($h = ({$sequence})) then $h else ()" />
                <xsl:for-each select="$roles">
                    <wadl:param name="X-ROLES" style="header" rax:code="403" rax:anyMatch="true"
                                rax:message="You are forbidden to perform the operation" type="xsd:string"
                                required="true" repeating="true">
                        <xsl:attribute name="fixed" select="rax:transformNBSP(.)"/>
                    </wadl:param>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <rax:captureHeader name="X-RELEVANT-ROLES"
                                   path="req:headers('X-ROLES', true())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:function name="rax:transformNBSP" as="xsd:string">
      <xsl:param name="in" as="xsd:string"/>
      <xsl:value-of select="replace($in,'&#xA0;',' ')"/>
    </xsl:function>
</xsl:stylesheet>
