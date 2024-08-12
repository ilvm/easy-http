# Easy HTTP
[![Android CI](https://github.com/ilvm/easy-http/workflows/Android%20CI/badge.svg)](https://github.com/ilvm/easy-http/actions?query=workflow%3A%22Android+CI%22) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/ilvm/easy-http/blob/master/LICENSE)

EasyHttp is a lightweight HTTP client library for Android, designed to make it easy to perform synchronous and asynchronous HTTP requests with support for various HTTP methods, automatic redirection handling, and customizable retry policies.

## Features
- Supports GET, POST, and PUT methods.
- Handles HTTP redirects automatically with customizable redirect depth.
- Supports synchronous and asynchronous requests.
- Allows for customizable headers, query parameters, and request bodies.
- Simple integration with Android's Executor and Handler.
- Customizable retry policies.

## Installation

To use EasyHttp in your project, add the following dependency to your build.gradle file:

```groovy
dependencies {
    implementation 'xds.lib:easyhttp:2.0.0'
}
```

## Usage
### 1. Define a Custom Request
To use HttpRequest, you need to extend the class and implement the necessary methods for your specific use case. Here’s an example of how to create a simple GET request:

```java
public class MyGetRequest extends HttpRequest<String> { 
    
    @Override
    protected String getUrl() {
        return "https://api.example.com/data";
    }
    
    @Override
    @WorkerThread
    protected String parseResponse(InputStream inputStream, String contentType) throws IOException {
        return IOUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
    }
}
``` 
### 2. Execute the Request Synchronously
You can execute the request synchronously using the execute method:
```java
MyGetRequest request = new MyGetRequest();
try {
    String response = request.execut();
    // Handle the response 
} catch (RequestException | ResponseException| ParseException e) {
    // Handle errors
}
```
### 3. Execute the Request Asynchronously
You can also execute the request asynchronously using the executeAsync method:
```java
MyGetRequest request = new MyGetRequest();
request.executeAsync(Executors.newSingleThreadExecutor(), new ResponseListener<String>() {
    @Override
    public void onSuccess(String response, String requestId) {
        // Handle the response
    }

    @Override
    public void onFailed(Throwable throwable, String requestId) {
        // Handle errors
    }
});
```
### 4. Customizing the Request
You can override additional methods to customize the behavior of your request:
```java
public class MyPostRequest extends HttpRequest<String> {
    
    @Override protected
    String getUrl() {
        return "https://api.example.com/submit";
    }
    
    @Override
    protected String getRequestMethod() {
        return "POST";
    }
    
    @Override
    protected String getRequestContentType() {
        return "application/json";
    }
    
    @Override
    protected void writeRequestBody(OutputStream os) throws IOException {
        String jsonBody = "{\"key\":\"value\"}";
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    @WorkerThread
    protected String parseResponse(InputStream inputStream, String contentType) throws IOException {
        return IOUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
    }
}
```
### 5. Handling Redirects
By default, the library handles HTTP redirects (3xx status codes) automatically. You can customize the maximum number of allowed redirects by overriding the getMaxRedirects method:
```java
public class CustomHttpRequest extends HttpRequest<String> {
    
    @Override
    protected int getMaxRedirects() {
        return 10; // Allow up to 10 redirects
    }
}
```
### 6. Customizing Retry Policy
You can define a custom retry policy by overriding the createRetryPolicy method:
```java
public class CustomRetryRequest extends HttpRequest<String> {

    @Override
    protected RetryPolicy createRetryPolicy() {
        return RetryPolicy.create(responseCode); // Retry up to 3 times with a delay 
    }
}
```
### 7. Contribution
Contributions are welcome! If you want to contribute to EasyHttp, feel free to submit a pull request or open an issue.
### 8. License
This project is licensed under the MIT License - see the [LICENSE](https://github.com/ilvm/easy-http?tab=MIT-1-ov-file#readme) file for details.
