import CryptoJS from 'crypto-js';

export const getEncodedParam = (data, beforeParamsRaw) => {
  let pm = {
    request: {
      body: {
        raw: data,
        mode: '',
        urlencoded: ''
      }
    }
  };
  const { appId, appSecret, version, clientType, domain } = beforeParamsRaw;
  // 生成时间戳
  let timestamp = Math.round(new Date());
  // 业务参数加密
  let paramsRaw = pm.request.body.raw;
  paramsRaw['appId'] = appId;
  paramsRaw['clientType'] = clientType;
  paramsRaw['version'] = version;
  paramsRaw['timestamp'] = timestamp;
  paramsRaw['domain'] = domain;
  var sb = buildSignQueryString(paramsRaw);
  let encryptParams = xxtea_encrypt(sb, appSecret).toUpperCase();
  // console.log(encryptParams);

  // 生成签名
  let signParams = appId + clientType + encryptParams + timestamp + version;
  let sign = CryptoJS.HmacSHA256(signParams, appSecret).toString();

  // pm.request.headers.upsert({
  //     key: 'Content-Type',
  //     value: 'application/x-www-form-urlencoded;chatset=utf-8'
  // });
  pm.request.body.mode = 'urlencoded';
  pm.request.body.urlencoded =
    'appId=' +
    appId +
    '&version=' +
    version +
    '&domain=' +
    domain +
    '&clientType=' +
    clientType +
    '&timestamp=' +
    timestamp +
    '&params=' +
    encryptParams +
    '&signature=' +
    sign;
  pm.request.body.encryptParams = encryptParams;
  return pm;
};
function buildSignQueryString(params) {
  var keys = Object.keys(params).sort();
  var queryString = '';
  keys.forEach((key) => {
    var val = params[key];
    if (key === undefined || key == null || key.length === 0) {
      return;
    }
    if (queryString.length > 0) {
      queryString += '&';
    }
    queryString += key + '=';
    switch (typeof val) {
      case 'object':
        queryString += JSON.stringify(val, (k, v) => (typeof v === 'undefined' ? null : v));
        break;
      default:
        queryString += val;
        break;
    }
  });
  return queryString;
}
function xxtea_encrypt(str, key) {
  function long2str(v, w) {
    var vl = v.length;
    var sl = v[vl - 1] & 0xffffffff;
    for (var i = 0; i < vl; i++) {
      v[i] = String.fromCharCode(
        v[i] & 0xff,
        (v[i] >>> 8) & 0xff,
        (v[i] >>> 16) & 0xff,
        (v[i] >>> 24) & 0xff
      );
    }
    if (w) {
      return v.join('').substring(0, sl);
    } else {
      return v.join('');
    }
  }

  function str2long(s, w) {
    var len = s.length;
    var v = [];
    for (var i = 0; i < len; i += 4) {
      v[i >> 2] =
        s.charCodeAt(i) |
        (s.charCodeAt(i + 1) << 8) |
        (s.charCodeAt(i + 2) << 16) |
        (s.charCodeAt(i + 3) << 24);
    }
    if (w) {
      v[v.length] = len;
    }
    return v;
  }

  function str2Hex(input) {
    var output = '';
    var chr1 = '';
    var i = 0;
    do {
      chr1 = input.charCodeAt(i++).toString(16);
      if (chr1.length === 1) chr1 = '0' + chr1;
      output += chr1;
    } while (i < input.length);
    return output;
  }

  if (str === '') {
    return '';
  }
  var v = str2long(str, true);
  var k = str2long(key, false);
  var n = v.length - 1;

  var z = v[n];
  var y = v[0];
  var delta = 0x9e3779b9;
  var mx;
  var e;
  var q = Math.floor(6 + 52 / (n + 1));
  var sum = 0;
  while (q-- > 0) {
    sum = (sum + delta) & 0xffffffff;
    e = (sum >>> 2) & 3;
    for (var p = 0; p < n; p++) {
      y = v[p + 1];
      mx = (((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4))) ^ ((sum ^ y) + (k[(p & 3) ^ e] ^ z));
      z = v[p] = (v[p] + mx) & 0xffffffff;
    }
    y = v[0];
    mx = (((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4))) ^ ((sum ^ y) + (k[(p & 3) ^ e] ^ z));
    z = v[n] = (v[n] + mx) & 0xffffffff;
  }

  return str2Hex(long2str(v, false));
}
