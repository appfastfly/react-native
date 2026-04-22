#import "AppfastflyApiClient.h"

static NSTimeInterval const kRequestTimeout = 15.0;

@implementation AppfastflyApiClient {
  NSString *_serviceUrl;
  NSString *_apiKey;
  NSURLSession *_session;
}

- (instancetype)initWithServiceUrl:(NSString *)serviceUrl apiKey:(NSString *)apiKey {
  self = [super init];
  if (self) {
    _serviceUrl = [serviceUrl copy];
    _apiKey = [apiKey copy];

    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    config.timeoutIntervalForRequest = kRequestTimeout;
    config.timeoutIntervalForResource = kRequestTimeout;
    _session = [NSURLSession sessionWithConfiguration:config];
  }
  return self;
}

- (void)post:(NSString *)path
        body:(NSDictionary *)body
  completion:(void (^)(NSDictionary * _Nullable))completion
{
  @try {
    NSString *urlString = [NSString stringWithFormat:@"%@%@", _serviceUrl, path];
    NSURL *url = [NSURL URLWithString:urlString];
    if (!url) {
      if (completion) completion(nil);
      return;
    }

    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    request.HTTPMethod = @"POST";
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    if (_apiKey.length > 0) {
      [request setValue:_apiKey forHTTPHeaderField:@"X-API-Key"];
    }

    NSError *jsonError;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&jsonError];
    if (jsonError || !jsonData) {
      if (completion) completion(nil);
      return;
    }
    request.HTTPBody = jsonData;

    NSURLSessionDataTask *task = [_session dataTaskWithRequest:request
      completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (error || !data) {
          if (completion) completion(nil);
          return;
        }

        NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
        if (httpResponse.statusCode < 200 || httpResponse.statusCode >= 300) {
          if (completion) completion(nil);
          return;
        }

        @try {
          NSError *parseError;
          NSDictionary *result = [NSJSONSerialization JSONObjectWithData:data options:0 error:&parseError];
          if (parseError || ![result isKindOfClass:[NSDictionary class]]) {
            if (completion) completion(nil);
            return;
          }
          if (completion) completion(result);
        } @catch (NSException *exception) {
          if (completion) completion(nil);
        }
      }];
    [task resume];
  } @catch (NSException *exception) {
    if (completion) completion(nil);
  }
}

- (void)deleteRequest:(NSString *)path
                 body:(NSDictionary *)body
           completion:(void (^)(NSError * _Nullable))completion
{
  @try {
    NSString *urlString = [NSString stringWithFormat:@"%@%@", _serviceUrl, path];
    NSURL *url = [NSURL URLWithString:urlString];
    if (!url) {
      if (completion) completion([NSError errorWithDomain:@"AppfastflyApiClient" code:-1 userInfo:@{NSLocalizedDescriptionKey: @"Invalid URL"}]);
      return;
    }

    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    request.HTTPMethod = @"DELETE";
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    if (_apiKey.length > 0) {
      [request setValue:_apiKey forHTTPHeaderField:@"X-API-Key"];
    }

    NSError *jsonError;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&jsonError];
    if (jsonError || !jsonData) {
      if (completion) completion(jsonError ?: [NSError errorWithDomain:@"AppfastflyApiClient" code:-1 userInfo:nil]);
      return;
    }
    request.HTTPBody = jsonData;

    NSURLSessionDataTask *task = [_session dataTaskWithRequest:request
      completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (completion) completion(error);
      }];
    [task resume];
  } @catch (NSException *exception) {
    if (completion) completion([NSError errorWithDomain:@"AppfastflyApiClient" code:-1 userInfo:@{NSLocalizedDescriptionKey: exception.reason ?: @"Unknown error"}]);
  }
}

@end
