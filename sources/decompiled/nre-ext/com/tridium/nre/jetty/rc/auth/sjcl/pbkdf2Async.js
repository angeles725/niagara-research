/*jslint white: true, plusplus: true, bitwise: true, browser: true */
/*global sjcl */

/** 
 * @fileOverview Password-based key-derivation function, version 2.0.
 * 
 * With modifications to run asynchronously by Logan Byam @ Tridium
 *
 * @author Emily Stark
 * @author Mike Hamburg
 * @author Dan Boneh
 * @author Logan Byam
 */

/** 
 * Password-Based Key-Derivation Function, version 2.0.
 *
 * Generate keys from passwords using PBKDF2-HMAC-SHA256.
 *
 * This is the method specified by RSA's PKCS #5 standard.
 * 
 * This is the original SJCL function, refactored and altered to run async.
 * Gives up control to the browser in between each chunk with
 * <code>setTimeout(0)</code>, which prevents IE from throwing up long-running
 * script errors.
 *
 * @param {Object} config
 * 
 * @param {bitArray|String} config.password The password.
 * 
 * @param {bitArray|String} config.salt The salt.  Should have lots of entropy.
 * 
 * @param {Number} [config.count=1000] The number of iterations.  Higher 
 *  numbers make the function slower but more secure.
 * 
 * @param {Number} [config.length] The length of the derived key.  Defaults 
 *  to the output size of the hash function.
 *  
 * @param {Object} [config.Prff=sjcl.misc.hmac] The pseudorandom function family.
 * 
 * @param {Number} [config.chunkSize=1024] the number of iterations to perform
 *  each time before giving up control to the browser (defaults to 1024)
 * 
 * @param {Function} callback called with the key derivation is complete.
 * first parameter is error or null, second parameter is the derived key.
 * 
 * @return {bitArray} derived key, passed to callback function.
 */
sjcl.misc.pbkdf2Async = function (config, callback) {
  
  "use strict";
  
  var password = config.password,
      salt = config.salt,
      count = config.count || 10000,
      length = config.length || 0,
      chunkSize = config.chunkSize || 1024,
      Prff = config.Prff || sjcl.misc.hmac,
      hash = config.hash || sjcl.hash.sha256,
      b = sjcl.bitArray,
      prf;
  
  if (!salt || length < 0 || count < 0) {
    throw new sjcl.exception.invalid("invalid params to pbkdf2");
  }

  if (!password) {
	  password = [];
  }
  
  if (typeof password === "string") {
	  password = sjcl.codec.utf8String.toBits(password);
  }

  if (typeof salt === "string") {
    salt = sjcl.codec.utf8String.toBits(salt);
  }
  
  prf = new Prff(password, hash);

  
  /**
   * Asynchronously perform a chunk of encryption/XORs on the source
   * and destination arrays.
   * @inner
   * @ignore
   * @param {Number} iterations the number of iterations to perform
   * @param {Array} srcArray the source array
   * @param {Array} destArray the destination array
   * @param {Function} callback will be called when all iterations are
   * complete. Receives error and result parameters.
   * @return {bitArray} the processed/encrypted result array, passed to
   * callback
   */
  function doIterations(iterations, srcArray, destArray, callback) {
    var i,
        j,
        p = prf,
        todo = Math.min(iterations, chunkSize);
    
    if (todo <= 0) {
      return callback(null, destArray);
    }    
    
    try {
      for (i = 0; i < todo; i++) {
        srcArray = p.encrypt(srcArray);
        for (j = 0; j < srcArray.length; j++) {
          destArray[j] ^= srcArray[j];
        }
      }
    } catch (e) {
      return callback(e);
    }
    
    setTimeout(function () {
      doIterations(iterations - todo, srcArray, destArray, callback);
    }, 0);
  }
  
  /**
   * Generates one block of the derived key.
   * @inner
   * @ignore
   * @param {Number} blockIndex
   * @param {Array} derivedKey the derived key in progress. Begins as an
   * empty array.
   * @param {Function} callback will be called with the derived key has
   * completed processing. Receives error and result parameters.
   * @return {bitArray} the complete derived key, passed to callback
   */
  function generateBlock(blockIndex, derivedKey, callback) {
    var saltAndIndex = prf.encrypt(b.concat(salt, [ blockIndex ]));
    
    doIterations(count - 1, saltAndIndex, saltAndIndex, function (err, result) {
      if (err) {
        return callback(err);
      }
      
      derivedKey = derivedKey.concat(result);
      
      if (32 * derivedKey.length < (length || 1)) {
        generateBlock(blockIndex + 1, derivedKey, callback);
      } else {
        callback(null, derivedKey);
      }
    });
  }
  
  generateBlock(1, [], function (err, derivedKey) {
    if (err) {
      return callback(err);
    }
    
    if (length) {
      derivedKey = b.clamp(derivedKey, length); 
    }
  
    callback(null, derivedKey);
  });
};
