/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 */

/*jshint browser: true, node: true, devel: true */
/*global ScramShaClient: true, sjcl, sendHttp, sendHttpHeader */

////////////////////////////////////////////////////////////////
// Environment magic
////////////////////////////////////////////////////////////////

//make module work in both Node and RequireJS
(function (createClient) {
  'use strict';

  if (typeof module !== 'undefined' && module.exports) { //Node
    module.exports = createClient(require('sjcl'), { //published sjcl module
      normalize: function (mode, value) {
        return String(value).normalize('NFKC');
      }
    }, function (method, uri, body, callback) {
      require('request')({ //custom http sender using request module
        method: method,
        uri: 'http://localhost' + uri,
        body: body,
        jar: true,
        headers: {
          'Content-Type': 'application/x-niagara-login-support'
        }
      }, function (err, response, body) {
        if (!err && response.statusCode === 200) {
          callback.ok(body);
        } else {
          callback.fail('auth failure');
        }
      });
    });
  } else if (typeof define === 'function' && define.amd) { //RequireJS
    define('ScramShaClient', ['sjcl', 'sendHttp', 'sendHttpHeader'], createClient);
  } else {
    //browser, assume all are globals
    ScramShaClient = createClient(sjcl, sendHttp, sendHttpHeader);
  }
}(function (sjcl, sendHttp, sendHttpHeader) {
  'use strict';

////////////////////////////////////////////////////////////////
// ScramShaClient
////////////////////////////////////////////////////////////////

  /* Scram Sha1 Client class */
  function ScramShaClient(authscheme, username, password, hash){

    //console.log("Entered ScramShaClient");
    //console.log("  authscheme '" + authscheme + "'");
    //console.log("  username '" + username + "'");
    //console.log("  password '" + password + "'");

    this._authscheme = authscheme;
    this._username = ScramShaClient._usernamePrep(username);
    this._password = ScramShaClient._passwordPrep(password);
    this._hash = hash;
  }

  ScramShaClient.generateGlibcSha512 = function (password, salt, count, length, hash, callback) {

    //console.log("Entered ScramShaClient.generateGlibcSha512");
    //console.log("  password '" + password + "'");
    //console.log("  salt '" + salt + "'");
    //console.log("  count '" + count + "'");
    //console.log("  length '" + length + "'");

    //sha512crypto will read the 'iterations' values as a 'rounds=X' value in the salt
    //
    //    $6$$rounds=X$79119c33f66fc18e
    //
    //Build that salt string here
    var combinedSalt = "$6$rounds=" + count + "$" + salt;

    //console.log("generateGlibcSha512 created the following combinedSalt '" + combinedSalt + "'");

    //sha512 crypt will generate /etc/shadow style hash of form:
    //
    //    $6$79119c33f66fc18e$lImapa93HJZsTnG4DKIhtV8D1vBBMSLHVRq3nmMKzqJAxvxsPDQhgXT.TTINOC7v.G3FQIv9CI5A7mCpmQABm1
    //
    //Create that hash here
    var glibcSha512Hash = sha512crypt(password, combinedSalt);

    //console.log("generateGlibcSha512 created the following shadow hash '" + glibcSha512Hash + "'");

    //Key deriving function expects ONLY the hashed password portion, strip off everything after
    //the last '$' character so that:
    //
    //    $6$79119c33f66fc18e$lImapa93HJZsTnG4DKIhtV8D1vBBMSLHVRq3nmMKzqJAxvxsPDQhgXT.TTINOC7v.G3FQIv9CI5A7mCpmQABm1
    //
    //becomes:
    //
    //   lImapa93HJZsTnG4DKIhtV8D1vBBMSLHVRq3nmMKzqJAxvxsPDQhgXT.TTINOC7v.G3FQIv9CI5A7mCpmQABm1
    glibcSha512Hash = glibcSha512Hash.substr(glibcSha512Hash.lastIndexOf('$') + 1);

    //console.log("generateGlibcSha512 created the following key '" + glibcSha512Hash + "'");

    //Pass result to provided callback function
    callback.ok(glibcSha512Hash);
  };

  ScramShaClient.generatePBKDF2 = function (password, salt, count, length, hash, callback) {

    //console.log("Entered ScramShaClient.generatePBKDF2");
    //console.log("  password '" + password + "'");
    //console.log("  salt '" + salt + "'");
    //console.log("  count '" + count + "'");
    //console.log("  length '" + length + "'");

    if (sjcl.misc.pbkdf2Async) {
      sjcl.misc.pbkdf2Async({
        password: password,
        salt: salt,
        count: count,
        length: length * 8,
        hash: hash
      }, function (err, result) {
        if (err) {
          callback.fail(err);
        } else {
          callback.ok(result);
        }
      });
    } else {
      callback.ok(sjcl.misc.pbkdf2(password, salt, count, length * 8, sjcl.misc.hmac, hash));
    }
  };

  // static placeholder for an eventual saslprep implementation
  ScramShaClient._usernamePrep = function(value){
    value = String(value).normalize('NFKC');
    //console.log("val = " + value);
    value = value.replace(/=/g, '=3D');
    //console.log("val = " + newValue);
    value = value.replace(/,/g,'=2C');
    //console.log("val = " + newValue);
    //return newValue;
    return value;
  };

  // static placeholder for an eventual saslprep implementation
  ScramShaClient._passwordPrep = function(value){
    value = String(value).normalize('NFKC');
    return value;
  };

  ScramShaClient._random = function(nBytes){

    var bits = [];
    for (var i = 0; i < nBytes; i++) {
      bits.push((Math.random() * 0x100000000) | 0);
    }

    return bits;
  };

  /* method for parsing a key value pair, comma delimited list. */
  ScramShaClient._parseMessage = function(message){
    var i, tuples = message.split(','), values = {};
    for (i = 0; i < tuples.length; i++){
      var equals = tuples[i].indexOf('=');
      if (equals > 0){
        var key = tuples[i].substring(0, equals);
        var value = tuples[i].substring(equals+1);
        values[key] = value;
      }
    }

    return values;
  };

  /* static method for creating a salted password */
  ScramShaClient._createSaltedPassword = function (authscheme, password, salt, iterationCount, hash, callback) {

    //NOTE: salt is Base64 encoded string

    //console.log("Entered ScramShaClient._createSaltedPassword");
    //console.log("  authscheme '" + authscheme + "'");
    //console.log("  password '" + password + "'");
    //console.log("  salt '" + salt + "'");
    //console.log("  iterationCount '" + iterationCount + "'");
    //console.log("  hash '" + hash + "'");

    if (authscheme === "scram-glibc-sha512")
    {
      //This is almost certainly platform debug login provided by plat-login.js (NCCB-20698)

      //NOTE: generateGlibcSha512 expects string for salt (NzkxMTljMzNmNjZmYzE4ZQ== -> 79119c33f66fc18e)
      var stringSalt = "";
      //Convert the base64 encoded salt string to its decoded bytes
      var binary_string =  window.atob(salt);
      //Iterate through base64 byte value, converting to character string
      var len = binary_string.length;
      for (var i = 0; i < len; i++)
      {
        stringSalt += binary_string.charAt(i);
      }

      //console.log("_createSaltedPassword converted Base64 salt '" + salt + "' to string '" + stringSalt + "'");

      ScramShaClient.generateGlibcSha512(password, stringSalt, iterationCount, 0, hash, callback);
    }
    else //if (authscheme.startsWith("scram-sha"))
    {
      //This *might* be a platform debug login provided by plat-login.js (QNX "scram-sha512" uses pbdkf2) but
      //it also might be from POST / header authenticate call that did not provide an auth type (defaults to "scram-sha512")
      //Although all scram "actions" were previously hardcoded as "scram-sha512", the behavior is really determined by the 'hash' function
      //that was provided to the ScramShaClient.authenticate() function

      //NOTE: generatePBKDF2 expect bit salt
      var bitSalt = sjcl.codec.base64.toBits(salt);
      ScramShaClient.generatePBKDF2(password, bitSalt, iterationCount, 0, hash, callback);
    }
  };

  /* static method for creating the client first message bare */
  ScramShaClient._createClientFirstMessageBare = function(userName, clientNonce){
    var clientFirstMessageBare = "n=" + userName + ",r=" + clientNonce;
    return clientFirstMessageBare;
  };

  /* static method for create the client final message without proof */
  ScramShaClient._createClientFinalMessageWithoutProof = function(message){
    var values = ScramShaClient._parseMessage(message);
    var clientFinalMessageWithoutProof = 'c=biws,r=' + values.r;
    return clientFinalMessageWithoutProof;
  };

  /* static method for creating the auth message */
  ScramShaClient._createAuthMessage = function(clientFirstMessageBare, serverFirstMessage, clientFinalMessageWithoutProof){
    var authMessage = clientFirstMessageBare + ',' + serverFirstMessage + ',' + clientFinalMessageWithoutProof;
    return authMessage;
  };

  ScramShaClient._hmacSha = function(key, message, hash){
    if (typeof key === "string") {
      key = sjcl.codec.utf8String.toBits(key);
    }
    var hasher = new sjcl.misc.hmac(key, hash);
    return hasher.encrypt(message);
  };

  /* static method for creating the client proof */
  ScramShaClient._createClientProof = function(saltedPassword, authMessage, hash){
    var i;
    var clientKey = ScramShaClient._hmacSha(saltedPassword, "Client Key", hash);
    var storedKey = hash.hash(clientKey);
    var clientSignature = ScramShaClient._hmacSha(storedKey, authMessage, hash);
    var clientProof = [];
    for (i = 0; i < clientKey.length; i++)
    {
      clientProof[i] = clientKey[i] ^ clientSignature[i];
    }

    return clientProof;
  };

  /* static method for creating the server signature */
  ScramShaClient._createServerSignature = function(saltedPassword, authMessage, hash){
    var serverKey = ScramShaClient._hmacSha(saltedPassword, "Server Key", hash);
    var serverSignature = ScramShaClient._hmacSha(serverKey, authMessage, hash);
    return serverSignature;
  };

  /* instance method for creating the client first message that will be sent to the server */
  ScramShaClient.prototype.createClientFirstMessage = function(){
    //var clientNonce = sjcl.random.randomWords(16);
    var clientNonce = ScramShaClient._random(4);
    this._clientNonce = sjcl.codec.base64.fromBits(clientNonce);
    this._clientFirstMessageBare = ScramShaClient._createClientFirstMessageBare(this._username, this._clientNonce);
    var clientFirstMessage = "n,," + this._clientFirstMessageBare;
    return clientFirstMessage;
  };

  /* instance method for creating the client final message for the server */
  ScramShaClient.prototype.createClientFinalMessage = function (serverFirstMessage, callback)
  {
    var values = ScramShaClient._parseMessage(serverFirstMessage),
      that = this;

    /* lets make sure that the client nonce matches what we sent */
    if (this._clientNonce !== values.r.substring(0, this._clientNonce.length)){
      return callback.fail("invalid client nonce");
    }

    ScramShaClient._createSaltedPassword(this._authscheme, this._password, values.s, values.i, this._hash, {
      ok: function (saltedPassword) {
        that._saltedPassword = saltedPassword;
        var clientFinalMessageWithoutProof = ScramShaClient._createClientFinalMessageWithoutProof(serverFirstMessage);
        that._authMessage = ScramShaClient._createAuthMessage(that._clientFirstMessageBare, serverFirstMessage, clientFinalMessageWithoutProof);
        var clientProof = ScramShaClient._createClientProof(saltedPassword, that._authMessage, that._hash);
        var clientFinalMessage = clientFinalMessageWithoutProof + ',p=' +  sjcl.codec.base64.fromBits(clientProof);
        callback.ok(clientFinalMessage);
      },
      fail: callback.fail
    });
  };

  /* instance method for validating the final server response */
  ScramShaClient.prototype.processServerFinalMessage = function(serverFinalMessage){
    var values = ScramShaClient._parseMessage(serverFinalMessage);
    var serverSignature = ScramShaClient._createServerSignature(this._saltedPassword, this._authMessage, this._hash);
    var remoteServerSignature = sjcl.codec.base64.toBits(values.v);

    if (remoteServerSignature.toString() !== serverSignature.toString()){
      throw new Error("invalid server signature");
    }
  };

////////////////////////////////////////////////////////////////
// "POST" authentication
////////////////////////////////////////////////////////////////

  //Known external calls:
  //
  //  fw\workbench\workbench-wb\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\wiresheet\wiresheet-ux\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\webEditors\webEditors-ux\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\web\web-rt\src\com\tridium\web\rc\digestLoginN4.js(20)
  //  fw\web\web-rt\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\smartTableHx\smartTableHx-wb\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\schedule\schedule-wb\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\js\js-ux\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\nre\src\test\rc\spec\ScramSha256ClientSpec.js(25)
  //  fw\nre\src\test\rc\spec\ScramSha256JsSpec.js(153)
  //  fw\nre\src\test\rc\spec\ScramSha256JsSpec.js(175)
  //  fw\nre\src\test\rc\spec\ScramSha256JsSpec.js(256)
  //  fw\nre\src\test\rc\spec\ScramSha256JsSpec.js(279)
  //  fw\hx\hx-wb\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\bajaux\bajaux-ux\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)
  //  fw\bajaScript\bajaScript-ux\node_modules\niagara-test-server\lib\http\routes\public\browserLogin.js(86)

  /* static method to simply perform an authentication using scram-sha */
  ScramShaClient.authenticate = function(absPathBase, username, password, hash, callback){
    try{

      //console.log("Entered POST ScramShaClient.authenticate");
      //console.log("  absPathBase '" + absPathBase + "'");
      //console.log("  username '" + username + "'");
      //console.log("  password '" + password + "'");
      //console.log("  hash '" + hash + "'");

      //NOTE: Provide default 'authscheme' of "scram-sha512" to maintain any previously hardcoded behaviors
      //      Actual hash function used by pbkdf2 will be determined by the 'hash' function provided
      var client = new ScramShaClient("scram-sha512", username, password, hash);
      var clientFirstMessage = client.createClientFirstMessage();
      //NOTE: POST authenticates DON'T prepend the authentication scheme to the scram action (unlike header)
      var body="action=sendClientFirstMessage&clientFirstMessage=" + clientFirstMessage;
      sendHttp("POST", absPathBase + "j_security_check/", body, {
        ok:function(message){
          ScramShaClient.receiveServerFirstMessage(absPathBase, client, message, callback);
        },
        fail:function(cause){
          callback.fail(cause);
        }
      });
    }
    catch(e){
      callback.fail(e);
    }
  };

  /* static method to handle the server response to the first message */
  ScramShaClient.receiveServerFirstMessage = function(absPathBase, client, message, callback){
    client.createClientFinalMessage(message, {
      ok: function (clientFinalMessage) {
        //NOTE: POST authenticates DON'T prepend the authentication scheme to the scram action (unlike header)
        var body="action=sendClientFinalMessage&clientFinalMessage=" + clientFinalMessage;
        sendHttp("POST", absPathBase + "j_security_check/", body, {
          ok: function (message) {
            ScramShaClient.receiveServerFinalMessage(client, message, callback);
          },
          fail: function (cause) {
            callback.fail(cause);
          }
        });
      },
      fail: callback.fail
    });
  };

  /* static method to handle the server final response */
  ScramShaClient.receiveServerFinalMessage = function(client, message, callback){
    try{
      client.processServerFinalMessage(message);
      callback.ok("authenticated");
    }
    catch(e){
      callback.fail(e);
    }
  };

////////////////////////////////////////////////////////////////
// Header authentication
////////////////////////////////////////////////////////////////

  //Known external calls:
  //
  //  fw\plat\niagarad\src\com\tridium\niagarad\http\rc\plat-login.js(85)

  /* static method to simply perform an authentication using scram-*
   * using HTTP authorization headers
   */
  ScramShaClient.headerAuthenticate = function(absPathBase, authscheme, username, password, hash, callback){
    try{

      //console.log("Entered Header ScramShaClient.headerAuthenticate");
      //console.log("  absPathBase '" + absPathBase + "'");
      //console.log("  authscheme '" + authscheme + "'");
      //console.log("  username '" + username + "'");
      //console.log("  password '" + password + "'");
      //console.log("  hash '" + hash + "'");

      var client = new ScramShaClient(authscheme, username, password, hash);
      var clientFirstMessage = client.createClientFirstMessage();
      var body=client._authscheme + " action=sendClientFirstMessage clientFirstMessage=" + clientFirstMessage;
      sendHttpHeader(absPathBase, body, {
        ok:function(message){
          ScramShaClient.headerReceiveServerFirstMessage(absPathBase, client, message, callback);
        },
        fail:function(cause){
          callback.fail(cause);
        }
      });
    }
    catch(e){
      callback.fail(e);
    }
  };

  /* static method to handle the server response to the first message
   * using HTTP authentication headers
   */
  ScramShaClient.headerReceiveServerFirstMessage = function(absPathBase, client, message, callback){
    // Parse out the actual scram-sha message
    var serverFirstMessage = message.substring(message.indexOf("serverFirstMessage"));
    serverFirstMessage = serverFirstMessage.substring(serverFirstMessage.indexOf("=")+1);
    // Process the message
    client.createClientFinalMessage(serverFirstMessage, {
      ok: function (clientFinalMessage) {
        var body=client._authscheme + " action=sendClientFinalMessage clientFinalMessage=" + clientFinalMessage;
        sendHttpHeader(absPathBase, body, {
          ok: function (message) {
            ScramShaClient.headerReceiveServerFinalMessage(client, message, callback);
          },
          fail: function (cause) {
            callback.fail(cause);
          }
        });
      },
      fail: callback.fail
    });
  };

  /* static method to handle the server final response */
  ScramShaClient.headerReceiveServerFinalMessage = function(client, message, callback){
    try{
      // Parse out the actual scram-sha message
      var serverFinalMessage = message.substring(message.indexOf("serverFinalMessage"));
      serverFinalMessage = serverFinalMessage.substring(serverFinalMessage.indexOf("=")+1);
      // Process the message
      client.processServerFinalMessage(serverFinalMessage);
      callback.ok("authenticated");
    }
    catch(e){
      callback.fail(e);
    }
  };

  return ScramShaClient;
}));
