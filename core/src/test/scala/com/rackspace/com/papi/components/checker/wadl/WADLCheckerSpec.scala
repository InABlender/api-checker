/***
 *   Copyright 2014 Rackspace US, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rackspace.com.papi.components.checker.wadl

import com.rackspace.com.papi.components.checker.TestConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.xml._

@RunWith(classOf[JUnitRunner])
class WADLCheckerSpec extends BaseCheckerSpec {

  //
  //  Register some common prefixes, you'll need the for XPath
  //  assertions.
  //
  register ("xsd", "http://www.w3.org/2001/XMLSchema")
  register ("wadl","http://wadl.dev.java.net/2009/02")
  register ("xsl","http://www.w3.org/1999/XSL/Transform")
  register ("chk","http://www.rackspace.com/repose/wadl/checker")
  register ("tst","http://www.rackspace.com/repose/wadl/checker/step/test")

  feature ("The WADLCheckerBuilder can correctly transforma a WADL into checker format") {

    info ("As a developer")
    info ("I want to be able to transform a WADL which references multiple XSDs into a ")
    info ("a description of a machine that can validate the API in checker format")
    info ("so that an API validator can process the checker format to validate the API")

    scenario("The WADL does not contain any resources") {
      Given("a WADL with no resources")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource/>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain a single start node")
      assert (checker, "count(//chk:step[@type='START']) = 1")
      And("The only steps accessible from start should be the fail states")
      val path = allStepsFromStart(checker)
      assert (path, "count(//chk:step) = 3")
      assert (path, "/chk:checker/chk:step[@type='START']")
      assert (path, "/chk:checker/chk:step[@type='METHOD_FAIL']")
      assert (path, "/chk:checker/chk:step[@type='URL_FAIL']")
      And("There should exist a direct path from start to each failed state")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
    }

    scenario("The WADL contains an explicit root element, with no methods on the root") {
      Given("a WADL with an explicit root element, no methods on root")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="/">
                <resource path="element">
                    <method name="GET">
                        <response status="200"/>
                    </method>
                </resource>
              </resource>
           </resources>
        </application>
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain an URL node only for the element")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 1")
      And ("The checker should contain a single GET method")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
      And ("The checker should NOT contian URL steps with a match == '/'")
      assert (checker, "count(/chk:checker/chk:step[@type='URL' and @match='/']) = 0")
      And ("The URL path should simply be from Start to the element")
      assert (checker, Start, URL("element"), Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("element"), URLFail)
      assert (checker, URL("element"), MethodFail)
    }

    scenario("The WADL contains an explicit root element, with methods on the root") {
      Given("a WADL with an explicit root element, methods on root")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="/">
                <method name="GET">
                    <response status="200"/>
                </method>
                <resource path="element">
                    <method name="GET">
                        <response status="200"/>
                    </method>
                </resource>
              </resource>
           </resources>
        </application>
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain an URL node only for the element")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 1")
      And ("The checker should contain two GET methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 2")
      And ("The checker should NOT contian URL steps with a match == '/'")
      assert (checker, "count(/chk:checker/chk:step[@type='URL' and @match='/']) = 0")
      And ("The URL path should exist from Start to the element")
      assert (checker, Start, URL("element"), Method("GET"))
      And ("The URL path should exist from START directly to GET method")
      assert (checker, Start, Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("element"), URLFail)
      assert (checker, URL("element"), MethodFail)
    }

    scenario("The WADL contains a / path deeper in the URI structure") {
      Given("a WADL that contains a / path deefer in the URI structure")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="/">
                <method name="GET">
                    <response status="200"/>
                </method>
                <resource path="element">
                    <resource path="/">
                       <method name="GET">
                          <response status="200"/>
                       </method>
                    </resource>
                </resource>
              </resource>
           </resources>
        </application>
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain an URL node only for the element")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 1")
      And ("The checker should contain two GET methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 2")
      And ("The checker should NOT contian URL steps with a match == '/'")
      assert (checker, "count(/chk:checker/chk:step[@type='URL' and @match='/']) = 0")
      And ("The URL path should exist from Start to the element")
      assert (checker, Start, URL("element"), Method("GET"))
      And ("The URL path should exist from START directly to GET method")
      assert (checker, Start, Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("element"), URLFail)
      assert (checker, URL("element"), MethodFail)
    }

    scenario("The WADL contains a / path deeper in the URI structure (with sub elements)") {
      Given("a WADL that contains a / path deeper in the URI structure")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="/">
                <method name="GET">
                    <response status="200"/>
                </method>
                <resource path="element">
                    <resource path="/">
                       <method name="GET">
                          <response status="200"/>
                       </method>
                       <resource path="element2">
                          <method name="POST">
                            <response status="200"/>
                          </method>
                       </resource>
                    </resource>
                </resource>
              </resource>
           </resources>
        </application>
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain an URL node and element2")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 2")
      And ("The checker should contain two GET methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 2")
      And ("The checker should contain a POST methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 1")
      And ("The checker should NOT contian URL steps with a match == '/'")
      assert (checker, "count(/chk:checker/chk:step[@type='URL' and @match='/']) = 0")
      And ("The URL path should exist from Start to the element")
      assert (checker, Start, URL("element"), Method("GET"))
      And ("The URL path should exist from Start to the element2")
      assert (checker, Start, URL("element"), URL("element2"), Method("POST"))
      And ("The URL path should exist from START directly to GET method")
      assert (checker, Start, Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("element"), URLFail)
      assert (checker, URL("element"), MethodFail)
      assert (checker, URL("element2"), URLFail)
      assert (checker, URL("element2"), MethodFail)
    }


    scenario("The WADL contains a / path deeper in the URI structure (with sub elements and multiple nested '/')") {
      Given("a WADL that contains a / path deeper in the URI structure")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="/">
                <method name="GET">
                    <response status="200"/>
                </method>
                <resource path="element">
                    <resource path="/">
                      <resource path="/">
                        <method name="GET">
                           <response status="200"/>
                        </method>
                        <resource path="element2">
                          <method name="POST">
                            <response status="200"/>
                          </method>
                        </resource>
                     </resource>
                    </resource>
                </resource>
              </resource>
           </resources>
        </application>
      val checker = builder.build (inWADL, stdConfig)
      Then("The checker should contain an URL node and element2")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 2")
      And ("The checker should contain two GET methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 2")
      And ("The checker should contain a POST methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 1")
      And ("The checker should NOT contian URL steps with a match == '/'")
      assert (checker, "count(/chk:checker/chk:step[@type='URL' and @match='/']) = 0")
      And ("The URL path should exist from Start to the element")
      assert (checker, Start, URL("element"), Method("GET"))
      And ("The URL path should exist from Start to the element2")
      assert (checker, Start, URL("element"), URL("element2"), Method("POST"))
      And ("The URL path should exist from START directly to GET method")
      assert (checker, Start, Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("element"), URLFail)
      assert (checker, URL("element"), MethodFail)
      assert (checker, URL("element2"), URLFail)
      assert (checker, URL("element2"), MethodFail)
    }

    //
    //  The following scenarios test a single resource located at
    //  /path/to/my/resource with a GET, DELETE, and POST method. They are
    //  equivalent but they are written in slightly different WADL
    //  form the assertions below must apply to all of them.
    //  Only application/xml is allowed in the POST operation
    //
    //

    def singlePathAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 4")
      And ("The checker should contain a GET, DELETE, and POST method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='DELETE']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='POST']")
      And ("The path from the start should contain all URL nodes in order")
      And ("it should end in the GET and a DELETE method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("DELETE"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), ReqType("(application/xml)(;.*)?"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      And ("The POST method should contain an ReqTypeFail")
      assert (checker, Method("POST"), ReqTypeFail)
    }

    scenario("The WADL contains a single multi-path resource") {
      Given("a WADL that contains a single multi-path resource with a GET, DELETE, and POST method")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="DELETE">
                      <response status="200"/>
                   </method>
                   <method name="POST">
                      <request>
                         <representation mediaType="application/xml"/>
                      </request>
                      <response status="200"/>
                   </method>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource ending in /") {
      Given("a WADL that contains a single multi-path resource with a GET, DELETE, and POST method and ending in /")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="DELETE">
                      <response status="200"/>
                   </method>
                   <method name="POST">
                      <request>
                         <representation mediaType="application/xml"/>
                      </request>
                      <response status="200"/>
                   </method>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource in tree form") {
      Given("a WADL that contains a single multi-path resource in tree form with a GET, DELETE, and POST method")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                     <method name="POST">
                        <request>
                           <representation mediaType="application/xml"/>
                        </request>
                        <response status="200"/>
                     </method>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource in mixed form") {
      Given("a WADL that contains a single multi-path resource in mixed form with a GET, DELETE, and POST method")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                     <method name="POST">
                        <request>
                           <representation mediaType="application/xml"/>
                        </request>
                        <response status="200"/>
                     </method>
                   </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource with a method referece") {
      Given("a WADL that contains a single multi-path resource with a method reference")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method href="#getMethod" />
                   <method name="DELETE">
                      <response status="200"/>
                   </method>
                   <method name="POST">
                      <request>
                         <representation mediaType="application/xml"/>
                      </request>
                      <response status="200"/>
                   </method>
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource with a resource type") {
      Given("a WADL that contains a single multi-path resource with a resource type")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource" type="#test"/>
           </resources>
           <resource_type id="test">
              <method id="getMethod" name="GET">
                  <response status="200 203"/>
              </method>
              <method name="DELETE">
                  <response status="200"/>
              </method>
              <method name="POST">
                   <request>
                     <representation mediaType="application/xml"/>
                   </request>
                     <response status="200"/>
             </method>
           </resource_type>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    scenario("The WADL contains a single multi-path resource with a resource type with method references") {
      Given("a WADL that contains a single multi-path resource with a resource type with method references")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource" type="#test"/>
           </resources>
           <resource_type id="test">
              <method href="#getMethod" />
              <method name="DELETE">
                  <response status="200"/>
              </method>
              <method name="POST">
                 <request>
                    <representation mediaType="application/xml"/>
                 </request>
                 <response status="200"/>
              </method>
           </resource_type>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      singlePathAssertions(checker)
    }

    //
    //  The following scenarios test two resources located at
    //  /path/to/my/resource with a GET and a DELETE method and
    //  /path/to/my/other_resource with a GET and POST method. They
    //  are equivalent but they are written in slightly different WADL
    //  form. The assertions below must apply to all of them.
    //

    def multiplePathAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 5")
      And ("The checker should contain a GET, POST, and DELETE method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='DELETE']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='POST']")
      And ("The path from the start should contain all URL nodes in order")
      And ("it should end in the right method")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("DELETE"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("other_resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("other_resource"), Method("POST"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL("other_resource"), URLFail)
      assert (checker, URL("other_resource"), MethodFail)
    }

    scenario("The WADL contains multiple, related paths") {
      Given ("a WADL with multiple related paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="DELETE">
                      <response status="200"/>
                   </method>
              </resource>
              <resource path="path/to/my/other_resource">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="POST">
                      <response status="200"/>
                   </method>
              </resource>
          </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multiplePathAssertions(checker)
    }

    scenario("The WADL in tree format contains multiple, related paths") {
      Given ("a WADL in tree format with multiple related paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                   </resource>
                   <resource path="other_resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="POST">
                        <response status="200"/>
                     </method>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multiplePathAssertions(checker)
    }

    scenario("The WADL in mix format contains multiple, related paths") {
      Given ("a WADL in mix format with multiple related paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                   </resource>
                   <resource path="other_resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="POST">
                        <response status="200"/>
                     </method>
                   </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multiplePathAssertions(checker)
    }

    //
    //  The following scenarios test two resources located at
    //  /path/to/my/resource with a GET and a DELETE method and
    //  /path/to/my/other_resource with a GET and POST method. They
    //  are equivalent but they are written in slightly different WADL
    //  form. The assertions below must apply to all of them.
    //

    def multipleUnrelatedPathAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 8")
      And ("The checker should contain a GET, POST, and DELETE method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='DELETE']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='POST']")
      And ("The path from the start should contain all URL nodes in order")
      And ("it should end in the right method")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("DELETE"))
      assert (checker, Start, URL("this"), URL("is"), URL("my"), URL("other_resource"), Method("GET"))
      assert (checker, Start, URL("this"), URL("is"), URL("my"), URL("other_resource"), Method("POST"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("this"), URLFail)
      assert (checker, URL("this"), MethodFail)
      assert (checker, URL("is"), URLFail)
      assert (checker, URL("is"), MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL("other_resource"), URLFail)
      assert (checker, URL("other_resource"), MethodFail)
    }

    scenario("The WADL contains multiple, unrelated paths") {
      Given ("a WADL with multiple unrelated paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="DELETE">
                      <response status="200"/>
                   </method>
              </resource>
              <resource path="this/is/my/other_resource">
                   <method name="GET">
                      <response status="200 203"/>
                   </method>
                   <method name="POST">
                      <response status="200"/>
                   </method>
              </resource>
          </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multipleUnrelatedPathAssertions(checker)
    }

    scenario("The WADL in tree format contains multiple, unrelated paths") {
      Given ("a WADL in tree format with multiple unrelated paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                   </resource>
                 </resource>
                </resource>
              </resource>
              <resource path="this">
                <resource path="is">
                   <resource path="my">
                     <resource path="other_resource">
                       <method name="GET">
                          <response status="200 203"/>
                       </method>
                       <method name="POST">
                          <response status="200"/>
                       </method>
                     </resource>
                   </resource>
                </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multipleUnrelatedPathAssertions(checker)
    }

    scenario("The WADL in mix format contains multiple, unrelated paths") {
      Given ("a WADL in mix format with multiple unrelated paths")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                   </resource>
              </resource>
              <resource path="this/is/my">
                   <resource path="other_resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="POST">
                        <response status="200"/>
                     </method>
                   </resource>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      multipleUnrelatedPathAssertions(checker)
    }

    scenario("The WADL contains method ids") {
      Given ("a WADL with method IDs")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method id="getResource" name="GET">
                      <response status="200 203"/>
                   </method>
                   <method id="deleteResource" name="DELETE">
                      <response status="200"/>
                   </method>
              </resource>
          </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      Then("The method nodes should contain a resource label with the id")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD']) = 2")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET' and @label='getResource']")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='DELETE' and @label='deleteResource']")
    }

    scenario("The WADL contains an initial invisible node") {
      Given ("a WADL with an initial invisble node")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:rax="http://docs.rackspace.com/api">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource rax:invisible="true" path="path">
               <method name="GET">
                    <response status="200 203"/>
                </method>
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                     <method name="DELETE">
                        <response status="200"/>
                     </method>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      Then("All paths should be available as defined in the WADL...")
      assert (checker, Start, URL("path"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("DELETE"))
      And("Paths should also be accessible directly from start")
      assert (checker, Start, Method("GET"))
      assert (checker, Start, URL("to"), URL("my"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("to"), URL("my"), URL("resource"), Method("DELETE"))
    }

    //
    //  The following scenarios test a string template parameter at the 
    //  of a resource path (/path/to/my/resource/{id}. They are
    //  equivalent but they are written in slightly different WADL
    //  form the assertions below must apply to all of them.
    //

    def stringTemplateAtEndAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 5")
      And ("The checker should contain a GET method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      And ("The path from the start should contain all URL nodes including a .*")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), URL(".*"), Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL(".*"), URLFail)
      assert (checker, URL(".*"), MethodFail)
      And ("the URL('resource') will not have an URL fail because all URLs are accepted")
      val stepsFromResource = allStepsFromStep (checker, URL("resource"), 2)
      assert (stepsFromResource, "not(//chk:step[@type='URL_FAIL'])")
    }


    scenario("The WADL contains a template parameter of type string at the end of a path") {
      Given("a WADL with a single template string at the end of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <param name="id" style="template" type="xsd:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in tree format contains a template parameter of type string at the end of a path") {
      Given("a WADL in tree format with a single template string at the end of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                    <resource path="{id}">
                       <param name="id" style="template" type="xsd:string"/>
                       <method href="#getMethod" />
                    </resource>
                  </resource>
                </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in mix format contains a template parameter of type string at the end of a path") {
      Given("a WADL in mix format with a single template string at the end of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="resource">
                    <resource path="{id}">
                       <param name="id" style="template" type="xsd:string"/>
                       <method href="#getMethod" />
                    </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateAtEndAssertions(checker)
    }

    scenario("The WADL contains a template parameter of type string at the end of a path, the prefix used is not xsd, but the qname is valid") {
      Given("a WADL with a single template string at the end of the path, the prefix used is not xsd, but the qname is valid")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:x="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <param name="id" style="template" type="x:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateAtEndAssertions(checker)
    }

    scenario("Error Condition: The WADL contains a template parameter of type string, but the param element has a name mismatch") {
      Given("a WADL with a template parameter, with a mismatch in the param name")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <param name="other" style="template" type="xsd:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      Then ("A WADLException should be thrown")
      intercept[WADLException] {
        val checker = builder.build (inWADL, stdConfig)
      }
    }

    scenario("Error Condition: The WADL contains a template parameter of type string, but is missing a  param element") {
      Given("a WADL with a template parameter but no param element")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      Then ("A WADLException should be thrown")
      intercept[WADLException] {
        val checker = builder.build (inWADL, stdConfig)
      }
    }

    scenario("Error Condition: The WADL contains a template parameter of type string, but the param element has a type mismatch") {
      Given("a WADL with a template parameter, with a mismatch in the param type")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <param name="id" style="header" type="xsd:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      Then ("A WADLException should be thrown")
      intercept[WADLException] {
        val checker = builder.build (inWADL, stdConfig)
      }
    }

    scenario("Error Condition: The WADL contains a template parameter of a type with a bad qname") {
      Given("a WADL with a template parameter of a type with a bad qname")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource/{id}">
                   <param name="id" style="template" type="xsd:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      Then ("A WADLException should be thrown")
      intercept[WADLException] {
        val checker = builder.build (inWADL, stdConfig)
      }
    }

    //
    //  The following scenarios test a string template parameter in the
    //  middle of the resource path (/path/to/my/{id}/resource. They are
    //  equivalent but they are written in slightly different WADL
    //  form the assertions below must apply to all of them.
    //

    def stringTemplateInMiddleAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 5")
      And ("The checker should contain a GET method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      And ("The path from the start should contain all URL nodes including a .*")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL(".*"), URL("resource"), Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL(".*"), URLFail)
      assert (checker, URL(".*"), MethodFail)
      And ("the URL('my') will not have an URL fail because all URLs are accepted")
      val stepsFromResource = allStepsFromStep (checker, URL("my"), 2)
      assert (stepsFromResource, "not(//chk:step[@type='URL_FAIL'])")
    }

    scenario("The WADL contains a template parameter of type string in the middle of the path") {
      Given("a WADL with a single template string in the middle of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/{id}/resource">
                   <param name="id" style="template" type="xsd:string"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateInMiddleAssertions(checker)
    }

    scenario("The WADL in tree format contains a template parameter of type string in the middle of the path") {
      Given("a WADL in tree format with a single template string in the middle of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                    <resource path="{id}">
                       <param name="id" style="template" type="xsd:string"/>
                      <resource path="resource">
                       <method href="#getMethod" />
                     </resource>
                  </resource>
                </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateInMiddleAssertions(checker)
    }

    scenario("The WADL in mix format contains a template parameter of type string in the middle of the path") {
      Given("a WADL in mix format with a single template string in the middle of the path")
      val inWADL=
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="{id}">
                      <param name="id" style="template" type="xsd:string"/>
                    <resource path="resource">
                       <method href="#getMethod" />
                    </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When ("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      stringTemplateInMiddleAssertions(checker)
    }

    //
    //  The following scenarios test a custom type template parameter at the
    //  of a resource path (/path/to/my/resource/{yn}. They are
    //  equivalent but they are written in slightly different WADL
    //  form the assertions below must apply to all of them.
    //

    def customTemplateAtEndAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 4")
      And("A single URLXSD node")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD']) = 1")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD' and @label='yn']) = 1")
      And ("The checker should contain a GET method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      And ("The path from the start should contain all URL and URLXSD nodes")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Label("yn"), Method("GET"))
      And ("The URLXSD should match a valid QName")
      assert (checker, "namespace-uri-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'test://schema/a'")
      assert (checker, "local-name-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'yesno'")
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, Label("yn"), URLFail)
      assert (checker, Label("yn"), MethodFail)
      And("The grammar nodes are added to the checker")
      assert (checker, "/chk:checker/chk:grammar[@ns='test://schema/a' and  @type='W3C_XML']")
      assert (checker, "if (/chk:checker/chk:grammar/@href) then not(/chk:checker/chk:grammar/xsd:schema) else /chk:checker/chk:grammar/xsd:schema")
      assert (checker, "if (/chk:checker/chk:grammar/@href) then /chk:checker/chk:grammar/@href='test://app/xsd/simple.xsd' else true()")
    }

    scenario("The WADL contains a template parameter of a custom type at the end of the path") {
      Given("A WADL with a template parameter of a custom type at the end of the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/resource/{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL contains a template parameter of a custom type at the end of the path, the XSD is embedded") {
      Given("A WADL with a template parameter of a custom type at the end of the path, the XSD is embedded")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/resource/{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL contains a template parameter of a custom type at the end of the path, the schema is in a relative path") {
      Given("A WADL with a template parameter of a custom type at the end of the path, the schema is in a relative path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/resource/{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in tree format contains a template parameter of a custom type at the end of the path") {
      Given("A WADL in tree format with a template parameter of a custom type at the end of the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                    <resource path="{yn}">
                       <param name="yn" style="template" type="tst:yesno"/>
                       <method href="#getMethod" />
                    </resource>
                  </resource>
                </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in mix format contains a template parameter of a custom type at the end of the path") {
      Given("A WADL in mix format with a template parameter of a custom type at the end of the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                   <resource path="resource">
                    <resource id="yn" path="{yn}">
                       <param name="yn" style="template" type="tst:yesno"/>
                       <method href="#getMethod" />
                    </resource>
                    </resource>
               </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL contains a template parameter of a custom type at the end of the path, the type is in the default namespace") {
      Given("A WADL with a template parameter of a custom type at the end of the path, with the type in a default namespace")
      val inWADL =
        <wadl:application xmlns:wadl="http://wadl.dev.java.net/2009/02"
                          xmlns="test://schema/a">
           <wadl:grammars>
              <wadl:include href="test://app/xsd/simple.xsd"/>
           </wadl:grammars>
           <wadl:resources base="https://test.api.openstack.com">
              <wadl:resource id="yn" path="path/to/my/resource/{yn}">
                   <wadl:param name="yn" style="template" type="yesno"/>
                   <wadl:method href="#getMethod" />
              </wadl:resource>
           </wadl:resources>
           <wadl:method id="getMethod" name="GET">
               <wadl:response status="200 203"/>
           </wadl:method>
        </wadl:application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in tree format contains a template parameter of a custom type at the end of the path, the type is in the default namespace") {
      Given("A WADL in tree format with a template parameter of a custom type at the end of the path, the type is in the default namespace")
      val inWADL =
        <wadl:application xmlns:wadl="http://wadl.dev.java.net/2009/02"
                          xmlns="test://schema/a">
           <wadl:grammars>
              <wadl:include href="test://app/xsd/simple.xsd"/>
           </wadl:grammars>
           <wadl:resources base="https://test.api.openstack.com">
              <wadl:resource path="path">
                <wadl:resource path="to">
                  <wadl:resource path="my">
                   <wadl:resource path="resource">
                    <wadl:resource path="{yn}">
                       <wadl:param name="yn" style="template" type="yesno"/>
                       <wadl:method href="#getMethod" />
                    </wadl:resource>
                  </wadl:resource>
                </wadl:resource>
               </wadl:resource>
             </wadl:resource>
           </wadl:resources>
           <wadl:method id="getMethod" name="GET">
               <wadl:response status="200 203"/>
           </wadl:method>
        </wadl:application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL in mix format contains a template parameter of a custom type at the end of the path, the type is in the default namespace") {
      Given("A WADL in mix format with a template parameter of a custom type at the end of the path, the type is in the default namespace")
      val inWADL =
        <wadl:application xmlns:wadl="http://wadl.dev.java.net/2009/02"
                         xmlns="test://schema/a">
           <wadl:grammars>
              <wadl:include href="test://app/xsd/simple.xsd"/>
           </wadl:grammars>
           <wadl:resources base="https://test.api.openstack.com">
              <wadl:resource path="path/to/my">
                   <wadl:resource path="resource">
                    <wadl:resource id="yn" path="{yn}">
                       <wadl:param name="yn" style="template" type="yesno"/>
                       <wadl:method href="#getMethod" />
                    </wadl:resource>
                    </wadl:resource>
               </wadl:resource>
           </wadl:resources>
           <wadl:method id="getMethod" name="GET">
               <wadl:response status="200 203"/>
           </wadl:method>
        </wadl:application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateAtEndAssertions(checker)
    }

    scenario("The WADL contains a template parameter of a custom type at the end of the path, with remove dup on") {
      Given("A WADL with a template parameter of a custom type at the end of the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/resource/{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
              <resource path="1/dup">
                   <method href="#getMethod" />
              </resource>
              <resource path="2/dup">
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://app/xsd/simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, dupConfig)
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 7")
      And("A single URLXSD node")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD']) = 1")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD' and @label='yn']) = 1")
      And ("The checker should contain a GET method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      And ("The path from the start should contain all URL and URLXSD nodes")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Label("yn"), Method("GET"))
      And ("The URLXSD should match a valid QName")
      assert (checker, "namespace-uri-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'test://schema/a'")
      assert (checker, "local-name-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'yesno'")
      And ("There should not be a duplicate dup node")
      assert (checker, "count(//chk:step[@type='URL' and @match='dup']) = 1")
      And ("The dup paths should be valid")
      assert (checker, Start, URL("1"), URL("dup"), Method("GET"))
      assert (checker, Start, URL("2"), URL("dup"), Method("GET"))
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("1"), URLFail)
      assert (checker, URL("1"), MethodFail)
      assert (checker, URL("2"), URLFail)
      assert (checker, URL("2"), MethodFail)
      assert (checker, URL("dup"), URLFail)
      assert (checker, URL("dup"), MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, Label("yn"), URLFail)
      assert (checker, Label("yn"), MethodFail)
      And("The grammar nodes are added to the checker")
      assert (checker, "/chk:checker/chk:grammar[@ns='test://schema/a' and @href='test://app/xsd/simple.xsd' and @type='W3C_XML']")
    }

    scenario("The WADL contains a template parameter of a custom type, but the grammar is missing") {
      Given("A WADL with a template parameter of a custom type, but the grammar is missing")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/resource/{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      Then ("A WADLException should be thrown")
      intercept[WADLException] {
        val checker = builder.build (inWADL, dupConfig)
      }
    }

    scenario("The WADL contains a template parameter of a custom type, but the grammar is not XSD") {
      Given("A WADL with a template parameter of a custom type, but the grammar is not XSD")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.rng"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="int" path="path/to/my/resource/{int}">
                   <param name="int" style="template" type="tst:int"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      register("test://app/xsd/simple.rng",
               <grammar datatypeLibrary="http://www.w3.org/2001/XMLSchema-datatypes"
               xmlns="http://relaxng.org/ns/structure/1.0" >
                 <define name="int">
                   <data type="integer"/>
                 </define>
               </grammar>
             )
      Then ("The grammar should be ignored")
      val checker = builder.build (inWADL, dupConfig)
      assert (checker, "not(/chk:checker/chk:grammar)")
    }

    //
    //  We ignore until we handle adding an unparsed text reslover for
    //  testing.
    //
    ignore("The WADL contains a template parameter of a custom type, but the grammar is not XML") {
      Given("A WADL with a template parameter of a custom type, but the grammar is not XML")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://app/xsd/simple.json"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="int" path="path/to/my/resource/{int}">
                   <param name="int" style="template" type="tst:int"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      When("the wadl is translated")
      // register_unparsed("test://app/xsd/simple.json",
      //          """
      //            {
      //              "type" : "int"
      //            }
      //          """
      //        )
      Then ("The grammar should be ignored")
      val checker = builder.build (inWADL, stdConfig)
      assert (checker, "not(/chk:checker/chk:grammar)")
    }


    //
    //  The following scenarios test a custom template parameter in the
    //  middle of the resource path (/path/to/my/{yn}/resource. They are
    //  equivalent but they are written in slightly different WADL
    //  form the assertions below must apply to all of them.
    //

    def customTemplateInMiddleAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 4")
      And("A single URLXSD node")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD']) = 1")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD' and @label='yn']) = 1")
      And ("The checker should contain a GET method")
      assert (checker, "/chk:checker/chk:step[@type='METHOD' and @match='GET']")
      And ("The path from the start should contain all URL and URLXSD nodes")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), Label("yn"), URL("resource"), Method("GET"))
      And ("The URLXSD should match a valid QName")
      assert (checker, "namespace-uri-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'test://schema/a'")
      assert (checker, "local-name-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'yesno'")
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, Label("yn"), URLFail)
      assert (checker, Label("yn"), MethodFail)
      And("The grammar nodes are added to the checker")
      assert (checker, "/chk:checker/chk:grammar[@ns='test://schema/a' and @href='test://simple.xsd' and @type='W3C_XML']")
    }

    scenario("The WADL contains a template parameter of a custom type in the middle of the path") {
      Given("A WADL with a template parameter of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/{yn}/resource">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateInMiddleAssertions(checker)
    }

    scenario("The WADL in tree format contains a template parameter of a custom type in the middle of the path") {
      Given("A WADL in tree format with a template parameter of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                    <resource id="yn" path="{yn}">
                       <param name="yn" style="template" type="tst:yesno"/>
                      <resource path="resource">
                       <method href="#getMethod" />
                     </resource>
                  </resource>
                </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateInMiddleAssertions(checker)
    }

    scenario("The WADL in mix format contains a template parameter of a custom type in the middle of the path") {
      Given("A WADL in mix format with a template parameter of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a">
           <grammars>
              <include href="test://simple.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                 <resource id="yn" path="{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <resource path="resource">
                     <method href="#getMethod" />
                   </resource>
                 </resource>
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplateInMiddleAssertions(checker)
    }

    //
    //  The following scenarios test multiple template parameters in
    //  the middle of the resource path (/path/to/my/{yn}/resource,
    //  /path/to/my/{tf}/resource. They are equivalent but they are
    //  written in slightly different WADL form the assertions below
    //  must apply to all of them.
    //

    def customTemplatesInMiddleAssertions (checker : NodeSeq) : Unit = {
      Then("The checker should contain an URL node for each path step")
      assert (checker, "count(/chk:checker/chk:step[@type='URL']) = 5")
      And("A single URLXSD node")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD']) = 2")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD' and @label='yn']) = 1")
      assert (checker, "count(/chk:checker/chk:step[@type='URLXSD' and @label='tf']) = 1")
      And ("The checker should contain two GET methods")
      assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 2")
      And ("The path from the start should contain all URL and URLXSD nodes")
      And ("it should end in the GET method node")
      assert (checker, Start, URL("path"), URL("to"), URL("my"), Label("yn"), URL("resource"), Method("GET"))
      assert (checker, Start, URL("path"), URL("to"), URL("my"), Label("tf"), URL("resource"), Method("GET"))
      And ("The URLXSD should match a valid QName")
      assert (checker, "namespace-uri-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'test://schema/a'")
      assert (checker, "local-name-from-QName(resolve-QName(//chk:step[@label='yn'][1]/@match, //chk:step[@label='yn'][1])) "+
                                           "= 'yesno'")
      assert (checker, "namespace-uri-from-QName(resolve-QName(//chk:step[@label='tf'][1]/@match, //chk:step[@label='tf'][1])) "+
                                           "= 'test://schema/b'")
      assert (checker, "local-name-from-QName(resolve-QName(//chk:step[@label='tf'][1]/@match, //chk:step[@label='tf'][1])) "+
                                           "= 'truefalse'")
      And ("The Start state and each URL state should contain a path to MethodFail and URLFail")
      assert (checker, Start, URLFail)
      assert (checker, Start, MethodFail)
      assert (checker, URL("path"), URLFail)
      assert (checker, URL("path"), MethodFail)
      assert (checker, URL("to"), URLFail)
      assert (checker, URL("to"), MethodFail)
      assert (checker, URL("my"), MethodFail)
      assert (checker, URL("my"), URLFail)
      assert (checker, URL("resource"), MethodFail)
      assert (checker, URL("resource"), URLFail)
      assert (checker, Label("yn"), URLFail)
      assert (checker, Label("yn"), MethodFail)
      assert (checker, Label("tf"), URLFail)
      assert (checker, Label("tf"), MethodFail)
      And("The grammar nodes are added to the checker")
      assert (checker, "/chk:checker/chk:grammar[@ns='test://schema/a' and @href='test://simple.xsd' and @type='W3C_XML']")
      assert (checker, "/chk:checker/chk:grammar[@ns='test://schema/b' and @href='test://simple-too.xsd' and @type='W3C_XML']")
    }

    scenario("The WADL contains multiple template parameters of a custom type in the middle of the path") {
      Given("A WADL with multiple template parameters of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a"
                     xmlns:tst2="test://schema/b"
                     >
           <grammars>
              <include href="test://simple.xsd"/>
              <include href="test://simple-too.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource id="yn" path="path/to/my/{yn}/resource">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <method href="#getMethod" />
              </resource>
              <resource id="tf" path="path/to/my/{tf}/resource">
                   <param name="tf" style="template" type="tst2:truefalse"/>
                   <method href="#otherGetMethod" />
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
           <method id="otherGetMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      register("test://simple-too.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/b">
                   <simpleType name="truefalse">
                       <restriction base="xsd:string">
                           <enumeration value="true"/>
                           <enumeration value="false"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplatesInMiddleAssertions(checker)
    }

    scenario("The WADL in tree format contains multiple template parameter of a custom type in the middle of the path") {
      Given("A WADL in tree format with multiple template parameter of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a"
                     xmlns:tst2="test://schema/b"
                     >
           <grammars>
              <include href="test://simple.xsd"/>
              <include href="test://simple-too.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                    <resource id="yn" path="{yn}">
                       <param name="yn" style="template" type="tst:yesno"/>
                      <resource path="resource">
                       <method href="#getMethod" />
                     </resource>
                  </resource>
                   <resource id="tf" path="{tf}">
                       <param name="tf" style="template" type="tst2:truefalse"/>
                      <resource path="resource">
                       <method href="#otherGetMethod" />
                     </resource>
                  </resource>
                </resource>
               </resource>
             </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
           <method id="otherGetMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      register("test://simple-too.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/b">
                   <simpleType name="truefalse">
                       <restriction base="xsd:string">
                           <enumeration value="true"/>
                           <enumeration value="false"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplatesInMiddleAssertions(checker)
    }

    scenario("The WADL in mix format contains multiple template parameters of a custom type in the middle of the path") {
      Given("A WADL in mix format with multiple template parameters of a custom type in the middle of  the path")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02"
                     xmlns:tst="test://schema/a"
                     xmlns:tst2="test://schema/b"
                     >
           <grammars>
              <include href="test://simple.xsd"/>
              <include href="test://simple-too.xsd"/>
           </grammars>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                 <resource id="yn" path="{yn}">
                   <param name="yn" style="template" type="tst:yesno"/>
                   <resource path="resource">
                     <method href="#getMethod" />
                   </resource>
                 </resource>
                   <resource id="tf" path="{tf}">
                       <param name="tf" style="template" type="tst2:truefalse"/>
                      <resource path="resource">
                       <method href="#otherGetMethod" />
                     </resource>
                  </resource>
              </resource>
           </resources>
           <method id="getMethod" name="GET">
               <response status="200 203"/>
           </method>
           <method id="otherGetMethod" name="GET">
               <response status="200 203"/>
           </method>
        </application>
      register("test://simple.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/a">
                   <simpleType name="yesno">
                       <restriction base="xsd:string">
                           <enumeration value="yes"/>
                           <enumeration value="no"/>
                       </restriction>
                   </simpleType>
                </schema>)
      register("test://simple-too.xsd",
               <schema elementFormDefault="qualified"
                        attributeFormDefault="unqualified"
                        xmlns="http://www.w3.org/2001/XMLSchema"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        targetNamespace="test://schema/b">
                   <simpleType name="truefalse">
                       <restriction base="xsd:string">
                           <enumeration value="true"/>
                           <enumeration value="false"/>
                       </restriction>
                   </simpleType>
                </schema>)
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      customTemplatesInMiddleAssertions(checker)
    }

    scenario("The WADL contains a URL with special regex symbols") {
      Given("a WADL that contains an URL winh special regex symbols")
      val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="\^-.${}*+|#()[]">
                  <method name=".-"/>
              </resource>
              <resource path="^ABC[D]EFG#">
                  <method name="-GET..IT-"/>
              </resource>
           </resources>
        </application>
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      assert (checker, Start, URL("\\\\\\^\\-\\.\\$\\{\\}\\*\\+\\|\\#\\(\\)\\[\\]"), Method("\\.\\-"))
      assert (checker, Start, URL("\\^ABC\\[D\\]EFG\\#"), Method("\\-GET\\.\\.IT\\-"))
    }
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, they are used in the next couple of tests.
  //
  def reqTypeAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("any"), Method("POST"), AnyReqType)
    assert (checker, Start, URL("text"), Method("POST"), ReqType("(text/)(.*)"))
    assert (checker, Start, URL("v"), Method("POST"), ReqType("(text/plain;charset=UTF8)()"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }

  scenario("The WADL contains PUT and POST operations accepting various media types") {
    Given ("a WADL that contains multiple PUT and POST operation with various media types")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars/>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    When("the wadl is translated")
    val checker = builder.build (inWADL, stdConfig)
    reqTypeAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting various media types, with dups optimization on") {
    Given ("a WADL that contains multiple PUT and POST operation with various media types")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars/>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    When("the wadl is translated")
    val checker = builder.build (inWADL, dupConfig)
    reqTypeAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
  }

  //
  //  The following scenarios test the remove duplicate optimization
  //  when there are no duplicates. They are equivalent but they are
  //  written in slightly different WADL form the assertions below
  //  must apply to all of them.
  //

  def noDupAsserts(checker : NodeSeq) : Unit = {
    Then("The paths in both resources should match")
    assert (checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
    assert (checker, "count(//chk:step[@type='URL' and @match='path']) = 1")
    assert (checker, "count(//chk:step[@type='URL' and @match='to']) = 1")
    assert (checker, "count(//chk:step[@type='URL' and @match='my']) = 1")
    assert (checker, "count(//chk:step[@type='URL' and @match='resource']) = 1")
    assert (checker, "count(//chk:step[@type='METHOD' and @match='GET']) = 1")
  }

  scenario("The WADL does not contain any duplicate resources, but remove dup optimization is on") {
    Given("a WADL that contains no duplicate nodes")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
              </resource>
           </resources>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    noDupAsserts(checker_dupon)
    noDupAsserts(checker_dupoff)
  }

  scenario("The WADL, in tree format, does not contain any duplicate nodes, but remove dup optimization is on") {
    Given("a WADL in tree format that contains no duplicate nodes")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    noDupAsserts(checker_dupon)
    noDupAsserts(checker_dupoff)
  }

  scenario("The WADL, in mix format, does not contain any duplicate nodes, but remove dup optimization is on") {
    Given("a WADL in mix format that contains no duplicate nodes")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                  <resource path="resource">
                     <method name="GET">
                        <response status="200 203"/>
                     </method>
                  </resource>
              </resource>
           </resources>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    noDupAsserts(checker_dupon)
    noDupAsserts(checker_dupoff)
  }

  //
  //  The following scenarios test the remove duplicate optimization
  //  when there is a single duplicate. They are equivalent but they are
  //  written in slightly different WADL form the assertions below
  //  must apply to all of them.
  //

  def singleDupAsserts(checker_dupon : NodeSeq, checker_dupoff : NodeSeq) : Unit = {
    Then("Paths should exist in both checkers")
    assert (checker_dupon, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
    assert (checker_dupon, Start, URL("path"), URL("to"), URL("another"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("path"), URL("to"), URL("another"), URL("resource"), Method("GET"))

    And("there should be a duplicate resource node when dup is off")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='path']) = 1")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='to']) = 1")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='my']) = 1")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='resource']) = 2")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='another']) = 1")
    assert (checker_dupoff, "count(//chk:step[@type='METHOD' and @match='GET']) = 1")

    And("there should *not* be a duplicate resource node when dup is on")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='path']) = 1")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='to']) = 1")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='my']) = 1")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='resource']) = 1")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='another']) = 1")
    assert (checker_dupon, "count(//chk:step[@type='METHOD' and @match='GET']) = 1")
  }

  scenario("The WADL contain a single duplicate and remove dup optimization is on") {
    Given("a WADL that contains a single duplicate")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                     <method href="#res"/>
              </resource>
              <resource path="path/to/another/resource">
                     <method href="#res"/>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    singleDupAsserts(checker_dupon, checker_dupoff)
  }

  scenario("The WADL, in tree format, contains a single duplicate node and remove dup optimization is on") {
    Given("a WADL in tree format contains a single duplicate")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path">
                <resource path="to">
                  <resource path="my">
                   <resource path="resource">
                     <method href="#res"/>
                   </resource>
                 </resource>
                 <resource path="another">
                   <resource path="resource">
                     <method href="#res"/>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    singleDupAsserts(checker_dupon, checker_dupoff)
  }

  scenario("The WADL, in mix format,  contain a single duplicate and remove dup optimization is on") {
    Given("a WADL, in mix format,  that contains a single duplicate")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my">
                 <resource path="resource">
                     <method href="#res"/>
                 </resource>
              </resource>
              <resource path="path/to/another">
                 <resource path="resource">
                     <method href="#res"/>
                 </resource>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    singleDupAsserts(checker_dupon, checker_dupoff)
  }


  //
  //  The following scenarios test the remove duplicate optimization
  //  when there is a single duplicate. They are equivalent but they are
  //  written in slightly different WADL form the assertions below
  //  must apply to all of them.
  //

  def multipleDupAsserts(checker_dupon : NodeSeq, checker_dupoff : NodeSeq) : Unit = {
    Then("Paths should exist in both checkers")
    assert (checker_dupon, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
    assert (checker_dupon, Start, URL("path"), URL("to"), URL("another"), URL("resource"), Method("GET"))
    assert (checker_dupon, Start, URL("what"), URL("about"), URL("this"), URL("resource"), Method("GET"))
    assert (checker_dupon, Start, URL("what"), URL("about"), URL("this"), URL("resource"), URL("resource2"), Method("GET"))
    assert (checker_dupon, Start, URL("and"), URL("another"), URL("resource"), Method("GET"))
    assert (checker_dupon, Start, URL("and"), URL("another"), URL("resource2"), Method("GET"))
    assert (checker_dupon, Start, URL("resource2"), Method("GET"))
    assert (checker_dupoff, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("path"), URL("to"), URL("another"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("what"), URL("about"), URL("this"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("what"), URL("about"), URL("this"), URL("resource"), URL("resource2"), Method("GET"))
    assert (checker_dupoff, Start, URL("and"), URL("another"), URL("resource"), Method("GET"))
    assert (checker_dupoff, Start, URL("and"), URL("another"), URL("resource2"), Method("GET"))
    assert (checker_dupoff, Start, URL("resource2"), Method("GET"))


    And("there should be a number of duplicate resource node when dup is off")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='resource']) = 4")
    assert (checker_dupoff, "count(//chk:step[@type='URL' and @match='resource2']) = 3")

    And("there should *not* be many duplicate resources dup is on, resource will be duplicate 'cus it's used in 2 different ways")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='resource']) = 2")
    assert (checker_dupon, "count(//chk:step[@type='URL' and @match='resource2']) = 1")

    And("there should be the same number of methods in each")
    assert (checker_dupon, "count(//chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker_dupoff, "count(//chk:step[@type='METHOD' and @match='GET']) = 2")
  }

  scenario("The WADL contain multiple duplicates and remove dup optimization is on") {
    Given("a WADL that contains multiple duplicates")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                     <method href="#res"/>
              </resource>
              <resource path="path/to/another/resource">
                     <method href="#res"/>
              </resource>
              <resource path="and/another/resource">
                     <method href="#res"/>
              </resource>
              <resource path="what/about/this/resource">
                     <method href="#res"/>
              </resource>
              <resource path="what/about/this/resource/resource2">
                     <method href="#res2"/>
              </resource>
              <resource path="and/another/resource2">
                     <method href="#res2"/>
              </resource>
              <resource path="resource2">
                     <method href="#res2"/>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
           <method id="res2" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    multipleDupAsserts(checker_dupon, checker_dupoff)
  }

  scenario("The WADL, in tree format, contains multiple duplicates and remove dup optimization is on") {
    Given("a WADL in tree format contains multiple duplicates")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="resource2">
                 <method href="#res2"/>
              </resource>
              <resource path="and">
                  <resource path="another">
                   <resource path="resource">
                     <method href="#res"/>
                   </resource>
                   <resource path="resource2">
                      <method href="#res2"/>
                   </resource>
                 </resource>
              </resource>
              <resource path="what">
                <resource path="about">
                  <resource path="this">
                    <resource path="resource">
                      <method href="#res"/>
                      <resource path="resource2">
                        <method href="#res2"/>
                      </resource>
                    </resource>
                  </resource>
                </resource>
              </resource>
              <resource path="path">
                <resource path="to">
                  <resource path="another">
                   <resource path="resource">
                     <method href="#res"/>
                   </resource>
                  </resource>
                  <resource path="my">
                   <resource path="resource">
                     <method href="#res"/>
                   </resource>
                 </resource>
                </resource>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
           <method id="res2" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    multipleDupAsserts(checker_dupon, checker_dupoff)
  }

  scenario("The WADL, in mixed format, contains multiple duplicates and remove dup optimization is on") {
    Given("a WADL, in mixed format, that contains multiple duplicates")
    val inWADL =
        <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to">
                  <resource path="my/resource">
                     <method href="#res"/>
                  </resource>
                  <resource path="another/resource">
                     <method href="#res"/>
                  </resource>
              </resource>
              <resource path="and/another/resource">
                     <method href="#res"/>
              </resource>
              <resource path="what/about/this/resource">
                     <method href="#res"/>
                     <resource path="resource2">
                        <method href="#res2"/>
                     </resource>
              </resource>
              <resource path="and/another/resource2">
                     <method href="#res2"/>
              </resource>
              <resource path="resource2">
                     <method href="#res2"/>
              </resource>
           </resources>
           <method id="res" name="GET">
                 <response status="200 203"/>
           </method>
           <method id="res2" name="GET">
                 <response status="200 203"/>
           </method>
        </application>
    When("the WADL is translated, with dup remove on")
    val checker_dupon = builder.build(inWADL, dupConfig)
    And ("the WADL is translated, with dup remove off")
    val checker_dupoff = builder.build(inWADL, stdConfig)

    multipleDupAsserts(checker_dupon, checker_dupoff)
  }

  //
  // The following used by the next scenarios
  //
  val multi_post_tests : List[(String, Boolean, NodeSeq)] = List(
    ("With IDs", true,
     <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method id="action1" name="POST">
                      <response status="201"/>
                   </method>
                   <method id="action2" name="POST">
                      <response status="201"/>
                   </method>
                   <method id="action3" name="POST">
                      <response status="201"/>
                   </method>
                   <method id="action4" name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
        </application>),
    ("With RAXIds", true,
     <application xmlns="http://wadl.dev.java.net/2009/02"
                  xmlns:rax="http://docs.rackspace.com/api">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method rax:id="action1" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action2" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action3" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action4" name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
        </application>),
    ("With mix RAXIDs and IDs in resourceTypes", true,
     <application xmlns="http://wadl.dev.java.net/2009/02"
                  xmlns:rax="http://docs.rackspace.com/api">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method rax:id="action1" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action2" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action3" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action4" name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
           <resource_type id="test">
                <method id="action1" name="POST">
                   <response status="201"/>
                </method>
                <method id="action2" name="POST">
                  <response status="201"/>
                </method>
                <method id="action3" name="POST">
                  <response status="201"/>
                </method>
                <method id="action4" name="POST">
                  <response status="201"/>
                </method>
           </resource_type>
        </application>),
    ("With mix RAXIDs and IDs in resourceTypes (not all ids)", false,
     <application xmlns="http://wadl.dev.java.net/2009/02"
                  xmlns:rax="http://docs.rackspace.com/api">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method rax:id="action1" name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
           <resource_type id="test">
                <method id="action1" name="POST">
                   <response status="201"/>
                </method>
                <method name="POST">
                  <response status="201"/>
                </method>
                <method name="POST">
                  <response status="201"/>
                </method>
                <method name="POST">
                  <response status="201"/>
                </method>
           </resource_type>
        </application>),
    ("With mix RAXIds and IDs", true,
     <application xmlns="http://wadl.dev.java.net/2009/02"
                  xmlns:rax="http://docs.rackspace.com/api">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method id="action1" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action2" name="POST">
                      <response status="201"/>
                   </method>
                   <method id="action3" rax:id="action3" name="POST">
                      <response status="201"/>
                   </method>
                   <method rax:id="action4" name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
        </application>),
    ("Without IDs", false,
     <application xmlns="http://wadl.dev.java.net/2009/02">
           <grammars/>
           <resources base="https://test.api.openstack.com">
              <resource path="path/to/my/resource">
                   <method name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
                   <method name="POST">
                      <response status="201"/>
                   </method>
              </resource>
           </resources>
        </application>))

  for (t <- multi_post_tests) {
    scenario("The WADL contains a single resource with multiple methods of the same type "+t._1) {
      Given("a WADL that contains a single resource with multiple methods of the same type"+t._1)
      val inWADL = t._3
      When("the wadl is translated")
      val checker = builder.build (inWADL, stdConfig)
      Then("There should be a total of 5 POST method steps...4 specified plus 1 collecting them")
      assert(checker, "count(//chk:step[@type='METHOD']) = 5")
      And ("All paths should go through POST step.")
      assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), Method("POST"), Accept)
      if (t._2) {
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), Label("action1"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), Label("action2"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), Label("action3"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"), Label("action4"))
      } else {
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"))
        assert(checker, Start, URL("path"), URL("to"), URL("my"), URL("resource"), Method("POST"))
      }
      And ("One of the post steps is labeled ε, the rest are labeled according to thier ID")
      if (t._2) {
        assert(checker, "//chk:step[@type='METHOD' and @label='action1']")
        assert(checker, "//chk:step[@type='METHOD' and @label='action2']")
        assert(checker, "//chk:step[@type='METHOD' and @label='action3']")
        assert(checker, "//chk:step[@type='METHOD' and @label='action4']")
      }
      assert(checker, "//chk:step[@type='METHOD' and @label='ε']")
    }
  }

  //
  //  The following assertions are used to test WellFormXML and
  //  ContentError nodes.  They are used in the next couple of tests.
  //
  def wellFormedAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting various media types where well formness is checked") {
    Given ("a WADL that contains multiple PUT and POST operation with various media types")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars/>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting various media types where well formness is checked, with dupson") {
    Given ("a WADL that contains multiple PUT and POST operation with various media types")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars/>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  //
  // The following assertions are used to test XSD and ContentError
  // nodes. They are used in the next couple of tests.
  //
  def xsdAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD (well formed not specified)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD (well formed not specified)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD (only grammar transform option is specified)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD (only grammar transform option is specified)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, false, 1, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but no grammar is actually specified") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
    And("There should be no XSD steps, since no grammar was specified")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to false)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request rax:ignoreXSD="false">
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request rax:ignoreXSD="false">
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to 0)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request rax:ignoreXSD="0">
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request rax:ignoreXSD="0">
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to true)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request rax:ignoreXSD="true">
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request rax:ignoreXSD="true">
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to 1)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request rax:ignoreXSD="1">
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request rax:ignoreXSD="1">
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to true, in representation)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" rax:ignoreXSD="true"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" rax:ignoreXSD="true"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, but ignore XSD extension is used (set to true, in representation, mixed)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
      >
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" rax:ignoreXSD="true"/>
                      <representation mediaType="application/atom+xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request rax:ignoreXSD="true">
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1,
                                                  false, false, false, "XalanC",
                                                    false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/atom\\+xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/atom\\+xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, with dupson") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, with dupson")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  //
  // The following assertions are used to test XSD, XPath and ContentError
  // nodes. They are used in the next couple of tests.
  //
  def xsdElementAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
  }

  //
  //  Like the assertions above but only one XPath type is specified
  //
  def xsdElementAssertions2(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
  }

  //
  //  Here, were not doing XSD validation
  //
  def elementAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e", "Expecting the root element to be: tst:e"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a", "Expecting the root element to be: tst:a"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD with elements specified and checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD with elemenst specified")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdElementAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD with elements specified and checked. The same element is used for POST and PUT") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD with elemenst specified")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdElementAssertions2(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD with elements specified and checked. The same element is used for POST and PUT, dups on") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD with elemenst specified")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdElementAssertions2(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml no XSD validation with elements specified and checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML elements specified")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    elementAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml no XSD validation, no WELL Formed  with elements specified and checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML elements specified")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" element="tst:e"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    elementAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD with elements checked, but none specified") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml" />
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" />
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
           <resource path="/any">
              <method name="POST">
                 <request>
                    <representation mediaType="*/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/text">
              <method name="POST">
                 <request>
                    <representation mediaType="text/*"/>
                 </request>
              </method>
           </resource>
           <resource path="/v">
              <method name="POST">
                 <request>
                    <representation mediaType="text/plain;charset=UTF8"/>
                 </request>
              </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true))
    reqTypeAssertions(checker)
    wellFormedAssertions(checker)
    xsdAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(.*)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/)(.*)']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(text/plain;charset=UTF8)()']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 4")
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), ContentFail)
  }

  scenario("The WADL contains a POST  operation with plain params but a missing mediaType, should validaate but not create XPaths") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 0")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"),  Accept)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (rax:code extension)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" rax:code="401" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" rax:code="500" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", 401), XPath("/tst:a/@stepType", 500), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", 401), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", 401), XPath("/tst:a/@stepType", 500), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (with rax:message extension)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true" rax:message="Missing id attribute"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true" rax:message="Missing stepType attribute"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, false, false, "Xalan",
                                                  false, false, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute"), XPath("/tst:a/@stepType", "Missing stepType attribute"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute"), XPath("/tst:a/@stepType", "Missing stepType attribute"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (with rax:message, rax:code extension)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true" rax:message="Missing id attribute" rax:code="401"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true" rax:message="Missing stepType attribute" rax:code="500"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, false, false, "Xalan",
                                                  false, false, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute", 401), XPath("/tst:a/@stepType", "Missing stepType attribute", 500), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute", 401), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id","Missing id attribute", 401), XPath("/tst:a/@stepType", "Missing stepType attribute", 500), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different namespaces)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <wadl:application xmlns:wadl="http://wadl.dev.java.net/2009/02">
        <wadl:grammars>
            <wadl:include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </wadl:grammars>
        <wadl:resources base="https://test.api.openstack.com" xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
           <wadl:resource path="/a/b">
               <wadl:method name="POST">
                  <wadl:request>
                      <wadl:representation mediaType="application/xml" element="tst:a">
                         <wadl:param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <wadl:param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </wadl:representation>
                  </wadl:request>
               </wadl:method>
           </wadl:resource>
        </wadl:resources>
    </wadl:application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (wellform checks to false)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, false, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 5")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XPath("/tst:e/tst:stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XPath("/tst:e/tst:stepType"), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params, same element in multiple reps)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 6")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params, same element in multiple reps, rax message extension)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true" rax:message="Missing id in tst:a"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true" rax:message="Missing id in tst:e"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="true" rax:message="Missing stepType in tst:e"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, false, false, "Xalan",
                                                  false, false, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 6")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", "Missing id in tst:a"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", "Missing id in tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id", "Missing id in tst:e"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:stepType", "Missing stepType in tst:e"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params, same element in multiple reps, rax:message, rax:code extensions)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true" rax:message="Missing id in tst:a" rax:code="500"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true" rax:message="Missing id in tst:e" rax:code="501"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="true" rax:message="Missing stepType in tst:e" rax:code="502"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, false, false, "Xalan",
                                                  false, false, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 6")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", "Missing id in tst:a", 500), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id", "Missing id in tst:a", 500), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id", "Missing id in tst:e", 501), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:stepType", "Missing stepType in tst:e", 502), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params, same element in multiple reps, dups on)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 5")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml which must validate against an XSD with elements specified and multiple required plain params (different reps, multiple params one with required == false)") {
    Given ("a WADL that contains a POST operation with XML that must validate against an XSD with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                      </representation>
                      <representation mediaType="application/xml" element="tst:e">
                         <param name="id" style="plain" path="/tst:e/tst:id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:e/tst:stepType" required="false"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:e/tst:stepType']) = 0")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:e","Expecting the root element to be: tst:e"),
            XPath("/tst:e/tst:id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified and multiple required plain params") {
    Given ("a WADL that contains a POST operation with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, false, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified and multiple required plain params (no XSD, element, or well-form checks)") {
    Given ("a WADL that contains a POST operation with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, false, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML,
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a/@id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified and multiple required plain params (no element check)") {
    Given ("a WADL that contains a POST operation with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, false, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML,
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a/@id"),
            ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with no elements specified and multiple required plain params") {
    Given ("a WADL that contains a POST operation with elemenst specified and multiple plain params")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML,
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a/@id"),
            ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a single XSL transform") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a single embeded XSL transform") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess>
                            <xsl:stylesheet
                                  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                  xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                                  version="1.0">
                              <xsl:template match="/">
                                  <tst:success didIt="true">Yup, that worked</tst:success>
                              </xsl:template>
                            </xsl:stylesheet>
                         </rax:preprocess>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL' and not(@href)]/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success/@didIt")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a single embeded XSL transform (dups on)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess>
                            <xsl:stylesheet
                                  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                  xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                                  version="1.0">
                              <xsl:template match="/">
                                  <tst:success didIt="true">Yup, that worked</tst:success>
                              </xsl:template>
                            </xsl:stylesheet>
                         </rax:preprocess>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL' and not(@href)]/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success/@didIt")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a single embeded XSL transform (dups on, joins on)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess>
                            <xsl:stylesheet
                                  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                  xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                                  version="1.0">
                              <xsl:template match="/">
                                  <tst:success didIt="true">Yup, that worked</tst:success>
                              </xsl:template>
                            </xsl:stylesheet>
                         </rax:preprocess>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true, true, true, "XalanC", true))
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL' and not(@href)]/xsl:stylesheet")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:stylesheet/xsl:template/tst:success/@didIt")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a']")
    assert(checker, "in-scope-prefixes(chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@id']")
    assert(checker, "in-scope-prefixes(chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@stepType']")
    assert(checker, "in-scope-prefixes(chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', chk:checker/chk:step[@type='XSL']/xsl:transform/xsl:template/xsl:choose/xsl:when[@test = '/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, XSL, ContentFail)
  }


  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transform") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL2.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    register("test://app/src/test/resources/xsl/testXSL2.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL2.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transforms in different reps") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform in diffrent reps")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                      <representation mediaType="application/atom+xml" element="tst:a">
                         <rax:preprocess href="src/test/resources/xsl/testXSL2.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    register("test://app/src/test/resources/xsl/testXSL2.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL2.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
   assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
           XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transforms in different reps (dups on)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform in diffrent reps (dups on)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                      <representation mediaType="application/atom+xml" element="tst:a">
                         <rax:preprocess href="src/test/resources/xsl/testXSL2.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    register("test://app/src/test/resources/xsl/testXSL2.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL2.xsl"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
   assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
           XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transforms in different reps (dups on, join on)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform in diffrent reps (dups on, join on)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                      <representation mediaType="application/atom+xml" element="tst:a">
                         <rax:preprocess href="src/test/resources/xsl/testXSL2.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    register("test://app/src/test/resources/xsl/testXSL2.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL2.xsl"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true, true, true, "XalanC", true))
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), XSL,
           XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transforms in different reps (dups on, join on, xpath2)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform in diffrent reps (dups on, join on, xpath2)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                      <representation mediaType="application/atom+xml" element="tst:a">
                         <rax:preprocess href="src/test/resources/xsl/testXSL2.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    register("test://app/src/test/resources/xsl/testXSL2.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL2.xsl"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 2, true, true, true, "XalanC", true))
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), XSL,
           XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting valid xml with elements specified and multiple required plain params, and a multiple XSL transforms in different reps (dups on 2)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and multiple XSL transform in diffrent reps (dups on 2)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                      <representation mediaType="application/atom+xml" element="tst:a">
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1, true, true, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][1]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a'][2]) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='1']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL' and @version='2']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 4")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, XSD, Accept)
   assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
           XSL, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/atom\\+xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XSL, ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified and multiple required plain params, and a single XSL transform") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, true, false, true, 1, true, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified  and multiple required plain params, and a single XSL transform (no well form check)") {
    Given ("a WADL that contains a POST operation with elemets specified and multiple plain params and a single XSL transform (no well form check)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, true, 1, true, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 3")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a","Expecting the root element to be: tst:a"),
            XPath("/tst:a/@id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified (but check disabled) and multiple required plain params, and a single XSL transform (no well form check)") {
    Given ("a WADL that contains a POST operation with elemets specified (but check disabled) and multiple plain params and a single XSL transform (no well form check)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, false, 1, true, false, true))
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "in-scope-prefixes(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'tst'")
    assert(checker, "namespace-uri-for-prefix('tst', /chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 'http://www.rackspace.com/repose/wadl/checker/step/test'")
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 2")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 1")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML,
            XPath("/tst:a/@id"), XPath("/tst:a/@stepType"), XSL, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XPath("/tst:a/@id"), ContentFail)
  }

  scenario("The WADL contains a POST  operation accepting xml with elements specified (but check disabled) and multiple required plain params (check disabled) , and a single XSL transform (no well form check)") {
    Given ("a WADL that contains a POST operation with elemets specified (but check disabled) and multiple plain params (check disabled) and a single XSL transform (no well form check)")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:tst="http://www.rackspace.com/repose/wadl/checker/step/test"
                   xmlns:rax="http://docs.rackspace.com/api">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml" element="tst:a">
                         <param name="id" style="plain" path="/tst:a/@id" required="true"/>
                         <param name="stepType" style="plain" path="/tst:a/@stepType" required="true"/>
                         <rax:preprocess href="src/test/resources/xsl/testXSL1.xsl"/>
                      </representation>
                  </request>
               </method>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    register("test://app/src/test/resources/xsl/testXSL1.xsl",
             XML.loadFile("src/test/resources/xsl/testXSL1.xsl"))
    val checker = builder.build (inWADL, TestConfig(false, false, false, false, false, 1, false, false, true))
    assert(checker, "count(/chk:checker/chk:step[@type='XSD']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XSL']) = 1")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@id']) = 0")
    assert(checker, "count(/chk:checker/chk:step[@type='XPATH' and @match='/tst:a/@stepType']) = 0")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSL, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
  }


  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header nodes they are used in the next
  //  couple of tests.
  //
  def reqTypeAndHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }


  //
  //  Like reqTypeAndHeaderAssertions, but assumes that duplicates
  //  have been removed
  //
  def reqTypeAndHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header nodes.  They are used in the next couple of tests.
  //
  def wellFormedAndHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  //  Like wellFormedAndHeaderAssertions, but assumes that remove dups
  //  optimization is on, and duplicates have been removed
  //
  def wellFormedAndHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header nodes. They are used in the next couple of tests.
  //
  def xsdAndHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"),Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"),Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  // Like xsdAndHeaderAssertions, but it's assumed that remove dups opt is on
  //
  def xsdAndHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxCodeHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The header states should have the appropriate header codes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403),Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxMessageHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The header states should have the appropriate header codes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","foo", "X-FOO,foo,bad"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","bar", "X-FOO,bar,bad"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","foo", "X-FOO,foo,bad"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","bar", "X-FOO,bar,bad"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","foo", "X-FOO,foo,bad"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","bar", "X-FOO,bar,bad"),
            Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","foo", "X-FOO,foo,bad"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "X-TEST, bad"),
            Header("X-TEST2", ".*", "X-TEST2, bad"), HeaderAny("X-FOO","bar", "X-FOO,bar,bad"),
            Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }


  def raxSameCodeHeaderDupsAssertion(checker : NodeSeq) : Unit = {
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 401),
            HeaderAny("X-FOO","foo|bar", 401), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 401),
            HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 401),
            HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 401),
            HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxSameMessageHeaderDupsAssertion(checker : NodeSeq) : Unit = {
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "No!"), Header("X-TEST2", ".*", "No!"),
            HeaderAny("X-FOO","foo|bar", "No!"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "No!"), Header("X-TEST2", ".*", "No!"),
            HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "No!"), Header("X-TEST2", ".*", "No!"),
            HeaderAny("X-FOO","foo|bar"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*", "No!"), Header("X-TEST2", ".*", "No!"),
            HeaderAny("X-FOO","foo|bar"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and XSD header nodes they are used in the
  //  next couple of tests.
  //
  def reqTypeAndXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and XSD header nodes.  They are used in the next
  //  couple of tests.
  //
  def wellFormedAndXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // XSD header nodes. They are used in the next couple of tests.
  //
  def xsdAndXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }


  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests.
  //
  def reqTypeAndHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }


  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests.
  //
  def wellFormedAndHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests.
  //
  def xsdAndHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests.
  //
  def reqTypeAndHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests.
  //
  def wellFormedAndHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests.
  //
  def xsdAndHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("PUT"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (rax:code)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:code="402" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:code="403" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxCodeHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (rax:message)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:message="X-TEST, bad" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:message="X-FOO,foo,bad" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:message="X-FOO,bar,bad" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:message="X-TEST2, bad" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxMessageHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (rax:code, rax:message)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:code="401" rax:message="X-TEST, bad" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:code="402" rax:message="X-FOO,foo,bad" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" rax:code="403" rax:message="X-FOO,bar,bad" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" rax:message="X-TEST2, bad" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxMessageHeaderAssertions(checker)
    raxCodeHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderDupsOnAssertions(checker)
    wellFormedAndHeaderDupsOnAssertions(checker)
    xsdAndHeaderDupsOnAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:code (same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="401" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="401" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:code="401" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderDupsOnAssertions(checker)
    wellFormedAndHeaderDupsOnAssertions(checker)
    xsdAndHeaderDupsOnAssertions(checker)
    raxSameCodeHeaderDupsAssertion(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:message (same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:message="No!" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="No!" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="No!" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:message="No!" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderDupsOnAssertions(checker)
    wellFormedAndHeaderDupsOnAssertions(checker)
    xsdAndHeaderDupsOnAssertions(checker)
    raxSameMessageHeaderDupsAssertion(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:code, rax:message (same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:message="No!" rax:code="401" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="No!" rax:code="401" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="No!" rax:code="401" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:message="No!" rax:code="401" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderDupsOnAssertions(checker)
    wellFormedAndHeaderDupsOnAssertions(checker)
    xsdAndHeaderDupsOnAssertions(checker)
    raxSameMessageHeaderDupsAssertion(checker)
    raxSameCodeHeaderDupsAssertion(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:code (different))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="402" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="403" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxCodeHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:message (different))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:message="X-TEST, bad" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="X-FOO,foo,bad" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:message="X-FOO,bar,bad" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:message="X-TEST2, bad" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxMessageHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, required headers must be checked (remove dups on, rax:code, rax:message (different))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, required headers must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" rax:code="401" rax:message="X-TEST, bad" required="true"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="402" rax:message="X-FOO,foo,bad" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string"  rax:code="403" rax:message="X-FOO,bar,bad" required="true" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" rax:message="X-TEST2, bad" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    raxMessageHeaderAssertions(checker)
    raxCodeHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
               <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
               <param name="X-FOO" style="header" type="xsd:string" required="false" fixed="bar"/>
               <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST3" style="header" type="xsd:string" required="false"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderAssertions(checker)
    wellFormedAndHeaderAssertions(checker)
    xsdAndHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndXSDHeaderAssertions(checker)
    wellFormedAndXSDHeaderAssertions(checker)
    xsdAndXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndXSDHeaderAssertions(checker)
    wellFormedAndXSDHeaderAssertions(checker)
    xsdAndXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header and header must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header and header must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderXSDHeaderAssertions(checker)
    wellFormedAndHeaderXSDHeaderAssertions(checker)
    xsdAndHeaderXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 6")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header and header must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header and header must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderXSDHeaderAssertions(checker)
    wellFormedAndHeaderXSDHeaderAssertions(checker)
    xsdAndHeaderXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 6")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderXSDHeader2Assertions(checker)
    wellFormedAndHeaderXSDHeader2Assertions(checker)
    xsdAndHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 8")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers, non req headers should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers, nonrequired headers should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER-INT" style="header" type="xsd:int" required="false"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndHeaderXSDHeader2Assertions(checker)
    wellFormedAndHeaderXSDHeader2Assertions(checker)
    xsdAndHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 8")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers, opt on") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD header and header must be checked, multiple similar Headers, opt on")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <param name="X-TEST" style="header" type="xsd:string" required="true"/>
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="PUT">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    true, true))
    reqTypeAndHeaderXSDHeader2Assertions(checker)
    wellFormedAndHeaderXSDHeader2Assertions(checker)
    xsdAndHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header nodes on PUT request they are used
  //  in the next couple of tests.
  //
  def reqTypeAndReqHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }


  //
  //  Like reqTypeAndReqHeaderAssertions, but we assume remove dups optimization
  //
  def reqTypeAndReqHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }


  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header nodes.  They are used in the next couple of tests.
  //
  def wellFormedAndReqHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  //  Like wellFormedAndReqHeaderAssertions, but we assume remove dups on optimization
  //
  def wellFormedAndReqHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header nodes. They are used in the next couple of tests.
  //
  def xsdAndReqHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","bar"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  // Like xsdAndReqHeaderAssertions, but we assume remove dups optimization
  //
  def xsdAndReqHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), Header("X-TEST2", ".*"), HeaderAny("X-FOO","foo|bar"),
            ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxCodeReqHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain header assertions with correct error code")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","foo", 402), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401), Header("X-TEST2", ".*", 404),
            HeaderAny("X-FOO","bar", 403), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxCodeReqHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain header assertions with the correct error code")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401),
            Header("X-TEST2", ".*", 401), HeaderAny("X-FOO","foo|bar", 401),
            ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", 401),
            Header("X-TEST2", ".*", 401), HeaderAny("X-FOO","foo|bar", 401),
            ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxMessageReqHeaderDupsOnAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain header assertions with the correct error code")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No!"),
            Header("X-TEST2", ".*", "No!"), HeaderAny("X-FOO","foo|bar", "No!"),
            ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No!"),
            Header("X-TEST2", ".*", "No!"), HeaderAny("X-FOO","foo|bar", "No!"),
            ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  def raxMessageReqHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain header assertions with correct error code")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No1"), Header("X-TEST2", ".*", "No4"),
            HeaderAny("X-FOO","foo", "No2"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No1"), Header("X-TEST2", ".*", "No4"),
            HeaderAny("X-FOO","bar", "No3"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)

    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No1"), Header("X-TEST2", ".*", "No4"),
            HeaderAny("X-FOO","foo", "No2"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*", "No1"), Header("X-TEST2", ".*", "No4"),
            HeaderAny("X-FOO","bar", "No3"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)

    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and XSD header nodes they are used in the
  //  next couple of tests.
  //
  def reqTypeAndReqXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and XSD header nodes.  They are used in the next
  //  couple of tests.
  //
  def wellFormedAndReqXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // XSD header nodes. They are used in the next couple of tests.
  //
  def xsdAndReqXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }


  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests.
  //
  def reqTypeAndReqHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqTypeFail)
  }


  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests.
  //
  def wellFormedAndReqHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests.
  //
  def xsdAndReqHeaderXSDHeaderAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests.
  //
  def reqTypeAndReqHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests.
  //
  def wellFormedAndReqHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests.
  //
  def xsdAndReqHeaderXSDHeader2Assertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests.
  //
  def reqTypeAndReqHeaderXSDHeader2MixAssertions(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/json)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqTypeFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqTypeFail)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests.
  //
  def wellFormedAndReqHeaderXSDHeader2MixAssertions(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/json)(;.*)?"), WellJSON)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/xml)(;.*)?"), WellXML)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/json)(;.*)?"), ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/xml)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests.
  //
  def xsdAndReqHeaderXSDHeader2MixAssertions(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (method ref)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method href="#headerMethod"/>
               <method href="#postOnAB"/>
           </resource>
           <resource path="/c">
               <method href="#postOnC"/>
               <method href="#getOnC"/>
           </resource>
        </resources>
        <method id="headerMethod" name="PUT">
          <request>
           <param name="X-TEST" style="header" type="xsd:string" required="true"/>
           <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
           <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
           <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
           <representation mediaType="application/xml"/>
           <representation mediaType="application/json"/>
         </request>
       </method>
       <method id="postOnAB" name="POST">
         <request>
           <representation mediaType="application/xml"/>
         </request>
       </method>
       <method id="postOnC" name="POST">
          <request>
            <representation mediaType="application/json"/>
          </request>
       </method>
       <method id="getOnC" name="GET"/>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (rax:code)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:code="402" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:code="403" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxCodeReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (rax:message)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:message="No1" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:message="No2" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:message="No3" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:message="No4" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxMessageReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (rax:code, rax:message)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" rax:message="No1" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:code="402" rax:message="No2" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string"  rax:code="403" rax:message="No3" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" rax:message="No4" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxCodeReqHeaderAssertions(checker)
    raxMessageReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on)") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderDupsOnAssertions(checker)
    wellFormedAndReqHeaderDupsOnAssertions(checker)
    xsdAndReqHeaderDupsOnAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:code(same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="401" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="401" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="401" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderDupsOnAssertions(checker)
    wellFormedAndReqHeaderDupsOnAssertions(checker)
    xsdAndReqHeaderDupsOnAssertions(checker)
    raxCodeReqHeaderDupsOnAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:message(same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:message="No!" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:message="No!" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:message="No!" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:message="No!" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderDupsOnAssertions(checker)
    wellFormedAndReqHeaderDupsOnAssertions(checker)
    xsdAndReqHeaderDupsOnAssertions(checker)
    raxMessageReqHeaderDupsOnAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:code, rax:message(same))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" rax:message="No!" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="401" rax:message="No!" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="401" rax:message="No!" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="401" rax:message="No!" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderDupsOnAssertions(checker)
    wellFormedAndReqHeaderDupsOnAssertions(checker)
    xsdAndReqHeaderDupsOnAssertions(checker)
    raxCodeReqHeaderDupsOnAssertions(checker)
    raxMessageReqHeaderDupsOnAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:code(diff))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="402" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="403" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxCodeReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:message(diff))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:message="No1" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:message="No2" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:message="No3" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:message="No4" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxMessageReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked (dups on, rax:code, rax:message(diff))") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT that must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:rax="http://docs.rackspace.com/api"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" rax:code="401" rax:message="No1" required="true"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="402" rax:message="No2" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" rax:code="403" rax:message="No3" required="true" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" rax:code="404" rax:message="No4" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    raxCodeReqHeaderAssertions(checker)
    raxMessageReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }


  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required header on a PUT that must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required header on a PUT must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="foo"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="true" fixed="bar"/>
                      <param name="X-FOO" style="header" type="xsd:string" required="false" fixed="bar"/>
                      <param name="X-TEST2" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST3" style="header" type="xsd:string" required="false"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderAssertions(checker)
    wellFormedAndReqHeaderAssertions(checker)
    xsdAndReqHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 7")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqXSDHeaderAssertions(checker)
    wellFormedAndReqXSDHeaderAssertions(checker)
    xsdAndReqXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqXSDHeaderAssertions(checker)
    wellFormedAndReqXSDHeaderAssertions(checker)
    xsdAndReqXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and header must be checked") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and header must be checked")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeaderAssertions(checker)
    wellFormedAndReqHeaderXSDHeaderAssertions(checker)
    xsdAndReqHeaderXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 6")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required XSD request header and request header must be checked, non-req should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required XSD request header and request header must be checked, non-req should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <method name="POST">
                  <request>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeaderAssertions(checker)
    wellFormedAndReqHeaderXSDHeaderAssertions(checker)
    xsdAndReqHeaderXSDHeaderAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 6")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeader2Assertions(checker)
    wellFormedAndReqHeaderXSDHeader2Assertions(checker)
    xsdAndReqHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 8")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, non req headers should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, nonrequired headers should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER-INT" style="header" type="xsd:int" required="false"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeader2Assertions(checker)
    wellFormedAndReqHeaderXSDHeader2Assertions(checker)
    xsdAndReqHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 8")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, opt on") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, opt on")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(true, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    true, true))
    reqTypeAndReqHeaderXSDHeader2Assertions(checker)
    wellFormedAndReqHeaderXSDHeader2Assertions(checker)
    xsdAndReqHeaderXSDHeader2Assertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 1")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, mixed, multiple similar Headers") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <method name="POST">
                  <request>
                      <param name="X-TEST-OTHER" style="header" type="xsd:date" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeader2MixAssertions(checker)
    wellFormedAndReqHeaderXSDHeader2MixAssertions(checker)
    xsdAndReqHeaderXSDHeader2MixAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 9")
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, mixed, non req headers should be ignored") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, nonrequired headers should be ignored")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER-INT" style="header" type="xsd:int" required="false"/>
               <method name="POST">
                  <request>
                      <param name="X-TEST-OTHER" style="header" type="xsd:date" required="true"/>
                      <representation mediaType="application/xml"/>
                      <representation mediaType="application/json"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeader2MixAssertions(checker)
    wellFormedAndReqHeaderXSDHeader2MixAssertions(checker)
    xsdAndReqHeaderXSDHeader2MixAssertions(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 3")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 9")
  }

  //
  //  The following assertions are used to test ReqType and
  //  ReqTypeFail nodes, and header and xsd header nodes they are used
  //  in the next couple of tests. Some Header nodes do not have a ReqType.
  //
  def reqTypeAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker : NodeSeq) : Unit = {
    Then("The machine should contain paths to all ReqTypes")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"))
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("GET"))
    And("ReqTypeFail states should be after PUT and POST states")
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqTypeFail)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), Accept)
  }

  //
  //  The following assertions are used to test WellFormXML,
  //  ContentError, and header and xsd header nodes.  They are used in
  //  the next couple of tests. Some Header nodes do not have a ReqType.
  //
  def wellFormedAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker : NodeSeq) : Unit = {
    And("The machine should contain paths to WellXML and WELLJSON types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), Accept)
    assert (checker, Start, URL("c"), HeaderXSD("X-TEST-INT", "xsd:int"), Method("POST"), HeaderXSD("X-TEST-OTHER", "xsd:date"), Accept)
    And("There should be content failed states")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), ContentFail)
  }

  //
  // The following assertions are used to test XSD, ContentError, and
  // header and xsd header nodes. They are used in the next couple of
  // tests. Some Header nodes do not have a ReqType.
  //
  def xsdAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker : NodeSeq) : Unit = {
    And("The machine should cantain paths to XSD types")
    assert (checker, Start, URL("a"), URL("b"), Method("PUT"), Header("X-TEST", ".*"), HeaderXSD("X-TEST-INT", "xsd:int"), Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, XSD, Accept)
    assert (checker, Start, URL("a"), URL("b"), Method("POST"), ReqType("(application/xml)(;.*)?"), WellXML, ContentFail)
  }

  scenario("The WADL contains PUT and POST operations accepting xml which must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, mixed, non req headers should be ignored, checks should occur even if no represetation type is specified.") {
    Given ("a WADL that contains multiple PUT and POST operation with XML that must validate against an XSD, a required request XSD header and request header must be checked, multiple similar Headers, nonrequired headers should be ignored. No representation types are sepecified.")
    val inWADL =
      <application xmlns="http://wadl.dev.java.net/2009/02"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <grammars>
            <include href="src/test/resources/xsd/test-urlxsd.xsd"/>
        </grammars>
        <resources base="https://test.api.openstack.com">
           <resource path="/a/b">
               <method name="PUT">
                  <request>
                      <param name="X-TEST" style="header" type="xsd:string" required="true"/>
                      <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
                      <param name="X-TEST-OTHER" style="header" type="xsd:string" required="false"/>
                  </request>
               </method>
               <method name="POST">
                  <request>
                      <representation mediaType="application/xml"/>
                  </request>
               </method>
           </resource>
           <resource path="/c">
               <param name="X-TEST-INT" style="header" type="xsd:int" required="true"/>
               <param name="X-TEST-OTHER-INT" style="header" type="xsd:int" required="false"/>
               <method name="POST">
                  <request>
                      <param name="X-TEST-OTHER" style="header" type="xsd:date" required="true"/>
                  </request>
               </method>
               <method name="GET"/>
           </resource>
        </resources>
    </application>
    register("test://app/src/test/resources/xsd/test-urlxsd.xsd",
             XML.loadFile("src/test/resources/xsd/test-urlxsd.xsd"))
    When("the wadl is translated")
    val checker = builder.build (inWADL, TestConfig(false, false, true, true, true, 1,
                                                    true, true, true, "XalanC",
                                                    false, true))
    reqTypeAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker)
    wellFormedAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker)
    xsdAndReqHeaderXSDHeader2MixAssertionsNoReqType(checker)
    And("The following assertions should also hold:")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='POST']) = 2")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='PUT']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='METHOD' and @match='GET']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE' and @match='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='REQ_TYPE_FAIL' and @notMatch='(?i)(application/xml)(;.*)?|(?i)(application/json)(;.*)?']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_XML']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='WELL_JSON']) = 0")
    assert (checker, "count(/chk:checker/chk:step[@type='XSD']) = 1")
    assert (checker, "count(/chk:checker/chk:step[@type='CONTENT_FAIL']) = 5")
  }
}
