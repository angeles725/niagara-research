/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 */

var login = new Login();

(function() {
  "use strict";

  /**
   * Return the parameter value for the key in the URI's query.
   *
   * @param  {String} key The parameter key to search for.
   * @return {String} The parameter value or null if nothing can be found.
   */
  function querySearch(key) {
    var match,
        value;

    key = key.replace(/[*+?^$.\[\]{}()|\\\/]/g, "\\$&"); // escape RegEx control chars
    match = location.search.match(new RegExp("[?&]" + key + "=([^&]+)(&|$)"));

    if (match) {
      value = decodeURIComponent(match[1].replace(/\+/g, " "));
    }

    return value || null;
  }

  // Show authentication failed if listed as a query parameter
  var auth = querySearch("auth");
  if (auth && auth === "fail") {
    document.getElementById("login-failed").setAttribute("style", "display: block");
  }
}());

/**
 * Since IE doesn't apply style sheets to iframes, add the login style sheet
 * to the license file's iframe after the iframe is loaded
 */
function fixStyle() {
  "use strict";
  // create stylesheet
  var iframe = document.getElementById("licenseFile");

  var innerDoc = iframe.contentWindow.document;
  if (innerDoc) {
    var ss  = innerDoc.createElement("link");
    ss.type = "text/css";
    ss.rel  = "stylesheet";
    ss.href = "/login/loginN4.css";

    // apply to iframe's head
    if (document.all) {
      innerDoc.createStyleSheet(ss.href);
    }
    else {
      var head = innerDoc.getElementsByTagName("head")[0];
      head.appendChild(ss);
    }
  }
}

function Login()
{
////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////
  this.doLogin = function()
  {
    //NOTE: These values are defined in PlatformLoginServlet.java

    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var domain = document.getElementById("authdomain").value;
    var scheme = document.getElementById("authscheme").value;

    //console.log("Entered doLogin:");
    //console.log("  authscheme '" + scheme + "'");
    //console.log("  user '" + username + "'");
    //console.log("  password '" + password + "'");
    //console.log("  authdomain '" + domain + "'");

    var absPathElement = document.getElementById("absPathBase");
    var absPathBase = "/";
    if (absPathElement != null)
      absPathBase = absPathElement.value;

    ScramShaClient.headerAuthenticate(absPathBase, scheme, username, password, sjcl.hash.sha512, {
      ok: function(response){
        //console.log("Response: " + response);
        window.location.replace(absPathBase);
      },
      fail: function(error){
        //console.error("Error: " + error);
        window.location.replace(absPathBase+"login?auth=fail");
      }
    });
    return false;
  }
}


function hideAddressBar() {
  setTimeout(function () {
    window.scrollTo(0, 1);
  }, 100);
}

function init()
{
  hideAddressBar();
  document.getElementById("username").focus();
}

function doLogin(arg)
{
  return login.doLogin()
}
