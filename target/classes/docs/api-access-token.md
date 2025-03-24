# API 验证机制

> 多吉云服务端 API 访问的 `AccessToken` 机制

服务端 API 很多属于敏感的操作，原则上 **所有 API 均应在业务服务端进行** 。为了验证调用方的身份和保证调用方的信息安全，多吉云服务端 API 采用 `AccessToken` 访问机制。

因此，对于不带 `AccessToken` 的请求，API 将返回 `ERROR_UNAUTHORIZED` 错误。

## 传递方法

调用 API 时需要同时传递 `AccessToken`，具体来说就是需要在 HTTP 请求头部增加一个 `Authorization` 字段，其值的格式为 `TOKEN <AccessToken>`，一个简单的示例如下：

```http
GET /oss/bucket/list.json?a=1&b=2&c=3 HTTP/1.1
Host: api.dogecloud.com
Authorization: TOKEN <AccessToken>
```

## 算法

`AccessToken` 的加密部分是用你的 `SecretKey` 对请求进行 `HMAC-SHA1` 加密的计算值，其具体步骤如下。

 - 将 API 请求地址的 `REQUEST_URI` 与请求内容（HTTP Body）用 `\n` （换行符，是一个字符，而不是`\`+`n`）连接起来。如果该请求没有 HTTP Body，也必须将其视为空字符串。

 ```javascript
signStr = "/oss/bucket/list.json?a=1&b=2&c=3" + "\n" + ""
 ```

 - 将得到的字符串用你的 `SecretKey` 作为密钥进行 `HMAC-SHA1` 加密签名。

 ```javascript
sign = hmac_sha1(signStr, "YOUR_SECRET_KEY")
 ```

 !> **注意** 如果你使用的 `HMAC-SHA1` 算法得到的是二进制数据，需要将其转为十六进制 Hex 数据。如果是 Base64 则需要解码并转为 Hex 。

 - 将你的 `AccessKey` 与得到的加密值用 `:` 连接起来，就构成了 `AccessToken`。

 ```javascript
accessToken = "YOUR_ACCESS_KEY" + ":" + sign
 ```

### 算法示例

以 Python 为例，该算法过程为：

```python
from hashlib import sha1
import hmac

AccessKey = "MY_ACCESS_KEY"
SecretKey = "MY_SECRET_KEY"

#现在我们要请求这个 API 地址
# https://api.dogecloud.com/auth/upload.json?filename=a.mp4

#那么此例需要签名的原始字符串是
signStr = "/auth/upload.json?filename=a.mp4" + "\n" + "" #(最后这个 "" 是 Body，此例不需要请求 Body，所以为空)

#对字符串进行签名
signedData = hmac.new(SecretKey.encode(), signStr.encode('utf-8'), sha1)

#获取签名字符串
sign = signedData.digest().hex()
#此例应得到 bf5ec167c882d6ffa8afa4a1d2c2ed8d622beadf

#将 AccessKey 与签名字符串连接
accessToken = AccessKey + ":" + sign
#此例应得到 MY_ACCESS_KEY:bf5ec167c882d6ffa8afa4a1d2c2ed8d622beadf

#生成最终的 Authorization 请求头值
Authorization = "TOKEN " + accessToken
```

PHP：

```php
$accessKey = "MY_ACCESS_KEY";
$secretKey = "MY_SECRET_KEY";

// 现在我们要请求这个 API 地址
// https://api.dogecloud.com/auth/upload.json?filename=a.mp4

// 那么此例需要签名的原始字符串是
$signStr = "/auth/upload.json?filename=a.mp4" . "\n" . "";
// (最后这个 "" 是 Body，此例不需要请求 Body，所以为空，如果你的请求有 Body，需设置为 Body 的内容)

// 对字符串进行签名
$sign = hash_hmac('sha1', $signStr, $secretKey);

// 生成最终的 Authorization 请求头值
$authorization = "TOKEN " . $accessKey . ":" . $sign;

```

## 简易调用函数

为了方便你使用，我们为你封装了各**服务端**语言调用多吉云 API 的简易函数，请参考。

<!-- tabs:start -->

### **PHP**

?> **说明** <a name="php" id="php"></a>该简易函数使用 `cURL` 库访问网络，如果你的 PHP 环境未配置 cURL 证书，请下载证书文件 [cacert.pem](https://curl.se/ca/cacert.pem)，将其保存到 PHP 安装路径下，然后在 `php.ini` 文件里配置 `curl.cainfo` 为 cacert.pem 的绝对路径。

```php
/**
 * 调用多吉云 API
 *
 * @param string    $apiPath    调用的 API 接口地址，包含 URL 请求参数 QueryString，例如：/console/vfetch/add.json?url=xxx&a=1&b=2
 * @param array     $data       POST 的数据，关联数组，例如 array('a' => 1, 'b' => 2)，传递此参数表示不是 GET 请求而是 POST 请求
 * @param boolean   $jsonMode   数据 data 是否以 JSON 格式请求，默认为 false 则使用表单形式（a=1&b=2）
 * 
 * @author 多吉云
 * @return array 返回的数据
 */ 
function dogecloud_api($apiPath, $data = array(), $jsonMode = false) {
    $accessKey = "你的AccessKey"; // 可在用户中心 - 密钥管理中查看
    $secretKey = "你的SecretKey"; // 请勿在客户端暴露 AccessKey 和 SecretKey，否则恶意用户将获得账号完全控制权
    $body = $jsonMode ? json_encode($data) : http_build_query($data);
    $signStr = $apiPath . "\n" . $body;
    $sign = hash_hmac('sha1', $signStr, $secretKey);
    $Authorization = "TOKEN " . $accessKey . ":" . $sign;

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, "https://api.dogecloud.com" . $apiPath);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 1); // 如果是本地调试，或者根本不在乎中间人攻击，可以把这里的 1 和 2 修改为 0，就可以避免报错
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2); // 建议实际使用环境下 cURL 还是配置好本地证书
    if(isset($data) && $data){
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Content-Type: ' . ($jsonMode ? 'application/json' : 'application/x-www-form-urlencoded'),
        'Authorization: ' . $Authorization
    ));
    $ret = curl_exec($ch);
    curl_close($ch);
    return json_decode($ret, true);
}
```

### **Python**

?> **说明** <a name="python" id="python"></a>该简易函数请勿用于 Python 客户端，否则将暴露你的密钥，恶意用户将获得账号完全控制权。

```python
from hashlib import sha1
import hmac
import requests
import json
import urllib

def dogecloud_api(api_path, data={}, json_mode=False):
    """
    调用多吉云API

    :param api_path:    调用的 API 接口地址，包含 URL 请求参数 QueryString，例如：/console/vfetch/add.json?url=xxx&a=1&b=2
    :param data:        POST 的数据，字典，例如 {'a': 1, 'b': 2}，传递此参数表示不是 GET 请求而是 POST 请求
    :param json_mode:   数据 data 是否以 JSON 格式请求，默认为 false 则使用表单形式（a=1&b=2）

    :type api_path: string
    :type data: dict
    :type json_mode bool

    :return dict: 返回的数据
    """

    # 这里替换为你的多吉云永久 AccessKey 和 SecretKey，可在用户中心 - 密钥管理中查看
    # 请勿在客户端暴露 AccessKey 和 SecretKey，否则恶意用户将获得账号完全控制权
    access_key = '你的AccessKey'
    secret_key = '你的SecretKey'

    body = ''
    mime = ''
    if json_mode:
        body = json.dumps(data)
        mime = 'application/json'
    else:
        body = urllib.parse.urlencode(data) # Python 2 可以直接用 urllib.urlencode
        mime = 'application/x-www-form-urlencoded'
    sign_str = api_path + "\n" + body
    signed_data = hmac.new(secret_key.encode('utf-8'), sign_str.encode('utf-8'), sha1)
    sign = signed_data.digest().hex()
    authorization = 'TOKEN ' + access_key + ':' + sign
    response = requests.post('https://api.dogecloud.com' + api_path, data=body, headers = {
        'Authorization': authorization,
        'Content-Type': mime
    })
    return response.json()
```

### **Node.js**

?> **说明** <a name="nodejs" id="nodejs"></a>该简易函数使用 `axios` 库访问网络，采用异步形式，你也可以按需求使用其他的网络访问库或使用 `await` 改为同步形式。

```javascript
var axios = require('axios');
var crypto = require('crypto');
var querystring = require('querystring');

/**
 * 调用多吉云API
 *
 * @param  {string}     apiPath     调用的 API 接口地址，包含 URL 请求参数 QueryString，例如：/console/vfetch/add.json?url=xxx&a=1&b=2
 * @param  {object}     data        POST 的数据，对象，例如 {a: 1, b: 2}，传递此参数表示不是 GET 请求而是 POST 请求
 * @param  {boolean}    jsonMode    数据 data 是否以 JSON 格式请求，默认为 false 则使用表单形式（a=1&b=2）
 * @param  {function}   callback    回调函数，兼容老版本调用代码，有两个参数，第一个参数表示错误，第二个参数是返回的数据
 * 
 * @returns {Promise}               返回一个 Promise，在传递 callback 的情况下，可用 .then() 和 .catch() 处理返回的数据
 */
function dogecloudApi(apiPath, data = {}, jsonMode = false, callback = null) {
    // 这里替换为你的多吉云永久 AccessKey 和 SecretKey，可在用户中心 - 密钥管理中查看
    // 请勿在客户端暴露 AccessKey 和 SecretKey，那样恶意用户将获得账号完全控制权
    const accessKey = '你的AccessKey';
    const secretKey = '你的SecretKey';

    const body = jsonMode ? JSON.stringify(data) : querystring.encode(data);
    const sign = crypto.createHmac('sha1', secretKey).update(Buffer.from(apiPath + "\n" + body, 'utf8')).digest('hex');
    const authorization = 'TOKEN ' + accessKey + ':' + sign;

    return new Promise(function(resolve, reject) {
        try {
            axios.request({
                url: 'https://api.dogecloud.com' + apiPath,
                method: 'POST',
                data: body,
                responseType: 'json',
                headers: {
                    'Content-Type': jsonMode ? 'application/json' : 'application/x-www-form-urlencoded',
                    'Authorization': authorization
                }
            })
            .then(function (response) {
                if (response.data.code !== 200) { // API 返回错误
                    callback ? callback({Error: 'API Error: ' + response.data.msg}, null) : reject({errno: response.data.code, msg: 'API Error: ' + response.data.msg});
                    return
                }
                callback ? callback(null, response.data.data) : resolve(response.data.data);
                
            })
            .catch(function (err) {
                callback ? callback(err, null) : reject(err);
            });
        } catch (error) {
            callback ? callback(error, null) : reject(err);
        }
    });
}
```

### **Java**

?> **说明** <a name="java" id="java"></a>这里使用的是原生的 `HttpURLConnection` 进行网络请求、`JSONObject` 作为 JSON 处理库，所以代码显得比较臃肿，你可以替换为你自己使用的处理库，并把这个函数加到你自己的工具类里面。

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.apache.commons.codec.binary.Hex;

// 普通 API 请使用这个方法
public static JSONObject dogeAPIGet(String apiPath, Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, String> hm : params.entrySet()){
        try {
            sb.append(URLEncoder.encode(hm.getKey(), String.valueOf(StandardCharsets.UTF_8))).append('=').append(URLEncoder.encode(hm.getValue(), String.valueOf(StandardCharsets.UTF_8))).append("&");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    String bodyText = sb.toString().replace("&$", "");
    try {
        return dogeAPIGet(apiPath, bodyText, false);
    } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
    }
}

// 要求请求内容 Body 是一个 JSON 的 API，请使用这个方法
public static JSONObject dogeAPIGet(String apiPath, JSONObject params) {
    String bodyText = params.toString();
    try {
        return dogeAPIGet(apiPath, bodyText, true);
    } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
    }
}

// 无参数 API
public static JSONObject dogeAPIGet(String apiPath) {
    try {
        return dogeAPIGet(apiPath, "", true);
    } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
    }
}

public static JSONObject dogeAPIGet(String apiPath, String paramsText, Boolean jsonMode) throws IOException{
    // 这里返回值类型是 JSONObject，你也可以根据你的具体情况使用不同的 JSON 库并修改最下方 JSON 处理代码

    // 这里替换为你的多吉云永久 AccessKey 和 SecretKey，可在用户中心 - 密钥管理中查看
    // 请勿在客户端暴露 AccessKey 和 SecretKey，那样恶意用户将获得账号完全控制权
    String accessKey = "你的AccessKey";
    String secretKey = "你的SecretKey";

    String signStr = apiPath + "\n" + paramsText;
    String sign = "";
    try {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA1"));
        sign = new String(new Hex().encode(mac.doFinal(signStr.getBytes())), StandardCharsets.UTF_8); // 这里 Hex 来自 org.apache.commons.codec.binary.Hex
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
    } catch (InvalidKeyException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
    }
    String authorization = "TOKEN " + accessKey + ':' + sign;

    URL u = new URL("https://api.dogecloud.com" + apiPath);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty( "Content-Type", jsonMode ? "application/json" : "application/x-www-form-urlencoded");
    conn.setRequestProperty( "Authorization", authorization);
    conn.setRequestProperty( "Content-Length", String.valueOf(paramsText.length()));
    OutputStream os = conn.getOutputStream();
    os.write(paramsText.getBytes());
    os.flush();
    os.close();
    StringBuilder retJSON = new StringBuilder();
    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
        String readLine = "";
        try (BufferedReader responseReader=new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            while((readLine=responseReader.readLine())!=null){
                retJSON.append(readLine).append("\n");
            }
            responseReader.close();
        }
        JSONObject ret = new JSONObject(retJSON.toString());
        if (ret.getInt("code") != 200) {
            System.err.println("{\"error\":\"API 返回错误：" + ret.getString("msg") + "\"}");
        } else {
            JSONObject output = new JSONObject();
            JSONObject data = ret.getJSONObject("data");
            return data;
        }
    } else {
        System.err.println("{\"error\":\"网络错误：" + conn.getResponseCode() + "\"}");
    }
    return null;
}
```

举例使用：

**修改视频名称**

```java
Map<String, String> params = new HashMap<String, String>();
params.put("vid", "227068");
params.put("name", "Test");

JSONObject data = dogeAPIGet("/console/video/edit.json", params);
System.out.println(data.toString());
```

**获取客户端上传临时密钥**

```java
String _bucket = "xinan"; // 替换为你要上传到的存储空间名称
String _key = "abc/123.jpg"; // 本次允许客户端上传的文件名，请根据当前网站用户登录状态、权限进行合理的最小化授权
// String _key = "abc/*"; // 也可以这样设置为 abc/* ，表示允许客户端上传到 abc 文件夹下的任意文件名
// String _key = "*"; // 或者设为 * 表示允许客户端上传到该存储空间内的任意文件（有安全风险，不推荐这样做）

JSONObject body = new JSONObject(); // 这里 JSONObject 来自 org.json.JSONObject
body.put("channel", "OSS_UPLOAD");
body.append("scopes", _bucket + ":" + _key);

JSONObject data = dogeAPIGet("/auth/tmp_token.json", body);
System.out.println(data.toString());
```

### **Go**

?> **说明** <a name="go" id="go"></a>该简易函数请勿用于 Go 客户端，否则将暴露你的密钥，恶意用户将获得账号完全控制权。

```go
package main

import (
	"fmt"
	"encoding/json"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/hex"
	"log"
	"net/http"
	"net/url"
	"strings"
	"io/ioutil"
//	"reflect"
)

// 调用多吉云的 API
// apiPath：是调用的 API 接口地址，包含 URL 请求参数 QueryString，例如：/console/vfetch/add.json?url=xxx&a=1&b=2
// data：POST 的数据，对象，例如 {a: 1, b: 2}，传递此参数表示不是 GET 请求而是 POST 请求
// jsonMode：数据 data 是否以 JSON 格式请求，默认为 false 则使用表单形式（a=1&b=2）
// 返回值 ret 是一个 map[string]，其中 ret["code"] 为 200 表示 api 请求成功
func DogeCloudAPI(apiPath string, data map[string]interface{}, jsonMode bool) (ret map[string]interface{}) {

	// 这里替换为你的多吉云永久 AccessKey 和 SecretKey，可在用户中心 - 密钥管理中查看
	// 请勿在客户端暴露 AccessKey 和 SecretKey，那样恶意用户将获得账号完全控制权
	AccessKey := "你的AccessKey"
	SecretKey := "你的SecretKey"

	body := ""
	mime := ""
	if jsonMode {
		_body, err := json.Marshal(data)
		if err != nil{ log.Fatalln(err) }
		body = string(_body)
		mime = "application/json"
	} else {
		values := url.Values{}
		for k, v := range data {
			values.Set(k, v.(string))
		}
		body = values.Encode()
		mime = "application/x-www-form-urlencoded"
	}

	signStr := apiPath + "\n" + body
	hmacObj := hmac.New(sha1.New, []byte(SecretKey))
	hmacObj.Write([]byte(signStr))
	sign := hex.EncodeToString(hmacObj.Sum(nil))
	Authorization := "TOKEN " + AccessKey + ":" + sign

	req, err := http.NewRequest("POST", "https://api.dogecloud.com" + apiPath, strings.NewReader(body))
	req.Header.Add("Content-Type", mime)
	req.Header.Add("Authorization", Authorization)
	client := http.Client{}
	resp, err := client.Do(req)
	if err != nil{ log.Fatalln(err) } // 网络错误
	defer resp.Body.Close()
	r, err := ioutil.ReadAll(resp.Body)

    json.Unmarshal([]byte(r), &ret)
    
    // Debug，正式使用时可以注释掉
	fmt.Printf("[DogeCloudAPI] code: %d, msg: %s, data: %s\n", int(ret["code"].(float64)), ret["msg"], ret["data"])
	return
}
```

<!-- tabs:end -->