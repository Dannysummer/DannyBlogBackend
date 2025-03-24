# Java S3 SDK

> 在服务端使用 Java 进行存储空间管理、文件管理和文件上传的方案

如果需要在服务端对文件进行管理，上传和下载，可以使用 AWS S3 Java SDK。

!> 本文介绍的是使用 Java 在服务端进行管理操作，如果是 Android 客户端，请查看 [Android S3](oss/sdk-s3upload-android.md) 。

## 引入

### 添加 dogeAPIGet 方法

首先确保你已经在程序中添加了 dogeAPIGet 方法，例如可以添加到你自己的工具类中。

该方法代码请查看： [dogeAPIGet](oss/api-access-token?id=java) 。

### 引入 AWS S3 Java SDK

!> 本文使用的是 AWS S3 Java SDK **V2.X** 版本，关于 **V1.X** 版本的，请查看 [Java S3 SDK（V1.X）](oss/sdk-full-java-v1.md) 。由于本文介绍的 **V2.X** 的分片上传功能（`s3-transfer-manager`），AWS 官方还处于 **PREVIEW** 预览版状态，可能不稳定，建议需要分片上传大文件的用户使用 **V1.X** 版本。

可以通过 Maven 或 Gradle 引入 AWS S3 Java SDK，以 Maven 为例，在 pom.xml 中引入：

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
  <version>2.18.30</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>2.0.5</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3-transfer-manager</artifactId>
  <version>2.18.30-PREVIEW</version>
</dependency>
```

或者使用 Gradle：

```groovy
...
dependencies {
  ...
  implementation 'software.amazon.awssdk:s3:2.18.30'
  implementation 'org.slf4j:slf4j-simple:2.0.5'
  implementation 'software.amazon.awssdk:s3-transfer-manager:2.18.30-PREVIEW'
  ...
}
```

!> AWS S3 Java SDK 依赖 `slf4j` 实现日志功能，一并引入。另外常用库 `commons-codec` 和 `org.json` 如果不能自动引入，可能也需要你手动添加依赖。

### 初始化 S3 SDK

如果需要使用 Java 在服务端进行文件操作，需要先初始化 S3 SDK。

首先需要获取临时密钥：

```java
// 这段代码本质上就是调用多吉云 API 获取临时密钥，你也可以自己实现：
// 该 API 参考文档： https://docs.dogecloud.com/oss/api-tmp-token

JSONObject body = new JSONObject(); 
body.put("channel", "OSS_FULL");
body.append("scopes", "mybucket"); // 要操作的存储空间名称，注意不是 s3Bucket 值，也可以设置为 *

JSONObject api = dogeAPIGet("/auth/tmp_token.json", body);
JSONObject credentials = api.getJSONObject("Credentials");
```

然后使用获取到的临时密钥，初始化 S3 实例：

```java
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;
```

```java
AwsSessionCredentials awsCreds = AwsSessionCredentials.create(credentials.getString("accessKeyId"), credentials.getString("secretAccessKey"), credentials.getString("sessionToken"));
S3Client s3 = S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
        .region(Region.of("automatic"))
        .endpointOverride(URI.create("https://s3endpoint.example.com")) // 修改为多吉云控制台存储空间 SDK 参数中的 s3Endpoint
        .build();
```

## 存储空间操作

一些简单的存储空间操作，这些操作不需要用到 AWS S3 SDK。

### 获取存储空间列表

用于获取当前账号下的存储空间列表，获取列表并输出：

```java
JSONObject data = dogeAPIGet("/oss/bucket/list.json");
JSONArray buckets = data.getJSONArray("buckets");
for (int i = 0 ; i < buckets.length(); i++) {
    System.out.println(buckets.getJSONObject(i).toString());
}
```

### 创建存储空间

创建一个上海地域，加速类型为静态小文件，名为 `newbucket` 的存储空间：

```java
Map<String, String> params = new HashMap<String, String>();
params.put("name", "newbucket");
params.put("region", "0");  // 0: 上海（华东），1: 北京（华北），2: 广州（华南），3: 成都（西南）
params.put("service_type", "web");

JSONObject data = dogeAPIGet("/oss/bucket/create.json", params);
System.out.println(data.toString());
```

### 删除存储空间

删除名为 `newbucket` 的存储空间：

```java
Map<String, String> params = new HashMap<String, String>();
params.put("name", "newbucket");

JSONObject data = dogeAPIGet("/oss/bucket/delete.json", params);
System.out.println(data.toString());
```

## 文件操作

要进行文件操作，需要先初始化 S3 SDK，上面已经介绍如何初始化 S3 SDK。

### 获取文件列表

```java
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.util.Iterator;
import java.util.List;
```

```java
ListObjectsRequest.Builder reqBuilder = ListObjectsRequest
        .builder()
        .bucket("mybucket-1234567890") // 修改为多吉云控制台存储空间 SDK 参数中的 s3Bucket
        .marker("")
        .prefix("")
        .delimiter("") // 设置为 "/" 表示模拟目录，此时需要通过 response.commonPrefixes 获取目录列表。如果设置为空字符串表示不模拟目录，全部按文件处理
        .maxKeys(5);
String nextMarker = "";
while (true) {
    ListObjectsResponse response = s3.listObjects(reqBuilder.copy().marker(nextMarker).build());
    List<S3Object> objectListing = response.contents(); // 获取文件列表，如果需要获取目录列表可以 delimiter 设置为 "/"，然后用 response.commonPrefixes

    for (Iterator<?> iterator = objectListing.iterator(); iterator.hasNext(); ) {
        S3Object objectSummary = (S3Object) iterator.next();
        System.out.printf("文件: %s (大小: %d)\n", objectSummary.key(), objectSummary.size());
    }
    if (response.isTruncated()) { // isTruncated 为 true 表示文件还未列举完成，还应继续请求
        nextMarker = response.nextMarker(); // 将循环的下次请求设置为本次返回的 NextMarker 值
        System.out.printf("还有更多..., nextMarker: %s\n", nextMarker);
    } else {
        break;
    }
}
```

`ListObjectsRequest` 的各参数说明，其中 `*` 为必需：

参数 | 类型 | 说明
---- | ---- | ----
Bucket | String | 指定存储空间的 s3Bucket 值，控制台存储空间 SDK 参数选项卡中可以找到，获取临时密钥时也有
Prefix | String | 文件列表过滤前缀，限定返回中只包含指定前缀的文件，例如 `abc/` 表示列出前缀为 `abc/` 的文件，或者可以理解为 `abc` 文件夹下的文件
Delimiter | String | 一般为空，或者为 `/`，表示用于模拟“文件夹”概念的分隔字符。<br>设为 `/` 表示开启目录结构，将只会返回指定 Prefix 目录下的文件，不会返回子目录的文件，子目录列表将在结果中用 `CommonPrefixes` 列出；<br>设为空字符串表示不开启目录结构，路径中所有 / 将被当成普通字符串处理
Marker | String | 下一次获取的起始点，用于循环获取所有文件。首次获取设置为空字符串，接下来每次获取设置为前一次获取返回的 `NextMarker` 值
MaxKeys | Int | 设置每次获取的文件数量，最大 1000


### 简单上传

如果你需要上传的文件基本都在 100 MB 以内，可以使用简单上传。

简单上传一个字符串到指定路径（abc/123.txt）

```java
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
```

```java
PutObjectRequest putOb = PutObjectRequest.builder()
        .bucket("mybucket-1234567890") // 多吉云控制台存储空间 SDK 参数中的 s3Bucket
        .key("abc/123.txt")
        .build();

PutObjectResponse response = s3.putObject(putOb, RequestBody.fromString("test"));
// RequestBody 也支持 fromFile、fromBytes、fromBytesBuffer、fromInputStream 等多种来源
```

### 分片上传

如果你不确定需要上传的文件的大小，可能会有不太适合简单上传的大文件，可以进行分片上传。（不确定文件大小时，可以不管大小都直接使用分片上传）

`s3-transfer-manager` 内部封装的 `S3TransferManager` 工具已经为我们提供了分片上传的能力。

```java
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import java.net.URI;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
```

```java
S3TransferManager tm = S3TransferManager.builder()
        .s3ClientConfiguration(
                b -> b.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of("automatic"))
                .endpointOverride(URI.create("https://s3endpoint.example.com")) // 修改为多吉云控制台存储空间 SDK 参数中的 s3Endpoint
                .targetThroughputInGbps(20.0) // 上传限速，单位 Gbps，默认 5 Gbps
                .minimumPartSizeInBytes(8 * MB) // 每个分片最小体积，默认 8MB
                // .maxConcurrency(16) // 最大并发数，即最多允许同时上传多少个分片，默认会自动计算，建议不设置。
        ).build();

FileUpload upload =
        tm.uploadFile(u -> u.source(Paths.get("/path/to/file.mp4"))
                .putObjectRequest(p -> p.bucket("mybucket-1234567890").key("abc/largefile.mp4")) // bucket 为多吉云控制台存储空间 SDK 参数中的 s3Bucket，key 为上传目标文件名
                .overrideConfiguration(o -> o.addListener(LoggingTransferListener.create())) // 监控上传进度
        );
upload.completionFuture().join();
```

?> `TransferListener` 类可以用来监控上传进度，`LoggingTransferListener` 是其的一个实现，会在控制台输出上传进度。你也可以自己写一个 `TransferListener` 的实现，来完成你自己的进度监控功能。参考 S3 API 文档：[TransferListener](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/transfer/s3/progress/TransferListener.html) 。

### 复制文件

将 `sourceS3Bucket`（不是存储空间名，是空间对应的 s3Bucket 值） 的 `abc/123.jpg`，<br>
复制到 `targetS3Bucket`（不是存储空间名，是空间对应的 s3Bucket 值） 的 `abc/123_copy.jpg`：

!> 如果是跨存储空间复制，获取临时密钥时的 `scope` 中必须同时包含两个空间名，或设置为 `*` 。

```java
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
```

```java
CopyObjectRequest copyReq = CopyObjectRequest.builder()
        .sourceBucket(sourceS3Bucket)
        .sourceKey("abc/123.jpg")
        .destinationBucket(targetS3Bucket)
        .destinationKey("abc/123_copy.jpg")
        .build();
CopyObjectResponse copyRes = s3.copyObject(copyReq);
```

### 删除文件

```java
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import java.util.ArrayList;
```

```java
ArrayList<ObjectIdentifier> keys = new ArrayList<>();
keys.add(ObjectIdentifier.builder().key("abc/123.txt").build());
keys.add(ObjectIdentifier.builder().key("abc/abc.txt").build()); // 添加多个可以删除多个文件

Delete del = Delete.builder().objects(keys).build();
DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
        .bucket("mybucket-1234567890") // 多吉云控制台存储空间 SDK 参数中的 s3Bucket
        .delete(del)
        .build();
s3.deleteObjects(multiObjectDeleteRequest);
```

## 为客户端上传文件获取临时密钥

如果你需要在客户端上传文件，则需要服务端为客户端提供临时密钥：

```java
String _bucket = "mybucket"; // 替换为你要上传到的存储空间名称
String _key = "abc/123.jpg"; // 本次允许客户端上传的文件名，请根据当前网站用户登录状态、权限进行合理的最小化授权
// String _key = "abc/*"; // 也可以这样设置为 abc/* ，表示允许客户端上传到 abc 文件夹下的任意文件名
// String _key = "*"; // 或者设为 * 表示允许客户端上传到该存储空间内的任意文件（有安全风险，不推荐这样做）

JSONObject body = new JSONObject(); // 这里 JSONObject 来自 org.json.JSONObject
body.put("channel", "OSS_UPLOAD");
body.append("scopes", _bucket + ":" + _key);

JSONObject data = dogeAPIGet("/auth/tmp_token.json", body);
JSONObject bucket = data.getJSONArray("Buckets").optJSONObject(0);
JSONObject output = new JSONObject();
output.put("credentials", data.getJSONObject("Credentials"));
output.put("s3Bucket", bucket.getString("s3Bucket"));
output.put("s3Endpoint", bucket.getString("s3Endpoint"));
output.put("keyPrefix", _key); // 顺便告诉客户端本次它允许上传到哪个文件或文件前缀
System.out.println(output.toString()); //成功输出
```
